package nvb.dev.urlmetadataextractor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "url.extractor")
@Configuration
@Getter
@Setter
public class UrlExtractorProperties {
    private DataSize maxResponseSize;
}
