package org.sekoph.videoservice.minIO;


import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.sekoph.videoservice.exception.VideoProcessingException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
public class UploadToMiniO {
    private final MinioClient minioClient;

    public UploadToMiniO(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * upload files to minIO storage
     *
     * @param bucketName the target bucket name
     * @param objectKey the Key/path for the object in MinIO
     * @param file the file to upload
     * @param contentType the MIME type of the File
     * @throws Exception if the upload fails
     */

    public void uploadFile(String bucketName, String objectKey, MultipartFile file, String contentType) throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
        // Upload the file
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new VideoProcessingException("Failed to upload file", e);
        }
    }
}
