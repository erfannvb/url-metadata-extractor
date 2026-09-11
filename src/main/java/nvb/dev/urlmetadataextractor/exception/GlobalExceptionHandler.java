package nvb.dev.urlmetadataextractor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMEOUT_MSG = "The target URL did not respond in time.";
    private static final String OTHER_MSG = "Could not connect to the target URL.";

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorApiResponse> handleResourceAccessException(ResourceAccessException ex) {
        boolean isTimeout = isTimeoutInChain(ex);

        HttpStatus status = isTimeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY;
        String message = isTimeout ? TIMEOUT_MSG : OTHER_MSG;

        ErrorApiResponse response = new ErrorApiResponse(status.value(), message);
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(InvalidUrlException.class)
    public ResponseEntity<ErrorApiResponse> handleInvalidUrlException(InvalidUrlException ex) {
        ErrorApiResponse response = new ErrorApiResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(ResponseTooLargeException.class)
    public ResponseEntity<ErrorApiResponse> handleResponseTooLargeException(ResponseTooLargeException ex) {
        ErrorApiResponse response = new ErrorApiResponse(HttpStatus.CONTENT_TOO_LARGE.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorApiResponse> handleRuntimeErrorException(RuntimeException ex) {
        ErrorApiResponse response = new ErrorApiResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private boolean isTimeoutInChain(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof HttpTimeoutException ||
                    current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
