package kz.samat.smarthealth.doctorservice.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param url       MinIO's S3 API, such as http://localhost:9000
 * @param accessKey access key of a MinIO user that may create the bucket and read and write its objects
 * @param secretKey that user's secret key
 * @param bucket    bucket for license files; created on the first upload if it does not exist
 */
@Validated
@ConfigurationProperties(prefix = "minio")
public record MinioProperties(@NotBlank String url, @NotBlank String accessKey, @NotBlank String secretKey,
                              @NotBlank String bucket) {
}
