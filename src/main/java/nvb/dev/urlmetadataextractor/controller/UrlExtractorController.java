package nvb.dev.urlmetadataextractor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nvb.dev.urlmetadataextractor.dto.ExtractMetadataRequest;
import nvb.dev.urlmetadataextractor.dto.ExtractMetadataResponse;
import nvb.dev.urlmetadataextractor.service.UrlExtractorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1")
@RequiredArgsConstructor
public class UrlExtractorController {

    private final UrlExtractorService urlExtractorService;

    @PostMapping(path = "/metadata")
    public ExtractMetadataResponse extractMetadata(@Valid @RequestBody ExtractMetadataRequest request) {
        return urlExtractorService.extractMetadata(request);
    }

}
