package com.suleman.poc.adapters.storage;

import com.suleman.poc.domain.ports.outbound.InvoiceStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import java.time.Duration;

@Component
public class S3InvoiceStorageAdapter implements InvoiceStoragePort {

    private static final Logger log = LoggerFactory.getLogger(S3InvoiceStorageAdapter.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final int expirationMinutes;

    @Autowired
    public S3InvoiceStorageAdapter(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${app.aws.s3.invoice-bucket}") String bucketName,
            @Value("${app.aws.s3.presigned-url-expiration-minutes:15}") int expirationMinutes) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.expirationMinutes = expirationMinutes;
    }

    @Override
    public void uploadInvoice(String s3Key, String content) {
        log.info("📁 Uploading invoice to S3: bucket={}, key={}", bucketName, s3Key);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType("text/plain")
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromString(content));
        log.info("✅ Uploaded invoice successfully: key={}", s3Key);
    }

    @Override
    public String getPresignedDownloadUrl(String s3Key) {
        log.info("🔗 Generating presigned URL for S3 key: {}", s3Key);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }
}
