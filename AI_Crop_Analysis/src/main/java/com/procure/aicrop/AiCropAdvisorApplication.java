package com.procure.aicrop;

import com.procure.aicrop.service.CropService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
public class AiCropAdvisorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCropAdvisorApplication.class, args);
    }

    @Bean
    public CommandLineRunner seedSampleCrops(CropService cropService) {
        return args -> cropService.initializeSampleCrops();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
