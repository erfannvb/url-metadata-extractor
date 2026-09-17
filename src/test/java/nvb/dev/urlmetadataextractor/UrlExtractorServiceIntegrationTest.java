package nvb.dev.urlmetadataextractor;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import nvb.dev.urlmetadataextractor.dto.ExtractMetadataRequest;
import nvb.dev.urlmetadataextractor.dto.ExtractMetadataResponse;
import nvb.dev.urlmetadataextractor.exception.ResponseTooLargeException;
import nvb.dev.urlmetadataextractor.service.UrlExtractorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.ResourceAccessException;

import java.net.http.HttpTimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
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
        assertThat(response.contentType()).isEqualTo("text/html");
        assertThat(response.body()).isEqualTo("<html>Hello</html>");
    }

    @Test
    void shouldReturnErrorResponse_whenRemoteServerReturns500() {
        wireMockExtension.stubFor(
                get(urlEqualTo("/some-page"))
                        .willReturn(
                                aResponse()
                                        .withStatus(500)
                                        .withHeader("Content-Type", "text/html")
                                        .withBody("<html>An unexpected error occurred.</html>")
                        )
        );

        String url = "http://127.0.0.1:" + wireMockExtension.getPort() + "/some-page";
        ExtractMetadataResponse response = urlExtractorService.extractMetadata(new ExtractMetadataRequest(url));

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.contentType()).isEqualTo("text/html");
        assertThat(response.body()).isEqualTo("<html>An unexpected error occurred.</html>");
    }

    @Test
    void shouldThrowResourceAccessException_whenRemoteServerTimesOut() {
        wireMockExtension.stubFor(
                get(urlEqualTo("/some-page"))
                        .willReturn(
                                aResponse()
                                        .withFixedDelay(15000)
                                        .withHeader("Content-Type", "text/html")
                                        .withBody("<html>The target URL did not respond in time.</html>")
                        )
        );

        String url = "http://127.0.0.1:" + wireMockExtension.getPort() + "/some-page";
        assertThatThrownBy(() -> urlExtractorService.extractMetadata(new ExtractMetadataRequest(url)))
                .isInstanceOf(ResourceAccessException.class)
                .hasCauseInstanceOf(HttpTimeoutException.class);
    }

    @Test
    void shouldThrowResponseTooLargeException_whenRemoteServerReturnsLargeBody() {
        wireMockExtension.stubFor(
                get(urlEqualTo("/some-page"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "text/html")
                                        .withBody("<html>" + "TEST_DATA".repeat(1_000_000) + "</html>")
                        )
        );

        String url = "http://127.0.0.1:" + wireMockExtension.getPort() + "/some-page";
        assertThatThrownBy(() -> urlExtractorService.extractMetadata(new ExtractMetadataRequest(url)))
                .isInstanceOf(ResponseTooLargeException.class);
    }

    @Test
    void shouldFollowRedirect_whenRemoteServerRedirects() {
        wireMockExtension.stubFor(
                get(urlEqualTo("/old-page"))
                        .willReturn(
                                aResponse()
                                        .withStatus(302)
                                        .withHeader("Location", "/new-page")
                        )
        );

        wireMockExtension.stubFor(
                get(urlEqualTo("/new-page"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "text/html")
                                        .withBody("<html>New Page</html>")
                        )
        );

        String url = "http://127.0.0.1:" + wireMockExtension.getPort() + "/old-page";
        ExtractMetadataResponse response = urlExtractorService.extractMetadata(new ExtractMetadataRequest(url));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo("text/html");
        assertThat(response.body()).isEqualTo("<html>New Page</html>");
    }

    @Test
    void shouldSendUserAgent_whenRequestIsMade() {
        wireMockExtension.stubFor(
                get(urlEqualTo("/some-page"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "text/html")
                                        .withBody("<html>Some Page</html>")
                        )
        );

        String url = "http://127.0.0.1:" + wireMockExtension.getPort() + "/some-page";
        ExtractMetadataResponse response = urlExtractorService.extractMetadata(new ExtractMetadataRequest(url));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo("text/html");
        assertThat(response.body()).isEqualTo("<html>Some Page</html>");

        wireMockExtension.verify(
                getRequestedFor(urlEqualTo("/some-page"))
                        .withHeader("User-Agent", equalTo("UrlMetadataExtractor/1.0"))
        );
    }

    @Test
    void shouldFollowMultipleRedirects_whenRemoteServerRedirects() {
        wireMockExtension.stubFor(
                get(urlEqualTo("/page1"))
                        .willReturn(
                                aResponse()
                                        .withStatus(302)
                                        .withHeader("Location", "/page2")
                        )
        );

        wireMockExtension.stubFor(
                get(urlEqualTo("/page2"))
                        .willReturn(
                                aResponse()
                                        .withStatus(302)
                                        .withHeader("Location", "/page3")
                        )
        );

        wireMockExtension.stubFor(
                get(urlEqualTo("/page3"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "text/html")
                                        .withBody("<html>Final Page</html>")
                        )
        );

        String url = "http://127.0.0.1:" + wireMockExtension.getPort() + "/page1";
        ExtractMetadataResponse response = urlExtractorService.extractMetadata(new ExtractMetadataRequest(url));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo("text/html");
        assertThat(response.body()).isEqualTo("<html>Final Page</html>");

        wireMockExtension.verify(getRequestedFor(urlEqualTo("/page1")));
        wireMockExtension.verify(getRequestedFor(urlEqualTo("/page2")));
        wireMockExtension.verify(getRequestedFor(urlEqualTo("/page3")));
    }

    @Test
    void shouldStopFollowingRedirects_whenRedirectLimitIsReached() {
        wireMockExtension.stubFor(
                get(urlEqualTo("/page1"))
                        .willReturn(
                                aResponse()
                                        .withStatus(302)
                                        .withHeader("Location", "/page2")
                        )
        );

        wireMockExtension.stubFor(
                get(urlEqualTo("/page2"))
                        .willReturn(
                                aResponse()
                                        .withStatus(302)
                                        .withHeader("Location", "/page1")
                        )
        );

        String url = "http://127.0.0.1:" + wireMockExtension.getPort() + "/page1";
        ExtractMetadataResponse response = urlExtractorService.extractMetadata(new ExtractMetadataRequest(url));

        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(302),
                () -> assertThat(response.contentType()).isNull(),
                () -> assertThat(response.body()).isEmpty()
        );

        wireMockExtension.verify(exactly(3), getRequestedFor(urlEqualTo("/page1")));
        wireMockExtension.verify(exactly(2), getRequestedFor(urlEqualTo("/page2")));
    }
}
