package com.thoughtsandopinions.apigateway.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class MinioService {

    private final MinioClient minioClient;
    
    @Value("${minio.bucket:profiles}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public Mono<String> uploadProfilePicture(FilePart filePart) {
        String originalFilename = filePart.filename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String objectName = UUID.randomUUID().toString() + extension;

        return Mono.fromCallable(() -> Files.createTempFile("profile-", extension))
                .flatMap(tempFile -> 
                    filePart.transferTo(tempFile)
                            .then(Mono.fromCallable(() -> uploadToMinioAndGetUrl(tempFile, objectName))
                                    .subscribeOn(Schedulers.boundedElastic()))
                            .doFinally(signalType -> deleteTempFile(tempFile))
                );
    }

    private String uploadToMinioAndGetUrl(Path tempFile, String objectName) throws Exception {
        try (InputStream inputStream = Files.newInputStream(tempFile)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, Files.size(tempFile), -1L)
                            .contentType("image/jpeg") // Ideally derived from the filePart
                            .build()
            );
        }
        
        // Return the public URL for the uploaded image
        if (minioEndpoint.endsWith("/")) {
            return minioEndpoint + bucketName + "/" + objectName;
        }
        return minioEndpoint + "/" + bucketName + "/" + objectName;
    }

    private void deleteTempFile(Path tempFile) {
        try {
            Files.deleteIfExists(tempFile);
        } catch (Exception e) {
            // Log warning about failed cleanup
            System.err.println("Failed to delete temp file: " + tempFile.toString());
        }
    }

    public Mono<Void> deleteProfilePicture(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return Mono.empty();
        }
        return Mono.fromRunnable(() -> {
            try {
                // Extract object name from URL
                // URL format: http://localhost:9000/bucketName/objectName
                String[] parts = fileUrl.split("/");
                String objectName = parts[parts.length - 1];
                
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                );
            } catch (Exception e) {
                System.err.println("Failed to delete old profile picture from MinIO: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
