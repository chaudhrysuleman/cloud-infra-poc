package com.suleman.poc.domain.ports.outbound;

public interface InvoiceStoragePort {
    void uploadInvoice(String s3Key, String content);
    String getPresignedDownloadUrl(String s3Key);
}
