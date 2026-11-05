package com.tuhospedaje.controller;

import com.tuhospedaje.dto.upload.UploadResult;
import com.tuhospedaje.exception.UploadException;
import com.tuhospedaje.service.ImageStorageService;
import com.tuhospedaje.service.command.UploadImageCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/upload")
@Tag(name = "Upload", description = "Local image upload — ADMIN only")
public class UploadController {

    private final ImageStorageService imageStorageService;

    public UploadController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Upload an image",
            description = "Stores an image file on the server's local disk and returns the URL the image is " +
                          "served from and the stored file name. Accepts multipart/form-data with a 'file' " +
                          "part holding a JPEG, PNG, WebP or GIF image of at most 5MB. Requires ADMIN role."
    )
    @SecurityRequirement(name = "csrfToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image stored successfully — returns the /uploads/ URL and the stored file name"),
            @ApiResponse(responseCode = "400", description = "Empty file, or a content type outside JPEG/PNG/WebP/GIF", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required", content = @Content),
            @ApiResponse(responseCode = "413", description = "File exceeds spring.servlet.multipart.max-file-size", content = @Content),
            @ApiResponse(responseCode = "500", description = "The image could not be written to the storage directory", content = @Content),
    })
    public ResponseEntity<UploadResult> upload(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(imageStorageService.store(
                    new UploadImageCommand(file.getBytes(), file.getContentType())
            ));
        } catch (IOException e) {
            throw new UploadException("No se pudo leer la imagen", e);
        }
    }
}
