package com.procure.aicrop.service;

import com.procure.aicrop.entity.User;
import com.procure.aicrop.entity.WeatherData;
import com.procure.aicrop.repository.WeatherDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WeatherDataService {

    private final WeatherDataRepository weatherDataRepository;
    private final WeatherAPIService weatherAPIService;

    private static final Double DEFAULT_LATITUDE = 17.3850;
    private static final Double DEFAULT_LONGITUDE = 78.4867;

    /**
     * Fetches live weather for the user's location and stores it as today's record,
     * unless a record for today already exists (idempotent, safe to call repeatedly).
     */
    public WeatherData recordTodayWeather(User user) {
        String district = user.getDistrict() != null ? user.getDistrict() : "Unknown";
        String state = user.getState() != null ? user.getState() : "Unknown";
        LocalDate today = LocalDate.now();

        List<WeatherData> existing = weatherDataRepository
                .findByDistrictAndStateAndWeatherDateBetween(district, state, today, today);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        Double latitude = user.getLatitude() != null ? user.getLatitude() : DEFAULT_LATITUDE;
        Double longitude = user.getLongitude() != null ? user.getLongitude() : DEFAULT_LONGITUDE;

        Map<String, Object> current = weatherAPIService.getCurrentWeather(latitude, longitude);

        WeatherData weatherData = WeatherData.builder()
                .district(district)
                .state(state)
                .latitude(latitude)
                .longitude(longitude)
                .weatherDate(today)
                .minTemp(toDouble(current.get("min_temperature"), 22.0))
                .maxTemp(toDouble(current.get("max_temperature"), 32.0))
                .rainfall(toDouble(current.get("rainfall"), 0.0))
                .humidity(toDouble(current.get("humidity"), 60.0))
                .windSpeed(toDouble(current.get("wind_speed"), 5.0))
                .weatherCondition(current.get("condition") != null ? current.get("condition").toString() : "Unknown")
                .isForecast(false)
                .confidence(weatherAPIService.isEnabled() ? 95.0 : 50.0)
                .build();

        WeatherData saved = weatherDataRepository.save(weatherData);
        log.info("Recorded weather for {}, {} on {}: {}", district, state, today, saved.getWeatherCondition());
        return saved;
    }

    private Double toDouble(Object value, Double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }
}
