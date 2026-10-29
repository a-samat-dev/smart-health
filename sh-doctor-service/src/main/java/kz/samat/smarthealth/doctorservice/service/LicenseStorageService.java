package kz.samat.smarthealth.doctorservice.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.UUID;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import kz.samat.smarthealth.doctorservice.config.MinioProperties;
import kz.samat.smarthealth.doctorservice.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Keeps license files in a private MinIO bucket, one per doctor. Clients never get MinIO links: they download
 * the file through this service, which checks who is asking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseStorageService {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    private volatile boolean bucketExists;

    /**
     * Stores the doctor's license file and returns its link. A doctor has one license file, so a new one
     * replaces the old one.
     */
    public String store(UUID doctorId, MultipartFile file) {
        String objectName = doctorId + "/license.pdf";
        try (InputStream content = file.getInputStream()) {
            createBucketIfMissing();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectName)
                    .stream(content, file.getSize(), -1)
                    .contentType(MediaType.APPLICATION_PDF_VALUE)
                    .build());
        } catch (MinioException | IOException | GeneralSecurityException e) {
            throw unavailable(e);
        }
        return properties.url() + "/" + properties.bucket() + "/" + objectName;
    }

    /** Reads the file a link returned by {@link #store} points to. */
    public byte[] read(String link) {
        // The link's path is /{bucket}/{object}; reading only the path keeps old links working if MinIO moves.
        String objectName = URI.create(link).getPath().substring(properties.bucket().length() + 2);
        try (InputStream content = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.bucket())
                .object(objectName)
                .build())) {
            return content.readAllBytes();
        } catch (MinioException | IOException | GeneralSecurityException e) {
            throw unavailable(e);
        }
    }

    private void createBucketIfMissing() throws MinioException, IOException, GeneralSecurityException {
        if (bucketExists) {
            return;
        }
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(properties.bucket()).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
        }
        bucketExists = true;
    }

    private ServiceUnavailableException unavailable(Exception e) {
        log.warn("MinIO request failed: {}", e.toString());
        return new ServiceUnavailableException("File storage");
    }
}
