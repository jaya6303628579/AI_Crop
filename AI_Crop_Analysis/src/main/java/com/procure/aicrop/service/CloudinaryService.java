package com.procure.aicrop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Value("${cloudinary.folder.soil-analysis}")
    private String soilAnalysisFolder;

    public String uploadSoilImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        try {
            log.info("Uploading soil image to Cloudinary: {}", file.getOriginalFilename());

            // Build upload URL: https://api.cloudinary.com/v1_1/{cloud_name}/image/upload
            String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";

            // Prepare form data
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource());
            body.add("api_key", apiKey);
            body.add("folder", soilAnalysisFolder);
            body.add("public_id", generatePublicId(file.getOriginalFilename()));

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            // Upload to Cloudinary
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                String imageUrl = jsonResponse.get("secure_url").asText();
                log.info("Soil image uploaded successfully: {}", imageUrl);
                return imageUrl;
            } else {
                log.error("Failed to upload image: HTTP {}", response.getStatusCode());
                throw new RuntimeException("Failed to upload image to Cloudinary");
            }

        } catch (IOException e) {
            log.error("Error uploading soil image to Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload image to Cloudinary: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error uploading soil image to Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload image to Cloudinary: " + e.getMessage());
        }
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            log.warn("Attempted to delete null or empty image URL");
            return;
        }

        try {
            log.info("Deleting image from Cloudinary: {}", imageUrl);

            // Extract public ID from URL
            String publicId = extractPublicId(imageUrl);

            if (publicId != null && !publicId.isEmpty()) {
                String deleteUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/destroy";

                // Create authorization header
                String auth = apiKey + ":" + apiSecret;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("public_id", publicId);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                headers.set("Authorization", "Basic " + encodedAuth);

                HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(deleteUrl, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Image deleted successfully from Cloudinary: {}", publicId);
                }
            }
        } catch (Exception e) {
            log.error("Error deleting image from Cloudinary: {}", e.getMessage(), e);
        }
    }

    private String generatePublicId(String filename) {
        if (filename == null) {
            filename = "soil_image";
        }
        String nameWithoutExtension = filename.substring(0, filename.lastIndexOf('.') > 0 ? filename.lastIndexOf('.') : filename.length());
        return nameWithoutExtension + "_" + System.currentTimeMillis();
    }

    private String extractPublicId(String imageUrl) {
        // Cloudinary URL format: https://res.cloudinary.com/cloudname/image/upload/v123/folder/publicid.jpg
        if (imageUrl == null || !imageUrl.contains("/")) {
            return null;
        }

        try {
            String[] parts = imageUrl.split("/");
            String filename = parts[parts.length - 1]; // Get last part (filename.jpg)
            return filename.substring(0, filename.lastIndexOf('.')); // Remove extension
        } catch (Exception e) {
            log.error("Error extracting public ID from URL: {}", imageUrl);
            return null;
        }
    }
}
