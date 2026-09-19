package com.tuhospedaje.service.command;

public record UploadImageCommand(byte[] content, String contentType) {
}
