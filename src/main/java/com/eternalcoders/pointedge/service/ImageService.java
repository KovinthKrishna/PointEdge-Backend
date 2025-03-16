package com.eternalcoders.pointedge.service;

import com.eternalcoders.pointedge.entity.Product;
import com.eternalcoders.pointedge.repository.ProductRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ImageService {
    private final ProductRepository productRepository;
    private final String UPLOAD_DIR = "uploads/products/";

    public ImageService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Resource getProductImage(String filename) throws IOException {
        Path filePath = Paths.get(UPLOAD_DIR + filename);
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        }
        return null;
    }

    public boolean saveProductImage(Long productId, MultipartFile file) throws IOException {
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            String uniqueKey = UUID.randomUUID().toString();
            String newFileName = productId + "_" + uniqueKey + extension;

            Path filePath = Paths.get(UPLOAD_DIR + newFileName);

            if (product.getImageName() != null) {
                File oldImageFile = new File(UPLOAD_DIR + product.getImageName());
                if (oldImageFile.exists()) {
                    oldImageFile.delete();
                }
            }

            Files.write(filePath, file.getBytes());

            product.setImageName(newFileName);
            productRepository.save(product);
            return true;
        }
        return false;
    }

    public boolean deleteProductImage(Long productId) throws IOException {
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null && product.getImageName() != null) {
            File imageFile = new File(UPLOAD_DIR + product.getImageName());
            if (imageFile.exists()) {
                imageFile.delete();
            }
            product.setImageName(null);
            productRepository.save(product);
            return true;
        }
        return false;
    }
}
