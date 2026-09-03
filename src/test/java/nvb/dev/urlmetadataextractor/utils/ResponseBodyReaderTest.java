package nvb.dev.urlmetadataextractor.utils;

import nvb.dev.urlmetadataextractor.exception.ResponseTooLargeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ResponseBodyReaderTest {

    private ResponseBodyReader responseBodyReader;

    @BeforeEach
    void setUp() {
        responseBodyReader = new ResponseBodyReader();
    }

    @Test
    void shouldThrowException_whenMaxSizeIsZero() {
        assertThatThrownBy(() -> responseBodyReader.read(new ByteArrayInputStream("Hello".getBytes()), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Maximum size must be greater than zero.");
    }

    @Test
    void shouldThrowException_whenMaxSizeIsNegative() {
        assertThatThrownBy(() -> responseBodyReader.read(new ByteArrayInputStream("Hello".getBytes()), -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Maximum size must be greater than zero.");
    }

    @Test
    void shouldReturnEmptyArray_whenInputStreamIsEmpty() throws IOException {
        byte[] bytesRead = responseBodyReader.read(new ByteArrayInputStream("".getBytes()), 100);
        assertThat(bytesRead).isEmpty();
    }

    @Test
    void shouldReturnCompleteResponse_whenResponseIsSmallerThanMaxSize() throws IOException {
        byte[] readBytes = responseBodyReader.read(new ByteArrayInputStream("Hello".getBytes()), 100);
        assertArrayEquals("Hello".getBytes(), readBytes);
    }

    @Test
    void shouldReturnCompleteResponse_whenResponseIsExactlyMaxSize() throws IOException {
        byte[] bytesRead = responseBodyReader.read(new ByteArrayInputStream(new byte[]{12, 32, 40}), 3);
        assertArrayEquals(new byte[]{12, 32, 40}, bytesRead);
    }

    @Test
    void shouldThrowResponseTooLargeException_whenResponseExceedsMaxSize() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(new byte[]{15, 32, 17, 41, 12});
        assertThatThrownBy(() -> responseBodyReader.read(byteArrayInputStream, 3))
                .isInstanceOf(ResponseTooLargeException.class)
                .hasMessage("The response body exceeds the maximum allowed size.");
    }
}