package nvb.dev.urlmetadataextractor.dto;

import jakarta.validation.constraints.NotBlank;

public record ExtractMetadataRequest(
        @NotBlank(message = "URL cannot be blank.")
        String url
) {
}
