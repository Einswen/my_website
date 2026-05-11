package com.einswen.blogapi.controller;

import com.einswen.blogapi.dto.ForecastResponse;
import com.einswen.blogapi.service.ForecastService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping
    public ForecastResponse getForecast(
        @RequestParam double latitude,
        @RequestParam double longitude,
        @RequestParam(defaultValue = "auto") String timezone,
        @RequestParam(defaultValue = "today-sunset") String target,
        @RequestParam(defaultValue = "2") int forecastDays
    ) {
        return forecastService.fetchForecast(latitude, longitude, timezone, target, forecastDays);
    }
}
