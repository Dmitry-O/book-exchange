package com.example.bookexchange.common.storage;

import com.example.bookexchange.common.result.Success;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static com.example.bookexchange.common.i18n.MessageKey.SYSTEM_INVALID_IMAGE;
import static com.example.bookexchange.support.unit.ResultAssertions.assertFailure;
import static com.example.bookexchange.support.unit.ResultAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

class DefaultImageProcessingServiceTest {

    @Test
    void shouldResizeAndEncodeImage_whenPayloadIsValid() throws Exception {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.getImage().setMaxWidth(1200);
        storageProperties.getImage().setMaxHeight(1200);
        storageProperties.getImage().setJpegQuality(0.8F);
        DefaultImageProcessingService imageProcessingService = new DefaultImageProcessingService(storageProperties);

        String base64Image = createBase64Image(2000, 1000, BufferedImage.TYPE_INT_RGB, "png");

        Success<ProcessedImage> success = assertSuccess(imageProcessingService.process(base64Image), OK);
        ProcessedImage processedImage = success.body();
        BufferedImage resultImage = ImageIO.read(new ByteArrayInputStream(processedImage.bytes()));

        assertThat(processedImage.contentType()).isEqualTo("image/jpeg");
        assertThat(processedImage.extension()).isEqualTo("jpg");
        assertThat(resultImage.getWidth()).isEqualTo(1200);
        assertThat(resultImage.getHeight()).isEqualTo(600);
    }

    @Test
    void shouldReturnBadRequest_whenImagePayloadIsInvalid() {
        StorageProperties storageProperties = new StorageProperties();
        DefaultImageProcessingService imageProcessingService = new DefaultImageProcessingService(storageProperties);

        assertFailure(imageProcessingService.process("not-base64"), SYSTEM_INVALID_IMAGE, BAD_REQUEST);
    }

    private String createBase64Image(int width, int height, int type, String format) throws Exception {
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.ORANGE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, format, outputStream);

        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
}
