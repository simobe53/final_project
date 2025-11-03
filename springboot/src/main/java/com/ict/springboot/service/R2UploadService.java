package com.ict.springboot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class R2UploadService {

    private final S3Client s3Client;

    @Value("${cloudflare.r2.bucket}")
    private String bucketName;

    @Value("${cloudflare.r2.public-base-url}")
    private String pubBaseUrl;

    public String uploadBase64(byte[] bytes, String fileName, String contentType) {
        try {
            String safeFileName = UUID.randomUUID() + "_" + fileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(safeFileName)
                            .acl(ObjectCannedACL.PUBLIC_READ)
                            .contentType(contentType)
                            .contentLength((long) bytes.length)
                            .build(),
                    RequestBody.fromBytes(bytes)
            );

            String encodedFileName = URLEncoder.encode(safeFileName, StandardCharsets.UTF_8).replace("+", "%20");
            String fileUrl = String.format("%s/%s", pubBaseUrl, encodedFileName);
            return fileUrl;

        } catch (Exception e) {
            throw new RuntimeException("Base64 업로드 실패: " + e.getMessage(), e);
        }
    }
    public void deleteFileByUrl(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);

            if (key == null || key.isBlank()) {
                log.warn("⚠️ 잘못된 URL, 삭제 생략: {}", fileUrl);
                return;
            }

            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());

            log.info("🧹 R2 파일 삭제 완료: {}", key);
        } catch (Exception e) {
            log.error("❌ R2 파일 삭제 실패 ({}): {}", fileUrl, e.getMessage(), e);
        }
    }
        private String extractKeyFromUrl(String fileUrl) {
        try {
            URI uri = new URI(fileUrl);
            String path = uri.getPath(); // ex: /abc123_file.png
            if (path.startsWith("/")) path = path.substring(1);
            return path;
        } catch (Exception e) {
            log.error("URL 파싱 실패: {}", fileUrl);
            return null;
        }
    }
}
