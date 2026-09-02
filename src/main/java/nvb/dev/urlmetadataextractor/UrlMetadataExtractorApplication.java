package nvb.dev.urlmetadataextractor;

import nvb.dev.urlmetadataextractor.config.UrlExtractorProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(UrlExtractorProperties.class)
public class UrlMetadataExtractorApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlMetadataExtractorApplication.class, args);
    }

}
