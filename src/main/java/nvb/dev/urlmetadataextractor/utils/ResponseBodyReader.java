package nvb.dev.urlmetadataextractor.utils;

import nvb.dev.urlmetadataextractor.exception.ResponseTooLargeException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class ResponseBodyReader {

    private static final int BUFFER_SIZE = 8192;

    public byte[] read(InputStream inputStream, long maxSize) throws IOException {
        if (maxSize <= 0)
            throw new IllegalArgumentException("Maximum size must be greater than zero.");

        byte[] buffer = new byte[BUFFER_SIZE];
        long totalBytesRead = 0;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int) Math.min(BUFFER_SIZE, maxSize));

        while (true) {
            long remaining = maxSize - totalBytesRead;
            if (remaining == 0) {
                int extraByte = inputStream.read();
                if (extraByte != -1)
                    throw new ResponseTooLargeException("The response body exceeds the maximum allowed size.");
                break;
            }

            int bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (bytesRead == -1)
                break;

            outputStream.write(buffer, 0, bytesRead);
            totalBytesRead += bytesRead;
        }

        return outputStream.toByteArray();
    }

}
