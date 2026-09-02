package nvb.dev.urlmetadataextractor.exception;

public class ResponseTooLargeException extends RuntimeException {
    public ResponseTooLargeException(String message) {
        super(message);
    }
}
