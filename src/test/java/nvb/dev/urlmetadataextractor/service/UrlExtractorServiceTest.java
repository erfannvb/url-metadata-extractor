package nvb.dev.urlmetadataextractor.service;

import nvb.dev.urlmetadataextractor.config.UrlExtractorProperties;
import nvb.dev.urlmetadataextractor.dto.ExtractMetadataRequest;
import nvb.dev.urlmetadataextractor.dto.ExtractMetadataResponse;
import nvb.dev.urlmetadataextractor.exception.InvalidUrlException;
import nvb.dev.urlmetadataextractor.utils.ResponseBodyReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlExtractorServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private ResponseBodyReader responseBodyReader;

    @Mock
    private UrlExtractorProperties urlExtractorProperties;

    @Mock
    private ClientHttpRequest clientHttpRequest;

    @Mock
    private RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse clientHttpResponse;

    @Mock
    private HttpHeaders httpHeaders;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @InjectMocks
    private UrlExtractorService urlExtractorService;

    @Test
    void shouldThrowInvalidUrlException_whenUrlIsInvalid() {
        ExtractMetadataRequest request = new ExtractMetadataRequest("invalid-url");

        assertThatThrownBy(() -> urlExtractorService.extractMetadata(request))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessage("Invalid URL.");

        verifyNoInteractions(restClient);
    }

    @Test
    void shouldReturnMetadata_whenTargetUrlRespondsSuccessfully() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("Hello".getBytes(StandardCharsets.UTF_8));

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(URI.create("https://example.com"))).thenReturn(requestHeadersSpec);
        when(urlExtractorProperties.getMaxResponseSize()).thenReturn(DataSize.ofBytes(1000));
        when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        when(clientHttpResponse.getHeaders().getContentType()).thenReturn(MediaType.TEXT_PLAIN);
        when(clientHttpResponse.getBody()).thenReturn(byteArrayInputStream);
        when(responseBodyReader.read(byteArrayInputStream, 1000)).thenReturn("Hello".getBytes(StandardCharsets.UTF_8));
        when(requestHeadersSpec.exchange(any())).thenAnswer(invocation -> {
            RestClient.RequestHeadersSpec.ExchangeFunction<ExtractMetadataResponse> exchangeFunction = invocation.getArgument(0);
            return exchangeFunction.exchange(clientHttpRequest, clientHttpResponse);
        });

        ExtractMetadataRequest request = new ExtractMetadataRequest("https://example.com");
        ExtractMetadataResponse response = urlExtractorService.extractMetadata(request);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo("text/plain");
        assertThat(response.body()).isEqualTo("Hello");

        verify(restClient, times(1)).get();
        verify(requestHeadersUriSpec, times(1)).uri(URI.create("https://example.com"));
        verify(responseBodyReader, times(1)).read(byteArrayInputStream, 1000);
    }

    @Test
    void shouldReturnMetadataWithUtf8Body_whenContentTypeIsMissing() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("Hello".getBytes(StandardCharsets.UTF_8));

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(URI.create("https://example.com"))).thenReturn(requestHeadersSpec);
        when(urlExtractorProperties.getMaxResponseSize()).thenReturn(DataSize.ofBytes(1000));
        when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        when(clientHttpResponse.getHeaders().getContentType()).thenReturn(null);
        when(clientHttpResponse.getBody()).thenReturn(byteArrayInputStream);
        when(responseBodyReader.read(byteArrayInputStream, 1000)).thenReturn("Hello".getBytes(StandardCharsets.UTF_8));
        when(requestHeadersSpec.exchange(any())).thenAnswer(invocation -> {
            RestClient.RequestHeadersSpec.ExchangeFunction<ExtractMetadataResponse> exchangeFunction = invocation.getArgument(0);
            return exchangeFunction.exchange(clientHttpRequest, clientHttpResponse);
        });

        ExtractMetadataRequest request = new ExtractMetadataRequest("https://example.com");
        ExtractMetadataResponse response = urlExtractorService.extractMetadata(request);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isNull();
        assertThat(response.body()).isEqualTo("Hello");

        verify(restClient).get();
        verify(requestHeadersUriSpec).uri(URI.create("https://example.com"));
        verify(responseBodyReader).read(byteArrayInputStream, 1000);
    }

    @Test
    void shouldReturnEmptyBody_whenResponseBodyIsNull() throws IOException {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(URI.create("https://example.com"))).thenReturn(requestHeadersSpec);
        when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        when(clientHttpResponse.getHeaders().getContentType()).thenReturn(MediaType.TEXT_PLAIN);
        when(clientHttpResponse.getBody()).thenReturn(null);
        when(requestHeadersSpec.exchange(any())).thenAnswer(invocation -> {
            RestClient.RequestHeadersSpec.ExchangeFunction<ExtractMetadataResponse> exchangeFunction = invocation.getArgument(0);
            return exchangeFunction.exchange(clientHttpRequest, clientHttpResponse);
        });

        ExtractMetadataRequest request = new ExtractMetadataRequest("https://example.com");
        ExtractMetadataResponse response = urlExtractorService.extractMetadata(request);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo("text/plain");
        assertThat(response.body()).isEqualTo("");

        verify(restClient).get();
        verify(requestHeadersUriSpec).uri(URI.create("https://example.com"));
        verify(responseBodyReader, never()).read(any(InputStream.class), anyLong());
    }

    @Test
    void shouldDecodeBodyUsingExplicitCharset_whenContentTypeSpecifiesCharset() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("Hello".getBytes(StandardCharsets.UTF_16));

        MediaType mediaType = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_16);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(URI.create("https://example.com"))).thenReturn(requestHeadersSpec);
        when(urlExtractorProperties.getMaxResponseSize()).thenReturn(DataSize.ofBytes(1000));
        when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        when(clientHttpResponse.getHeaders().getContentType()).thenReturn(mediaType);
        when(clientHttpResponse.getBody()).thenReturn(byteArrayInputStream);
        when(responseBodyReader.read(byteArrayInputStream, 1000)).thenReturn("Hello".getBytes(StandardCharsets.UTF_16));
        when(requestHeadersSpec.exchange(any())).thenAnswer(invocation -> {
            RestClient.RequestHeadersSpec.ExchangeFunction<ExtractMetadataResponse> exchangeFunction = invocation.getArgument(0);
            return exchangeFunction.exchange(clientHttpRequest, clientHttpResponse);
        });

        ExtractMetadataRequest request = new ExtractMetadataRequest("https://example.com");
        ExtractMetadataResponse response = urlExtractorService.extractMetadata(request);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo("text/plain;charset=UTF-16");
        assertThat(response.body()).isEqualTo("Hello");

        verify(restClient, times(1)).get();
        verify(requestHeadersUriSpec, times(1)).uri(URI.create("https://example.com"));
        verify(responseBodyReader, times(1)).read(byteArrayInputStream, 1000);
    }

    @Test
    void shouldDecodeBodyUsingUtf8_whenContentTypeDoesNotSpecifyCharset() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("Hello".getBytes());

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(URI.create("https://example.com"))).thenReturn(requestHeadersSpec);
        when(urlExtractorProperties.getMaxResponseSize()).thenReturn(DataSize.ofBytes(1000));
        when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(clientHttpResponse.getHeaders()).thenReturn(httpHeaders);
        when(clientHttpResponse.getHeaders().getContentType()).thenReturn(MediaType.TEXT_PLAIN);
        when(clientHttpResponse.getBody()).thenReturn(byteArrayInputStream);
        when(responseBodyReader.read(byteArrayInputStream, 1000)).thenReturn("Hello".getBytes());
        when(requestHeadersSpec.exchange(any())).thenAnswer(invocation -> {
            RestClient.RequestHeadersSpec.ExchangeFunction<ExtractMetadataResponse> exchangeFunction = invocation.getArgument(0);
            return exchangeFunction.exchange(clientHttpRequest, clientHttpResponse);
        });

        ExtractMetadataRequest request = new ExtractMetadataRequest("https://example.com");
        ExtractMetadataResponse response = urlExtractorService.extractMetadata(request);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo("text/plain");
        assertThat(response.body()).isEqualTo("Hello");

        verify(restClient, times(1)).get();
        verify(requestHeadersUriSpec, times(1)).uri(URI.create("https://example.com"));
        verify(responseBodyReader, times(1)).read(byteArrayInputStream, 1000);
    }
}