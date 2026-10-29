package kz.samat.smarthealth.doctorservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Starts PostgreSQL and MinIO in Docker and points the service at them. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18-alpine");
    }

    @Bean
    public MinIOContainer minio() {
        // MinIO publishes its images on quay.io; Testcontainers only knows the Docker Hub name.
        return new MinIOContainer(DockerImageName.parse("quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z")
                .asCompatibleSubstituteFor("minio/minio"));
    }

    @Bean
    public DynamicPropertyRegistrar minioProperties(MinIOContainer minio) {
        return registry -> {
            registry.add("minio.url", minio::getS3URL);
            registry.add("minio.access-key", minio::getUserName);
            registry.add("minio.secret-key", minio::getPassword);
        };
    }
}
