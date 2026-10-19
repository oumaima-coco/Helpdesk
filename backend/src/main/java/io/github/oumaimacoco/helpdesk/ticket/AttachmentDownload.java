package io.github.oumaimacoco.helpdesk.ticket;

import java.io.InputStream;

public record AttachmentDownload(String filename, String contentType, long sizeBytes, InputStream content) {
}