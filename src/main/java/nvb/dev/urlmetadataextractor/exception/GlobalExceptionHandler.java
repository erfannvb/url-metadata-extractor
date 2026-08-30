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
