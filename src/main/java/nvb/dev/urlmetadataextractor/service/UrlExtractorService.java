package nvb.dev.urlmetadataextractor.service;

import lombok.RequiredArgsConstructor;
import nvb.dev.urlmetadataextractor.dto.ExtractMetadataRequest;
import nvb.dev.urlmetadataextractor.dto.ExtractMetadataResponse;
import nvb.dev.urlmetadataextractor.exception.InvalidUrlException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.apache.commons.validator.routines.UrlValidator;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class UrlExtractorService {

    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{"http://", "https://"});

    private final RestClient restClient;

    public ExtractMetadataResponse extractMetadata(ExtractMetadataRequest request) {
        String url = request.url();
        if (!URL_VALIDATOR.isValid(url))
            throw new InvalidUrlException("Invalid URL.");

        ResponseEntity<String> responseEntity = restClient.get()
                .uri(URI.create(url))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {
                })
                .toEntity(String.class);

        int statusCode = responseEntity.getStatusCode().value();

        String contentType;
        MediaType mediaType = responseEntity.getHeaders().getContentType();
        if (mediaType == null)
            contentType = null;
        else
            contentType = mediaType.toString();

        String body = responseEntity.getBody();

        return new ExtractMetadataResponse(statusCode, contentType, body);
    }

}
