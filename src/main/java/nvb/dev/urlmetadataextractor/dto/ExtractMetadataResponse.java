package nvb.dev.urlmetadataextractor.dto;

public record ExtractMetadataResponse(int statusCode,
                                      String contentType,
                                      String body) {
}
