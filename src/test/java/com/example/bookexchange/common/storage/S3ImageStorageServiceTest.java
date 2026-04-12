package com.example.bookexchange.common.storage;

import com.example.bookexchange.common.result.ResultFactory;
import com.example.bookexchange.common.result.Success;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import static com.example.bookexchange.common.i18n.MessageKey.SYSTEM_INVALID_IMAGE;
import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(MockitoExtension.class)
class S3ImageStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private ImageProcessingService imageProcessingService;

    private S3ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.getS3().setRegion("eu-central-1");
        storageProperties.getS3().setTestBucket("book-exchange-test");
        storageProperties.getS3().setProdBucket("book-exchange-prod");
        storageProperties.getS3().setUseTestBucket(true);

        imageStorageService = new S3ImageStorageService(s3Client, storageProperties, imageProcessingService);
    }

    @Test
    void shouldUploadBookImageToBookPrefix_whenPayloadIsValid() {
        ProcessedImage processedImage = new ProcessedImage(new byte[]{1, 2, 3}, "image/jpeg", "jpg");

        when(imageProcessingService.process("book-photo-base64")).thenReturn(ResultFactory.ok(processedImage));
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(ListObjectsV2Response.builder().build());
        when(s3Client.utilities()).thenReturn(S3Utilities.builder().region(Region.EU_CENTRAL_1).build());

        Success<String> success = assertSuccess(imageStorageService.replaceBookImage(5L, 9L, "book-photo-base64"), OK);
        ArgumentCaptor<PutObjectRequest> putObjectCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3Client).putObject(putObjectCaptor.capture(), any(RequestBody.class));

        PutObjectRequest putObjectRequest = putObjectCaptor.getValue();
        assertThat(putObjectRequest.bucket()).isEqualTo("book-exchange-test");
        assertThat(putObjectRequest.key()).startsWith("users/5/books/9_");
        assertThat(putObjectRequest.key()).endsWith(".jpg");
        assertThat(success.body()).startsWith("https://book-exchange-test.s3.eu-central-1.amazonaws.com/users/5/books/9_");
    }

    @Test
    void shouldDeletePreviousUserProfileImage_whenReplacingProfilePhoto() {
        ProcessedImage processedImage = new ProcessedImage(new byte[]{1, 2, 3}, "image/jpeg", "jpg");

        when(imageProcessingService.process("user-photo-base64")).thenReturn(ResultFactory.ok(processedImage));
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(ListObjectsV2Response.builder()
                .contents(S3Object.builder().key("users/42/profile_photo_1712582410000.jpg").build())
                .build());
        when(s3Client.utilities()).thenReturn(S3Utilities.builder().region(Region.EU_CENTRAL_1).build());

        assertSuccess(imageStorageService.replaceUserProfileImage(42L, "user-photo-base64"), OK);

        ArgumentCaptor<ListObjectsV2Request> listRequestCaptor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        ArgumentCaptor<DeleteObjectRequest> deleteObjectCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);

        verify(s3Client).listObjectsV2(listRequestCaptor.capture());
        verify(s3Client).deleteObject(deleteObjectCaptor.capture());

        assertThat(listRequestCaptor.getValue().prefix()).isEqualTo("users/42/profile_photo_");
        assertThat(deleteObjectCaptor.getValue().key()).isEqualTo("users/42/profile_photo_1712582410000.jpg");
    }

    @Test
    void shouldDeleteBookImageByPrefix_whenDeleteBookImageIsCalled() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(ListObjectsV2Response.builder()
                .contents(S3Object.builder().key("users/42/books/7_1712582411000.jpg").build())
                .build());

        assertSuccess(imageStorageService.deleteBookImage(42L, 7L), null);

        ArgumentCaptor<ListObjectsV2Request> listRequestCaptor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        ArgumentCaptor<DeleteObjectRequest> deleteObjectCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);

        verify(s3Client).listObjectsV2(listRequestCaptor.capture());
        verify(s3Client).deleteObject(deleteObjectCaptor.capture());

        assertThat(listRequestCaptor.getValue().prefix()).isEqualTo("users/42/books/7_");
        assertThat(deleteObjectCaptor.getValue().key()).isEqualTo("users/42/books/7_1712582411000.jpg");
    }

    @Test
    void shouldDeleteUserProfileAndBookPrefixes_whenDeletingAllUserImages() {
        when(s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket("book-exchange-test")
                .prefix("users/42/")
                .continuationToken(null)
                .build()))
                .thenReturn(ListObjectsV2Response.builder()
                        .contents(
                                S3Object.builder().key("users/42/profile_photo_1712582410000.jpg").build(),
                                S3Object.builder().key("users/42/books/7_1712582411000.jpg").build()
                        )
                        .build());

        assertSuccess(imageStorageService.deleteAllUserImages(42L), null);

        ArgumentCaptor<DeleteObjectsRequest> deleteObjectsCaptor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client).deleteObjects(deleteObjectsCaptor.capture());

        assertThat(deleteObjectsCaptor.getValue().delete().objects())
                .extracting(objectIdentifier -> objectIdentifier.key())
                .containsExactlyInAnyOrder("users/42/profile_photo_1712582410000.jpg", "users/42/books/7_1712582411000.jpg");
    }

    @Test
    void shouldReturnBadRequest_whenImageProcessingFails() {
        when(imageProcessingService.process("invalid")).thenReturn(ResultFactory.error(SYSTEM_INVALID_IMAGE, BAD_REQUEST));

        assertFailure(imageStorageService.replaceUserProfileImage(42L, "invalid"), SYSTEM_INVALID_IMAGE, BAD_REQUEST);
    }
}
