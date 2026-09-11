package nvb.dev.urlmetadataextractor.controller;

import nvb.dev.urlmetadataextractor.dto.ExtractMetadataRequest;
import nvb.dev.urlmetadataextractor.dto.ExtractMetadataResponse;
import nvb.dev.urlmetadataextractor.exception.InvalidUrlException;
import nvb.dev.urlmetadataextractor.exception.ResponseTooLargeException;
import nvb.dev.urlmetadataextractor.service.UrlExtractorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpConnectTimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UrlExtractorController.class)
class UrlExtractorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UrlExtractorService urlExtractorService;

    @Test
    void shouldReturnMetadata_whenRequestIsValid() throws Exception {
        ExtractMetadataResponse response = new ExtractMetadataResponse(200, "text/html", "<html>Hello</html>");

        when(urlExtractorService.extractMetadata(any(ExtractMetadataRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExtractMetadataRequest("https://example.com")))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.contentType").value("text/html"))
                .andExpect(jsonPath("$.body").value("<html>Hello</html>"));
    }

    @Test
    void shouldReturnBadRequest_whenUrlIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString("{}"))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(urlExtractorService);
    }

    @Test
    void shouldReturnBadRequest_whenUrlIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExtractMetadataRequest("")))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(urlExtractorService);
    }

    @Test
    void shouldReturnBadRequest_whenUrlContainsOnlyWhitespace() throws Exception {
        mockMvc.perform(post("/api/v1/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExtractMetadataRequest("  ")))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(urlExtractorService);
    }

    @Test
    void shouldReturnBadRequest_whenRequestBodyIsInvalidJson() throws Exception {
        mockMvc.perform(post("/api/v1/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "INVALID_URL",
                                }""")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(urlExtractorService);
    }

    @Test
    void shouldReturnBadRequest_whenServiceThrowsInvalidUrlException() throws Exception {
        when(urlExtractorService.extractMetadata(any(ExtractMetadataRequest.class)))
                .thenThrow(new InvalidUrlException("Invalid URL."));

        mockMvc.perform(post("/api/v1/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExtractMetadataRequest("ftp://example.com")))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Invalid URL."));
    }

    @Test
    void shouldReturnPayloadTooLarge_whenServiceThrowsResponseTooLargeException() throws Exception {
        when(urlExtractorService.extractMetadata(any(ExtractMetadataRequest.class)))
                .thenThrow(new ResponseTooLargeException("The response body exceeds the maximum allowed size."));

        mockMvc.perform(post("/api/v1/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExtractMetadataRequest("https://example.com")))
                )
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.statusCode").value(413))
                .andExpect(jsonPath("$.message").value("The response body exceeds the maximum allowed size."));
    }

    @Test
    void shouldReturnBadGateway_whenServiceThrowsResourceAccessException() throws Exception {
        when(urlExtractorService.extractMetadata(any(ExtractMetadataRequest.class)))
                .thenThrow(new ResourceAccessException("Could not connect to the target URL."));

        mockMvc.perform(post("/api/v1/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExtractMetadataRequest("https://example.com")))
                )
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.statusCode").value(502))
                .andExpect(jsonPath("$.message").value("Could not connect to the target URL."));
    }

    @Test
    void shouldReturnGatewayTimeout_whenServiceThrowsTimeoutException() throws Exception {
        when(urlExtractorService.extractMetadata(any(ExtractMetadataRequest.class)))
                .thenThrow(new ResourceAccessException("The target URL did not respond in time.", new HttpConnectTimeoutException("")));

        mockMvc.perform(post("/api/v1/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExtractMetadataRequest("https://example.com")))
                )
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.statusCode").value(504))
                .andExpect(jsonPath("$.message").value("The target URL did not respond in time."));
    }

    @Test
    void shouldReturnInternalServerError_whenServiceThrowsUnexpectedException() throws Exception {
        when(urlExtractorService.extractMetadata(any(ExtractMetadataRequest.class)))
                .thenThrow(new RuntimeException());

        mockMvc.perform(post("/api/v1/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExtractMetadataRequest("https://example.com")))
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."));
    }
}