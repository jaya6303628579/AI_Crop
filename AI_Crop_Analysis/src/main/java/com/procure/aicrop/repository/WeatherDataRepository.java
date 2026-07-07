package com.procure.aicrop.repository;

import com.procure.aicrop.entity.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {
    List<WeatherData> findByDistrictAndStateAndWeatherDateBetween(
            String district, String state, LocalDate startDate, LocalDate endDate);

    List<WeatherData> findByDistrictAndStateOrderByWeatherDateDesc(String district, String state);
}
