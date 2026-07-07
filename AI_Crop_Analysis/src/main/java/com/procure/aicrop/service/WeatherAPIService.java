package com.procure.aicrop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WeatherAPIService {

    @Value("${weather.api.enabled:false}")
    private boolean weatherApiEnabled;

    @Value("${weather.api.key:}")
    private String openWeatherApiKey;

    @Value("${weather.api.base-url:https://api.openweathermap.org/data/2.5}")
    private String baseUrl;

    private HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(30)).build();
        }
        return httpClient;
    }

    // ============================================
    // GET CURRENT WEATHER
    // ============================================
    public Map<String, Object> getCurrentWeather(Double latitude, Double longitude) {
        if (!weatherApiEnabled || openWeatherApiKey.isEmpty()) {
            log.warn("Weather API not enabled or API key missing");
            return getDefaultWeather();
        }

        try {
            String url = String.format(
                    "%s/weather?lat=%.4f&lon=%.4f&units=metric&appid=%s",
                    baseUrl, latitude, longitude, openWeatherApiKey
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseWeatherResponse(response.body());
            } else {
                log.error("Weather API error: {}", response.statusCode());
                return getDefaultWeather();
            }
        } catch (Exception e) {
            log.error("Failed to fetch current weather", e);
            return getDefaultWeather();
        }
    }

    // ============================================
    // GET WEATHER FORECAST
    // ============================================
    public Map<String, Object> getWeatherForecast(Double latitude, Double longitude, int days) {
        if (!weatherApiEnabled || openWeatherApiKey.isEmpty()) {
            log.warn("Weather API not enabled or API key missing");
            return getDefaultForecast();
        }

        try {
            String url = String.format(
                    "%s/forecast?lat=%.4f&lon=%.4f&units=metric&appid=%s",
                    baseUrl, latitude, longitude, openWeatherApiKey
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseForecastResponse(response.body(), days);
            } else {
                log.error("Forecast API error: {}", response.statusCode());
                return getDefaultForecast();
            }
        } catch (Exception e) {
            log.error("Failed to fetch forecast", e);
            return getDefaultForecast();
        }
    }

    // ============================================
    // GET FORECAST FOR A SPECIFIC DATE
    // ============================================
    /**
     * Looks up forecasted weather for one specific future date, rather than an aggregate
     * total across N days. OpenWeatherMap's free tier only forecasts ~5 days ahead, so this
     * returns available=false for dates beyond that - callers should fall back to seasonal
     * reasoning rather than pretending a real forecast exists for far-future dates.
     */
    public Map<String, Object> getForecastForDate(Double latitude, Double longitude, java.time.LocalDate targetDate) {
        Map<String, Object> result = new HashMap<>();

        if (!weatherApiEnabled || openWeatherApiKey.isEmpty()) {
            result.put("available", false);
            return result;
        }

        try {
            String url = String.format(
                    "%s/forecast?lat=%.4f&lon=%.4f&units=metric&appid=%s",
                    baseUrl, latitude, longitude, openWeatherApiKey
            );

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                result.put("available", false);
                return result;
            }

            JsonNode json = objectMapper.readTree(response.body());
            var list = json.path("list");
            String targetDateStr = targetDate.toString();

            double totalTemp = 0;
            double totalRain = 0;
            int count = 0;
            String condition = null;

            for (int i = 0; i < list.size(); i++) {
                var item = list.get(i);
                String dtTxt = item.path("dt_txt").asText("");
                if (dtTxt.startsWith(targetDateStr)) {
                    totalTemp += item.path("main").path("temp").asDouble();
                    totalRain += item.path("rain").path("3h").asDouble(0.0);
                    if (condition == null) {
                        condition = item.path("weather").path(0).path("main").asText("Unknown");
                    }
                    count++;
                }
            }

            if (count == 0) {
                result.put("available", false);
                return result;
            }

            result.put("available", true);
            result.put("temperature", totalTemp / count);
            result.put("rainfall", totalRain);
            result.put("condition", condition);
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch forecast for date {}", targetDate, e);
            result.put("available", false);
            return result;
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================
    private Map<String, Object> parseWeatherResponse(String response) {
        try {
            JsonNode json = objectMapper.readTree(response);
            Map<String, Object> weather = new HashMap<>();

            weather.put("temperature", json.path("main").path("temp").asDouble());
            weather.put("feels_like", json.path("main").path("feels_like").asDouble());
            weather.put("min_temperature", json.path("main").path("temp_min").asDouble());
            weather.put("max_temperature", json.path("main").path("temp_max").asDouble());
            weather.put("pressure", json.path("main").path("pressure").asInt());
            weather.put("humidity", json.path("main").path("humidity").asInt());
            weather.put("rainfall", json.path("rain").path("1h").asDouble(0.0));
            weather.put("wind_speed", json.path("wind").path("speed").asDouble());
            weather.put("condition", json.path("weather").path(0).path("main").asText());
            weather.put("description", json.path("weather").path(0).path("description").asText());
            weather.put("cloudiness", json.path("clouds").path("all").asInt());
            weather.put("sunrise", json.path("sys").path("sunrise").asLong());
            weather.put("sunset", json.path("sys").path("sunset").asLong());

            return weather;
        } catch (Exception e) {
            log.error("Failed to parse weather response", e);
            return getDefaultWeather();
        }
    }

    private Map<String, Object> parseForecastResponse(String response, int days) {
        try {
            JsonNode json = objectMapper.readTree(response);
            Map<String, Object> forecast = new HashMap<>();

            double totalRainfall = 0;
            double maxTemp = -100;
            double minTemp = 100;
            int count = 0;

            var list = json.path("list");
            for (int i = 0; i < Math.min(list.size(), days * 8); i++) { // 8 forecasts per day
                var item = list.get(i);
                double temp = item.path("main").path("temp").asDouble();
                double rain = item.path("rain").path("3h").asDouble(0.0);

                maxTemp = Math.max(maxTemp, temp);
                minTemp = Math.min(minTemp, temp);
                totalRainfall += rain;
                count++;
            }

            forecast.put("forecast_days", days);
            forecast.put("total_rainfall", totalRainfall);
            forecast.put("average_temperature", (maxTemp + minTemp) / 2);
            forecast.put("max_temperature", maxTemp);
            forecast.put("min_temperature", minTemp);
            forecast.put("data_points", count);
            forecast.put("location", json.path("city").path("name").asText("Unknown"));
            forecast.put("country", json.path("city").path("country").asText(""));

            return forecast;
        } catch (Exception e) {
            log.error("Failed to parse forecast response", e);
            return getDefaultForecast();
        }
    }

    // ============================================
    // DEFAULT RESPONSES
    // ============================================
    private Map<String, Object> getDefaultWeather() {
        Map<String, Object> weather = new HashMap<>();
        weather.put("temperature", 28.5);
        weather.put("feels_like", 30.0);
        weather.put("min_temperature", 24.0);
        weather.put("max_temperature", 32.0);
        weather.put("pressure", 1013);
        weather.put("humidity", 65);
        weather.put("rainfall", 0.0);
        weather.put("wind_speed", 5.5);
        weather.put("condition", "Clear");
        weather.put("description", "clear sky");
        weather.put("cloudiness", 10);
        weather.put("source", "default");
        return weather;
    }

    private Map<String, Object> getDefaultForecast() {
        Map<String, Object> forecast = new HashMap<>();
        forecast.put("forecast_days", 7);
        forecast.put("total_rainfall", 15.0);
        forecast.put("average_temperature", 28.0);
        forecast.put("max_temperature", 32.0);
        forecast.put("min_temperature", 24.0);
        forecast.put("data_points", 0);
        forecast.put("location", "Default Location");
        forecast.put("country", "");
        forecast.put("source", "default");
        return forecast;
    }

    public boolean isEnabled() {
        return weatherApiEnabled && !openWeatherApiKey.isEmpty();
    }
}
