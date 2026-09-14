package nvb.dev.urlmetadataextractor;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import nvb.dev.urlmetadataextractor.dto.ExtractMetadataRequest;
import nvb.dev.urlmetadataextractor.dto.ExtractMetadataResponse;
import nvb.dev.urlmetadataextractor.service.UrlExtractorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UrlExtractorServiceIntegrationTest {

    @Autowired
    private UrlExtractorService urlExtractorService;

    @RegisterExtension
    static WireMockExtension wireMockExtension = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void shouldExtractMetadata_whenRemoteServerReturnsSuccess() {
        wireMockExtension.stubFor(
                get(urlEqualTo("/some-page"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "text/html")
                                        .withBody("<html>Hello</html>")
                        )
        );

        String url = "http://127.0.0.1:" + wireMockExtension.getPort() + "/some-page";
        ExtractMetadataResponse response = urlExtractorService.extractMetadata(new ExtractMetadataRequest(url));

        assertThat(response.statusCode()).isEqualTo(200);
    }
}
