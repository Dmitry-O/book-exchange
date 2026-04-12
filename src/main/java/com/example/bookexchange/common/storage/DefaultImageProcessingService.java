package com.example.bookexchange.common.storage;

import com.example.bookexchange.common.i18n.MessageKey;
import com.example.bookexchange.common.result.Result;
import com.example.bookexchange.common.result.ResultFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;

@Service
@RequiredArgsConstructor
public class DefaultImageProcessingService implements ImageProcessingService {

    private static final String PNG_CONTENT_TYPE = "image/png";
    private static final String JPEG_CONTENT_TYPE = "image/jpeg";

    private final StorageProperties storageProperties;

    @Override
    public Result<ProcessedImage> process(String photoBase64) {
        if (photoBase64 == null || photoBase64.isBlank()) {
            return ResultFactory.error(MessageKey.SYSTEM_INVALID_IMAGE, HttpStatus.BAD_REQUEST);
        }

        try {
            String normalizedBase64 = normalizeBase64(photoBase64);
            byte[] rawBytes = Base64.getDecoder().decode(normalizedBase64);
            BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(rawBytes));

            if (sourceImage == null) {
                return ResultFactory.error(MessageKey.SYSTEM_INVALID_IMAGE, HttpStatus.BAD_REQUEST);
            }

            BufferedImage resizedImage = resize(sourceImage);
            boolean hasAlpha = resizedImage.getColorModel().hasAlpha();

            return hasAlpha ? encodePng(resizedImage) : encodeJpeg(resizedImage);
        } catch (IllegalArgumentException | IOException ex) {
            return ResultFactory.error(MessageKey.SYSTEM_INVALID_IMAGE, HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeBase64(String photoBase64) {
        int commaIndex = photoBase64.indexOf(',');

        if (photoBase64.startsWith("data:image/") && commaIndex > 0) {
            return photoBase64.substring(commaIndex + 1);
        }

        return photoBase64;
    }

    private BufferedImage resize(BufferedImage sourceImage) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        int maxWidth = storageProperties.getImage().getMaxWidth();
        int maxHeight = storageProperties.getImage().getMaxHeight();

        double scale = Math.min(
                Math.min((double) maxWidth / width, (double) maxHeight / height),
                1.0D
        );

        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        int imageType = sourceImage.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;

        BufferedImage targetImage = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D graphics = targetImage.createGraphics();

        if (!sourceImage.getColorModel().hasAlpha()) {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
        }

        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();

        return targetImage;
    }

    private Result<ProcessedImage> encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);

        return ResultFactory.ok(new ProcessedImage(outputStream.toByteArray(), PNG_CONTENT_TYPE, "png"));
    }

    private Result<ProcessedImage> encodeJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");

        if (!writers.hasNext()) {
            return ResultFactory.error(MessageKey.SYSTEM_INVALID_IMAGE, HttpStatus.BAD_REQUEST);
        }

        ImageWriter writer = writers.next();

        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);

            ImageWriteParam writeParam = writer.getDefaultWriteParam();

            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(storageProperties.getImage().getJpegQuality());
            }

            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }

        return ResultFactory.ok(new ProcessedImage(outputStream.toByteArray(), JPEG_CONTENT_TYPE, "jpg"));
    }
}
