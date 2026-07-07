package com.procure.aicrop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.procure.aicrop.entity.Crop;
import com.procure.aicrop.repository.CropRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CropService {

    private final CropRepository cropRepository;
    private final GroqAIService groqAIService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Crop> getAllCrops() {
        return cropRepository.findAll();
    }

    public Optional<Crop> getCropById(Long id) {
        return cropRepository.findById(id);
    }

    public Optional<Crop> getCropByName(String name) {
        return cropRepository.findByName(name);
    }

    public Optional<Crop> getCropByNameIgnoreCase(String name) {
        return cropRepository.findByNameIgnoreCase(name);
    }

    public Crop createCrop(Crop crop) {
        if (crop.getImageUrl() == null) {
            crop.setImageUrl(guessCropIcon(crop.getName()));
        }
        return cropRepository.save(crop);
    }

    public Crop updateCrop(Long id, Crop cropDetails) {
        return cropRepository.findById(id).map(crop -> {
            crop.setName(cropDetails.getName());
            crop.setDescription(cropDetails.getDescription());
            crop.setGrowingDays(cropDetails.getGrowingDays());
            crop.setWaterRequirement(cropDetails.getWaterRequirement());
            crop.setMinTemperature(cropDetails.getMinTemperature());
            crop.setMaxTemperature(cropDetails.getMaxTemperature());
            crop.setOptimalTemperature(cropDetails.getOptimalTemperature());
            crop.setMinRainfall(cropDetails.getMinRainfall());
            crop.setMaxRainfall(cropDetails.getMaxRainfall());
            crop.setSuitableSoilTypes(cropDetails.getSuitableSoilTypes());
            crop.setSowingMonths(cropDetails.getSowingMonths());
            crop.setAverageYield(cropDetails.getAverageYield());
            crop.setImageUrl(cropDetails.getImageUrl());
            crop.setRemarks(cropDetails.getRemarks());
            crop.setLocalName(cropDetails.getLocalName());
            return cropRepository.save(crop);
        }).orElseThrow(() -> new RuntimeException("Crop not found"));
    }

    public List<Crop> getCropsBySuitableForSoil(String soilType) {
        List<Crop> allCrops = cropRepository.findAll();
        return allCrops.stream()
                .filter(crop -> crop.getSuitableSoilTypes() != null &&
                        crop.getSuitableSoilTypes().contains(soilType))
                .toList();
    }

    /**
     * Asks the AI for more Indian crops beyond what's already in the catalog (covering
     * regional crops like Bengal Gram, Ragi, Coconut etc. common in South India) and
     * upserts them. Used by the "Other crops" option in the crop picker so the catalog
     * can grow on demand instead of being limited to a hardcoded list.
     */
    public List<Crop> discoverMoreCrops() {
        List<String> existingNames = cropRepository.findAll().stream()
                .map(Crop::getName)
                .collect(Collectors.toList());

        String aiResponse = groqAIService.discoverIndianCrops(existingNames);
        log.info("AI crop discovery response length: {}", aiResponse.length());

        List<Crop> added = new ArrayList<>();
        try {
            JsonNode json = objectMapper.readTree(aiResponse);
            JsonNode crops = json.path("crops");

            for (JsonNode cropNode : crops) {
                String name = cropNode.path("name").asText(null);
                if (name == null || name.isBlank()) continue;
                if (cropRepository.findByNameIgnoreCase(name).isPresent()) continue;

                List<String> soilTypes = new ArrayList<>();
                if (cropNode.has("suitable_soil_types") && cropNode.get("suitable_soil_types").isArray()) {
                    cropNode.get("suitable_soil_types").forEach(s -> soilTypes.add(s.asText()));
                }
                List<String> sowingMonths = new ArrayList<>();
                if (cropNode.has("sowing_months") && cropNode.get("sowing_months").isArray()) {
                    cropNode.get("sowing_months").forEach(s -> sowingMonths.add(s.asText()));
                }

                Crop crop = Crop.builder()
                        .name(name)
                        .localName(cropNode.has("local_name") ? cropNode.get("local_name").asText(null) : null)
                        .description(cropNode.path("description").asText("Indian crop discovered via AI"))
                        .growingDays(cropNode.has("growing_days") ? cropNode.get("growing_days").asInt() : null)
                        .waterRequirement(cropNode.has("water_requirement_mm") ? cropNode.get("water_requirement_mm").asDouble() : null)
                        .minTemperature(cropNode.has("min_temperature") ? cropNode.get("min_temperature").asDouble() : null)
                        .maxTemperature(cropNode.has("max_temperature") ? cropNode.get("max_temperature").asDouble() : null)
                        .optimalTemperature(cropNode.has("optimal_temperature") ? cropNode.get("optimal_temperature").asDouble() : null)
                        .minRainfall(cropNode.has("min_rainfall_mm") ? cropNode.get("min_rainfall_mm").asDouble() : null)
                        .maxRainfall(cropNode.has("max_rainfall_mm") ? cropNode.get("max_rainfall_mm").asDouble() : null)
                        .suitableSoilTypes(soilTypes)
                        .sowingMonths(sowingMonths)
                        .averageYield(cropNode.has("average_yield") ? cropNode.get("average_yield").asDouble() : null)
                        .imageUrl(cropNode.path("icon").asText("🌱"))
                        .build();

                added.add(cropRepository.save(crop));
            }
        } catch (Exception e) {
            log.error("Failed to parse AI crop discovery response", e);
        }

        return added;
    }

    /**
     * Best-effort icon lookup for crops created dynamically by AI recommendations
     * (which only provide a name), so they don't show up with a blank icon.
     */
    public static String guessCropIcon(String cropName) {
        if (cropName == null) return "🌱";
        String key = cropName.trim().toLowerCase();
        for (IndianCrop seed : INDIAN_CROPS) {
            if (seed.name.toLowerCase().contains(key) || key.contains(seed.name.toLowerCase())) {
                return seed.icon;
            }
        }
        return "🌱"; // generic seedling fallback
    }

    /**
     * Seeds/backfills the Indian crop catalog. Existing crops (including ones the AI
     * auto-created from recommendations with only a name, e.g. "Sorghum") are matched
     * by name and backfilled rather than duplicated, since plantings/recommendations
     * already hold foreign keys to those rows.
     */
    public void ensureIndianCropCatalog() {
        for (IndianCrop seed : INDIAN_CROPS) {
            Crop existing = cropRepository.findByNameIgnoreCase(seed.name).orElse(null);
            if (existing == null) {
                cropRepository.save(seed.toEntity());
            } else {
                boolean changed = false;
                if (existing.getImageUrl() == null) { existing.setImageUrl(seed.icon); changed = true; }
                if (existing.getLocalName() == null) { existing.setLocalName(seed.localName); changed = true; }
                if (existing.getDescription() == null) { existing.setDescription(seed.description); changed = true; }
                if (existing.getGrowingDays() == null) { existing.setGrowingDays(seed.growingDays); changed = true; }
                if (existing.getWaterRequirement() == null) { existing.setWaterRequirement(seed.waterRequirement); changed = true; }
                if (existing.getMinTemperature() == null) { existing.setMinTemperature(seed.minTemperature); changed = true; }
                if (existing.getMaxTemperature() == null) { existing.setMaxTemperature(seed.maxTemperature); changed = true; }
                if (existing.getOptimalTemperature() == null) { existing.setOptimalTemperature(seed.optimalTemperature); changed = true; }
                if (existing.getMinRainfall() == null) { existing.setMinRainfall(seed.minRainfall); changed = true; }
                if (existing.getMaxRainfall() == null) { existing.setMaxRainfall(seed.maxRainfall); changed = true; }
                if (existing.getSuitableSoilTypes() == null || existing.getSuitableSoilTypes().isEmpty()) {
                    existing.setSuitableSoilTypes(new java.util.ArrayList<>(seed.suitableSoilTypes)); changed = true;
                }
                if (existing.getSowingMonths() == null || existing.getSowingMonths().isEmpty()) {
                    existing.setSowingMonths(new java.util.ArrayList<>(seed.sowingMonths)); changed = true;
                }
                if (existing.getAverageYield() == null) { existing.setAverageYield(seed.averageYield); changed = true; }
                if (changed) {
                    cropRepository.save(existing);
                }
            }
        }
    }

    private record IndianCrop(
            String name, String icon, String localName, String description, Integer growingDays, Double waterRequirement,
            Double minTemperature, Double maxTemperature, Double optimalTemperature,
            Double minRainfall, Double maxRainfall, List<String> suitableSoilTypes,
            List<String> sowingMonths, Double averageYield) {

        Crop toEntity() {
            return Crop.builder()
                    .name(name)
                    .localName(localName)
                    .description(description)
                    .growingDays(growingDays)
                    .waterRequirement(waterRequirement)
                    .minTemperature(minTemperature)
                    .maxTemperature(maxTemperature)
                    .optimalTemperature(optimalTemperature)
                    .minRainfall(minRainfall)
                    .maxRainfall(maxRainfall)
                    .suitableSoilTypes(new java.util.ArrayList<>(suitableSoilTypes))
                    .sowingMonths(new java.util.ArrayList<>(sowingMonths))
                    .averageYield(averageYield)
                    .imageUrl(icon)
                    .build();
        }
    }

    private static final List<IndianCrop> INDIAN_CROPS = List.of(
            new IndianCrop("Rice", "🍚", "వరి", "Staple Kharif crop requiring standing water and warm, humid climate",
                    120, 1200.0, 20.0, 40.0, 27.0, 150.0, 300.0,
                    List.of("LOAMY", "CLAY", "SILTY"), List.of("June", "July", "August"), 50.0),
            new IndianCrop("Wheat", "🌾", "గోధుమ", "Major Rabi cereal grown widely across North India",
                    140, 400.0, 15.0, 25.0, 20.0, 40.0, 100.0,
                    List.of("LOAMY", "CLAY"), List.of("October", "November"), 40.0),
            new IndianCrop("Maize", "🌽", "మొక్కజొన్న", "Versatile cereal grown in both Kharif and Rabi seasons",
                    110, 500.0, 15.0, 35.0, 25.0, 60.0, 200.0,
                    List.of("LOAMY", "SANDY"), List.of("April", "May", "June"), 35.0),
            new IndianCrop("Sugarcane", "🎋", "చెరకు", "Long-duration cash crop needing heavy irrigation",
                    365, 2000.0, 20.0, 38.0, 30.0, 100.0, 150.0,
                    List.of("LOAMY", "CLAY"), List.of("February", "March"), 700.0),
            new IndianCrop("Cotton", "☁", "పత్తి", "Kharif fiber crop that thrives in warm climates with moderate rainfall",
                    180, 700.0, 21.0, 30.0, 25.0, 50.0, 100.0,
                    List.of("CLAY", "LOAMY"), List.of("April", "May"), 15.0),
            new IndianCrop("Groundnut", "🥜", "వేరుశనగ", "Kharif oilseed legume, prefers well-drained sandy soils",
                    110, 500.0, 20.0, 30.0, 25.0, 50.0, 125.0,
                    List.of("SANDY", "LOAMY"), List.of("June", "July"), 20.0),
            new IndianCrop("Soybean", "🫘", "సోయాబీన్", "Kharif oilseed legume popular in central India",
                    100, 450.0, 20.0, 30.0, 25.0, 60.0, 100.0,
                    List.of("LOAMY", "CLAY"), List.of("June", "July"), 25.0),
            new IndianCrop("Pearl Millet", "🌾", "సజ్జ", "Drought-tolerant Kharif millet, also known as Bajra",
                    80, 350.0, 25.0, 35.0, 30.0, 40.0, 75.0,
                    List.of("SANDY", "LOAMY"), List.of("June", "July"), 15.0),
            new IndianCrop("Sorghum", "🌾", "జొన్న", "Hardy Kharif/Rabi millet, also known as Jowar",
                    110, 400.0, 25.0, 32.0, 28.0, 40.0, 100.0,
                    List.of("LOAMY", "SANDY"), List.of("June", "October"), 20.0),
            new IndianCrop("Chickpea", "🫘", "శనగలు", "Rabi pulse crop, also known as Bengal Gram or Chana",
                    100, 300.0, 10.0, 25.0, 20.0, 30.0, 65.0,
                    List.of("LOAMY", "CLAY"), List.of("October", "November"), 15.0),
            new IndianCrop("Cowpea", "🫘", "అలసందలు", "Fast-growing Kharif pulse, also known as Lobia",
                    75, 350.0, 20.0, 35.0, 28.0, 40.0, 100.0,
                    List.of("SANDY", "LOAMY"), List.of("June", "July"), 12.0),
            new IndianCrop("Pigeon Pea", "🫙", "కంది", "Long-duration Kharif pulse, also known as Tur or Arhar",
                    150, 400.0, 20.0, 30.0, 26.0, 60.0, 100.0,
                    List.of("LOAMY", "SANDY"), List.of("June", "July"), 12.0),
            new IndianCrop("Black Gram", "🫘", "మినుములు", "Rabi/Kharif pulse crop, also known as Urad, common across South India",
                    90, 300.0, 15.0, 30.0, 22.0, 30.0, 60.0,
                    List.of("LOAMY", "SANDY", "CLAY"), List.of("June", "October"), 10.0),
            new IndianCrop("Green Gram", "🌿", "పెసలు", "Short-duration pulse crop, also known as Moong",
                    70, 250.0, 15.0, 30.0, 22.0, 25.0, 50.0,
                    List.of("LOAMY", "SANDY"), List.of("June", "October"), 10.0),
            new IndianCrop("Horse Gram", "🫘", "ఉలవలు", "Hardy drought-tolerant pulse popular in Andhra Pradesh and Telangana",
                    120, 300.0, 15.0, 30.0, 22.0, 40.0, 75.0,
                    List.of("SANDY", "LOAMY", "LATERITE"), List.of("September", "October"), 8.0),
            new IndianCrop("Ragi", "🌾", "రాగులు", "Finger millet, a highly drought-tolerant staple grain of South India",
                    120, 400.0, 15.0, 30.0, 25.0, 50.0, 100.0,
                    List.of("LOAMY", "SANDY", "LATERITE"), List.of("June", "July"), 18.0),
            new IndianCrop("Coconut", "🥥", "కొబ్బరి", "Perennial palm crop grown widely along South India's coastal belt",
                    365, 2000.0, 20.0, 35.0, 28.0, 100.0, 300.0,
                    List.of("SANDY", "LATERITE"), List.of("June", "July"), 100.0),
            new IndianCrop("Sesame", "🌱", "నువ్వులు", "Kharif oilseed crop, also known as Til or Nuvvulu",
                    90, 300.0, 25.0, 35.0, 28.0, 40.0, 65.0,
                    List.of("SANDY", "LOAMY"), List.of("June", "July"), 8.0),
            new IndianCrop("Sunflower", "🌻", "పొద్దుతిరుగుడు", "Oilseed crop grown across Kharif, Rabi and summer seasons",
                    100, 400.0, 18.0, 30.0, 24.0, 50.0, 100.0,
                    List.of("LOAMY", "SANDY", "CLAY"), List.of("January", "June"), 15.0),
            new IndianCrop("Mustard", "🌼", "ఆవాలు", "Rabi oilseed crop widely grown in North India",
                    130, 300.0, 10.0, 25.0, 18.0, 25.0, 40.0,
                    List.of("LOAMY", "SANDY"), List.of("October", "November"), 12.0),
            new IndianCrop("Turmeric", "🟡", "పసుపు", "Kharif spice crop needing warm, humid conditions",
                    240, 1000.0, 20.0, 35.0, 28.0, 150.0, 230.0,
                    List.of("LOAMY", "CLAY"), List.of("June", "July"), 25.0),
            new IndianCrop("Chilli", "🌶", "మిర్చి", "Kharif spice crop grown across most Indian states",
                    150, 600.0, 20.0, 30.0, 25.0, 60.0, 125.0,
                    List.of("LOAMY", "SANDY"), List.of("June", "July"), 10.0),
            new IndianCrop("Onion", "🧅", "ఉల్లిపాయ", "Rabi vegetable crop, major cash crop in Maharashtra and Karnataka",
                    120, 350.0, 13.0, 28.0, 22.0, 65.0, 75.0,
                    List.of("LOAMY", "SANDY"), List.of("October", "November"), 200.0),
            new IndianCrop("Potato", "🥔", "బంగాళదుంప", "Tuber crop that thrives in cool climates with organic-rich soil",
                    100, 500.0, 10.0, 25.0, 18.0, 50.0, 150.0,
                    List.of("PEAT", "LOAMY", "SANDY"), List.of("October", "November", "December"), 200.0),
            new IndianCrop("Tomato", "🍅", "టమాటా", "Widely grown vegetable crop across Kharif and Rabi seasons",
                    90, 400.0, 18.0, 27.0, 22.0, 60.0, 130.0,
                    List.of("LOAMY", "SANDY"), List.of("June", "October"), 250.0),
            new IndianCrop("Banana", "🍌", "అరటి", "Perennial fruit crop needing consistent warmth and irrigation",
                    300, 1800.0, 20.0, 35.0, 27.0, 150.0, 220.0,
                    List.of("LOAMY", "CLAY"), List.of("June", "July"), 400.0),
            new IndianCrop("Mango", "🥭", "మామిడి", "Perennial orchard crop suited to lateritic and loamy soils",
                    365, 1000.0, 24.0, 35.0, 30.0, 75.0, 250.0,
                    List.of("LATERITE", "LOAMY"), List.of("June", "July"), 80.0),
            new IndianCrop("Tea", "🍵", "తేయాకు", "Perennial plantation crop grown in hilly, high-rainfall regions",
                    365, 1500.0, 13.0, 30.0, 24.0, 150.0, 300.0,
                    List.of("LATERITE", "PEAT"), List.of("March", "April"), 20.0),
            new IndianCrop("Jute", "🌿", "జనపనార", "Kharif fiber crop needing hot, humid conditions with heavy rainfall",
                    120, 1200.0, 24.0, 37.0, 30.0, 150.0, 250.0,
                    List.of("SILTY", "CLAY"), List.of("March", "April"), 25.0),
            new IndianCrop("Cashew", "🌰", "జీడిమామిడి", "Hardy tree crop well-suited to lateritic and coastal soils",
                    365, 1000.0, 20.0, 35.0, 27.0, 100.0, 300.0,
                    List.of("LATERITE", "SANDY"), List.of("June", "July"), 10.0)
    );

    public void initializeSampleCrops() {
        ensureIndianCropCatalog();
    }
}
