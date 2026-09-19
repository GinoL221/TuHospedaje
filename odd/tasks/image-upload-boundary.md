# Image Upload Boundary

## Goal

Keep `/api/upload` and Cloudinary behavior. Stop exposing `MultipartFile` on the application service.

## Tasks

- [ ] 1. Map multipart in the controller; service accepts `UploadImageCommand`.
