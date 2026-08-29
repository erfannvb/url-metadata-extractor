package nvb.dev.urlmetadataextractor.service;

import lombok.RequiredArgsConstructor;
import nvb.dev.urlmetadataextractor.dto.ExtractMetadataRequest;
import nvb.dev.urlmetadataextractor.dto.ExtractMetadataResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class UrlExtractorService {

    private final RestClient restClient;

    public ExtractMetadataResponse extractMetadata(ExtractMetadataRequest request) {
        String url = request.url();

        ResponseEntity<String> responseEntity = restClient.get()
                .uri(URI.create(url))
                .retrieve()
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
