package nvb.dev.urlmetadataextractor.exception;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorApiResponse> handleResourceAccessException(ResourceAccessException ex) {
        boolean isTimeout = isTimeoutInChain(ex);
        HttpStatus status = isTimeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY;

        Throwable mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(ex);
        String message = mostSpecificCause.getMessage() != null ? mostSpecificCause.getMessage() : ex.getMessage();

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
