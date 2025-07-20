package com.eternalcoders.pointedge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class ImageStorageService {
    private static final String UPLOAD_DIR = "uploads/";

    public String saveImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) return null;

        String extension = getFileExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString() + "." + extension;
        String fullPath = UPLOAD_DIR + fileName;

        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        Files.write(Paths.get(fullPath), file.getBytes());

        log.info("Saved image to {}", fullPath);
        return fullPath;
    }

    private String getFileExtension(String filename) {
        return filename != null && filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1)
                : "jpg";
    }
}