package nvb.dev.urlmetadataextractor.service;

import lombok.RequiredArgsConstructor;
import nvb.dev.urlmetadataextractor.config.UrlExtractorProperties;
import nvb.dev.urlmetadataextractor.dto.ExtractMetadataRequest;
import nvb.dev.urlmetadataextractor.dto.ExtractMetadataResponse;
import nvb.dev.urlmetadataextractor.exception.InvalidUrlException;
import nvb.dev.urlmetadataextractor.utils.ResponseBodyReader;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UrlExtractorService {

    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{"http", "https"});

    private final RestClient restClient;
    private final ResponseBodyReader responseBodyReader;
    private final UrlExtractorProperties urlExtractorProperties;

    public ExtractMetadataResponse extractMetadata(ExtractMetadataRequest request) {
        String url = request.url();
        if (!URL_VALIDATOR.isValid(url))
            throw new InvalidUrlException("Invalid URL.");

        return restClient.get()
                .uri(URI.create(url))
                .exchange((req, resp) -> {

                    int statusCode = resp.getStatusCode().value();

                    String contentType = null;
                    Charset charset;

                    MediaType mediaType = resp.getHeaders().getContentType();
                    if (mediaType == null) {
                        charset = StandardCharsets.UTF_8;
                    } else {
                        contentType = mediaType.toString();
                        Charset foundCharset = mediaType.getCharset();
                        charset = Objects.requireNonNullElse(foundCharset, StandardCharsets.UTF_8);
                    }

                    String actualBody;
                    InputStream body = resp.getBody();
                    if (body == null) {
                        actualBody = "";
                    } else {
                        byte[] readBytes = responseBodyReader.read(body, urlExtractorProperties.getMaxResponseSize().toBytes());
                        actualBody = new String(readBytes, charset);
                    }

                    return new ExtractMetadataResponse(statusCode, contentType, actualBody);
                });
    }

}
