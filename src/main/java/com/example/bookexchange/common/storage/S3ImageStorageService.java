package com.example.bookexchange.common.storage;

import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3ImageStorageService implements ImageStorageService {

    private static final String USERS_PREFIX = "users";
    private static final String BOOKS_DIRECTORY = "books";
    private static final String PROFILE_PHOTO_PREFIX = "profile_photo_";
    private static final int DELETE_BATCH_SIZE = 1000;

    private final S3Client s3Client;
    private final StorageProperties storageProperties;
    private final ImageProcessingService imageProcessingService;

    @Override
    public Result<String> replaceUserProfileImage(Long userId, String photoBase64) {
        return uploadAndReplace(
                USERS_PREFIX + "/" + userId + "/" + PROFILE_PHOTO_PREFIX,
                photoBase64
        );
    }

    @Override
    public Result<String> replaceBookImage(Long userId, Long bookId, String photoBase64) {
        return uploadAndReplace(
                USERS_PREFIX + "/" + userId + "/" + BOOKS_DIRECTORY + "/" + bookId + "_",
                photoBase64
        );
    }

    @Override
    public Result<Void> deleteUserProfileImage(Long userId) {
        return deleteByPrefix(USERS_PREFIX + "/" + userId + "/" + PROFILE_PHOTO_PREFIX);
    }

    @Override
    public Result<Void> deleteBookImage(Long userId, Long bookId) {
        return deleteByPrefix(USERS_PREFIX + "/" + userId + "/" + BOOKS_DIRECTORY + "/" + bookId + "_");
    }

    @Override
    public Result<Void> deleteAllUserImages(Long userId) {
        return deleteByPrefix(USERS_PREFIX + "/" + userId + "/");
    }

    private Result<String> uploadAndReplace(String prefix, String photoBase64) {
        String bucketName = resolveBucketName();

        return imageProcessingService.process(photoBase64)
                .flatMap(processedImage -> {
                    String key = prefix + Instant.now().toEpochMilli() + "." + processedImage.extension();

                    try {
                        uploadObject(bucketName, key, processedImage);
                        deleteByPrefix(bucketName, prefix, key);

                        return ResultFactory.ok(buildPublicUrl(bucketName, key));
                    } catch (RuntimeException ex) {
                        log.warn(
                                "Failed to upload image to S3. bucket={}, region={}, keyPrefix={}, reason={}",
                                bucketName,
                                storageProperties.getS3().getRegion(),
                                prefix,
                                ex.getMessage(),
                                ex
                        );

                        return ResultFactory.error(MessageKey.SYSTEM_IMAGE_STORAGE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                });
    }

    private Result<Void> deleteByPrefix(String prefix) {
        String bucketName = resolveBucketName();

        try {
            deleteByPrefix(bucketName, prefix, null);

            return ResultFactory.successVoid();
        } catch (RuntimeException ex) {
            log.warn(
                    "Failed to delete image(s) from S3. bucket={}, region={}, keyPrefix={}, reason={}",
                    bucketName,
                    storageProperties.getS3().getRegion(),
                    prefix,
                    ex.getMessage(),
                    ex
            );
            return ResultFactory.error(MessageKey.SYSTEM_IMAGE_STORAGE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String resolveBucketName() {
        return storageProperties.getS3().isUseTestBucket()
                ? storageProperties.getS3().getTestBucket()
                : storageProperties.getS3().getProdBucket();
    }

    private void uploadObject(String bucketName, String key, ProcessedImage processedImage) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(processedImage.contentType())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(processedImage.bytes()));
    }

    private void deleteByPrefix(String bucketName, String prefix, String keyToKeep) {
        String continuationToken = null;

        do {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .continuationToken(continuationToken)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            List<ObjectIdentifier> objectIdentifiers = new ArrayList<>();

            for (S3Object object : response.contents()) {
                if (!object.key().equals(keyToKeep)) {
                    objectIdentifiers.add(ObjectIdentifier.builder().key(object.key()).build());
                }
            }

            deleteObjects(bucketName, objectIdentifiers);
            continuationToken = response.nextContinuationToken();
        } while (continuationToken != null);
    }

    private void deleteObjects(String bucketName, List<ObjectIdentifier> objectIdentifiers) {
        if (objectIdentifiers.isEmpty()) {
            return;
        }

        for (int startIndex = 0; startIndex < objectIdentifiers.size(); startIndex += DELETE_BATCH_SIZE) {
            int endIndex = Math.min(startIndex + DELETE_BATCH_SIZE, objectIdentifiers.size());
            List<ObjectIdentifier> batch = objectIdentifiers.subList(startIndex, endIndex);

            if (batch.size() == 1) {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(batch.get(0).key())
                        .build());
                continue;
            }

            s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder().objects(batch).build())
                    .build());
        }
    }

    private String buildPublicUrl(String bucketName, String key) {
        return s3Client.utilities()
                .getUrl(GetUrlRequest.builder().bucket(bucketName).key(key).build())
                .toString();
    }
}
