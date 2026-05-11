package com.einswen.blogapi.service;

import com.einswen.blogapi.dto.ForecastResponse;
import com.einswen.blogapi.dto.OpticalState;
import com.einswen.blogapi.exception.ApiException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ForecastService {

    private static final String FORECAST_ENDPOINT = "https://api.open-meteo.com/v1/forecast";
    private static final String AIR_QUALITY_ENDPOINT = "https://air-quality-api.open-meteo.com/v1/air-quality";
    private static final List<String> HOURLY_FIELDS = List.of(
        "temperature_2m",
        "relative_humidity_2m",
        "cloud_cover",
        "cloud_cover_low",
        "cloud_cover_mid",
        "cloud_cover_high",
        "wind_speed_10m",
        "visibility"
    );
    private static final List<String> DAILY_FIELDS = List.of("sunrise", "sunset");
    private static final List<String> AIR_QUALITY_FIELDS = List.of("aerosol_optical_depth");
    private static final List<Integer> BOUNDARY_SAMPLE_DISTANCES_KM = List.of(50, 100, 150, 200, 250, 300, 350, 400, 450, 500);
    private static final double EARTH_RADIUS_KM = 6371;
    private static final double HIGH_CLOUD_ZERO_THRESHOLD = 10;
    private static final double CLOUD_THRESHOLD = 0.1;
    private static final double MAX_SEARCH_DISTANCE_KM = 500;
    private static final double OPTIMAL_DISTANCE_KM = 400;
    private static final double BOUNDARY_WEIGHT = 0.8;
    private static final double HIGH_CLOUD_WEIGHT = 0.1;
    private static final double MID_CLOUD_WEIGHT = 0.1;

    private final RestClient restClient;

    public ForecastService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public ForecastResponse fetchForecast(
        double latitude,
        double longitude,
        String timezone,
        String target,
        int forecastDays
    ) {
        String resolvedTimezone = timezone == null || timezone.isBlank() ? "auto" : timezone;
        int resolvedForecastDays = Math.max(1, forecastDays);

        OpenMeteoForecastResponse forecastData = fetchWeatherData(
            latitude,
            longitude,
            resolvedTimezone,
            resolvedForecastDays,
            HOURLY_FIELDS,
            "暂时无法获取天气数据。"
        );
        OpenMeteoAirQualityResponse airQualityData = fetchAirQualityData(
            latitude,
            longitude,
            resolvedTimezone,
            resolvedForecastDays
        );

        if (forecastData.hourly() == null
            || forecastData.hourly().time() == null
            || forecastData.hourly().time().isEmpty()
            || forecastData.daily() == null
            || forecastData.daily().sunrise() == null
            || forecastData.daily().sunrise().isEmpty()
            || forecastData.daily().sunset() == null
            || forecastData.daily().sunset().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "这个城市的预测数据不完整。");
        }

        TargetEvent targetEvent = getTargetEventTime(forecastData.daily(), target);
        int targetIndex = findNearestWeatherIndex(forecastData.hourly().time(), targetEvent == null ? null : targetEvent.eventTime());

        if (targetEvent == null || targetIndex == -1) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "没有找到接近目标时刻的小时级预测数据。");
        }

        String snapshotTime = forecastData.hourly().time().get(targetIndex);
        int aerosolIndex = airQualityData.hourly() == null || airQualityData.hourly().time() == null
            ? -1
            : airQualityData.hourly().time().indexOf(snapshotTime);
        int utcOffsetSeconds = forecastData.utcOffsetSeconds() == null ? 0 : forecastData.utcOffsetSeconds();
        double solarAzimuth = calculateSolarAzimuth(
            forecastData.latitude(),
            forecastData.longitude(),
            targetEvent.eventTime(),
            utcOffsetSeconds
        );
        double boundaryDistanceKm = fetchHighCloudBoundaryDistance(
            forecastData.latitude(),
            forecastData.longitude(),
            forecastData.timezone() == null || forecastData.timezone().isBlank() ? resolvedTimezone : forecastData.timezone(),
            resolvedForecastDays,
            snapshotTime,
            solarAzimuth
        );

        Double cloudCoverLow = getValueAtIndex(forecastData.hourly().cloudCoverLow(), targetIndex);
        Double cloudCoverMid = getValueAtIndex(forecastData.hourly().cloudCoverMid(), targetIndex);
        Double cloudCoverHigh = getValueAtIndex(forecastData.hourly().cloudCoverHigh(), targetIndex);
        Double aerosolOpticalDepth = aerosolIndex >= 0 && airQualityData.hourly() != null
            ? getValueAtIndex(airQualityData.hourly().aerosolOpticalDepth(), aerosolIndex)
            : null;

        OpticalState opticalState = evaluateSkyOpticalState(
            cloudCoverLow,
            cloudCoverMid,
            cloudCoverHigh,
            aerosolOpticalDepth,
            boundaryDistanceKm
        );

        long minutesToTarget = Math.abs(parseIsoMinutes(targetEvent.eventTime()) - parseIsoMinutes(snapshotTime));

        return new ForecastResponse(
            forecastData.latitude(),
            forecastData.longitude(),
            forecastData.timezone() == null || forecastData.timezone().isBlank() ? resolvedTimezone : forecastData.timezone(),
            utcOffsetSeconds,
            targetEvent.target(),
            targetEvent.dayIndex(),
            targetEvent.eventKey(),
            targetEvent.eventTime(),
            snapshotTime,
            targetEvent.eventTime(),
            getValueAtIndex(forecastData.hourly().temperature2m(), targetIndex),
            getValueAtIndex(forecastData.hourly().relativeHumidity2m(), targetIndex),
            getValueAtIndex(forecastData.hourly().cloudCover(), targetIndex),
            cloudCoverLow,
            cloudCoverMid,
            cloudCoverHigh,
            getValueAtIndex(forecastData.hourly().windSpeed10m(), targetIndex),
            getValueAtIndex(forecastData.hourly().visibility(), targetIndex),
            aerosolOpticalDepth,
            boundaryDistanceKm,
            solarAzimuth,
            minutesToTarget,
            minutesToTarget,
            opticalState
        );
    }

    private OpenMeteoForecastResponse fetchWeatherData(
        double latitude,
        double longitude,
        String timezone,
        int forecastDays,
        List<String> hourlyFields,
        String errorMessage
    ) {
        String uri = UriComponentsBuilder.fromUriString(FORECAST_ENDPOINT)
            .queryParam("latitude", latitude)
            .queryParam("longitude", longitude)
            .queryParam("current", "temperature_2m")
            .queryParam("hourly", String.join(",", hourlyFields))
            .queryParam("daily", String.join(",", DAILY_FIELDS))
            .queryParam("forecast_days", forecastDays)
            .queryParam("timezone", timezone)
            .build(true)
            .toUriString();

        try {
            OpenMeteoForecastResponse response = restClient.get().uri(uri).retrieve().body(OpenMeteoForecastResponse.class);

            if (response == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, errorMessage);
            }

            return response;
        } catch (RestClientException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, errorMessage);
        }
    }

    private OpenMeteoAirQualityResponse fetchAirQualityData(
        double latitude,
        double longitude,
        String timezone,
        int forecastDays
    ) {
        String uri = UriComponentsBuilder.fromUriString(AIR_QUALITY_ENDPOINT)
            .queryParam("latitude", latitude)
            .queryParam("longitude", longitude)
            .queryParam("hourly", String.join(",", AIR_QUALITY_FIELDS))
            .queryParam("forecast_days", forecastDays)
            .queryParam("timezone", timezone)
            .build(true)
            .toUriString();

        try {
            OpenMeteoAirQualityResponse response = restClient.get().uri(uri).retrieve().body(OpenMeteoAirQualityResponse.class);

            if (response == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "暂时无法获取空气质量数据。");
            }

            return response;
        } catch (RestClientException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "暂时无法获取空气质量数据。");
        }
    }

    private double fetchHighCloudBoundaryDistance(
        double latitude,
        double longitude,
        String timezone,
        int forecastDays,
        String snapshotTime,
        double solarAzimuth
    ) {
        for (Integer distanceKm : BOUNDARY_SAMPLE_DISTANCES_KM) {
            DestinationPoint point = destinationPoint(latitude, longitude, solarAzimuth, distanceKm);
            OpenMeteoForecastResponse forecast = fetchWeatherData(
                point.latitude(),
                point.longitude(),
                timezone,
                forecastDays,
                List.of("cloud_cover_high"),
                "暂时无法采样高云边界数据。"
            );

            if (forecast.hourly() == null || forecast.hourly().time() == null) {
                continue;
            }

            int timeIndex = forecast.hourly().time().indexOf(snapshotTime);

            if (timeIndex == -1) {
                continue;
            }

            Double highCloud = getValueAtIndex(forecast.hourly().cloudCoverHigh(), timeIndex);

            if (highCloud != null && highCloud < HIGH_CLOUD_ZERO_THRESHOLD) {
                return distanceKm;
            }
        }

        return BOUNDARY_SAMPLE_DISTANCES_KM.getLast();
    }

    private OpticalState evaluateSkyOpticalState(
        Double cloudCoverLow,
        Double cloudCoverMid,
        Double cloudCoverHigh,
        Double aerosolOpticalDepth,
        double boundaryDistanceKm
    ) {
        double highCloudFraction = toFraction(cloudCoverHigh);
        double midCloudFraction = toFraction(cloudCoverMid);
        double lowCloudFraction = toFraction(cloudCoverLow);
        double resolvedAerosolOpticalDepth = aerosolOpticalDepth == null ? 0.3 : aerosolOpticalDepth;

        Map<String, Double> factorScores = Map.of(
            "boundary", scoreBoundaryDistance(boundaryDistanceKm),
            "highCloud", scoreHighCloud(highCloudFraction),
            "midCloud", scoreMidCloud(midCloudFraction),
            "lowCloud", scoreLowCloud(lowCloudFraction),
            "aerosol", scoreAerosolOpticalDepth(resolvedAerosolOpticalDepth)
        );

        if (highCloudFraction < CLOUD_THRESHOLD) {
            return new OpticalState(
                0,
                false,
                "不适合",
                "观测点上方高云太少，缺少形成晚霞层次感的主要载体。",
                factorScores,
                Map.of(
                    "highCloudFraction", round(highCloudFraction, 2),
                    "midCloudFraction", round(midCloudFraction, 2),
                    "lowCloudFraction", round(lowCloudFraction, 2),
                    "aerosolOpticalDepth", round(resolvedAerosolOpticalDepth, 2),
                    "boundaryDistanceKm", round(boundaryDistanceKm, 2)
                )
            );
        }

        double qualityScore =
            factorScores.get("boundary") * BOUNDARY_WEIGHT
                + factorScores.get("highCloud") * HIGH_CLOUD_WEIGHT
                + factorScores.get("midCloud") * MID_CLOUD_WEIGHT;
        double penaltyScore = factorScores.get("lowCloud") * factorScores.get("aerosol");
        int finalScore = (int) Math.round(qualityScore * penaltyScore * 100);
        ScoreClassification classification = classifySunsetScore(finalScore);

        return new OpticalState(
            finalScore,
            finalScore >= 45,
            classification.label(),
            classification.description(),
            factorScores,
            Map.of(
                "highCloudFraction", round(highCloudFraction, 2),
                "midCloudFraction", round(midCloudFraction, 2),
                "lowCloudFraction", round(lowCloudFraction, 2),
                "aerosolOpticalDepth", round(resolvedAerosolOpticalDepth, 2),
                "boundaryDistanceKm", round(boundaryDistanceKm, 2),
                "qualityScore", round(qualityScore, 2),
                "penaltyScore", round(penaltyScore, 2)
            )
        );
    }

    private ScoreClassification classifySunsetScore(int score) {
        if (score >= 70) {
            return new ScoreClassification("非常适合", "高云结构、云边界距离和透明度都比较理想，天空具备很强的晚霞潜力。");
        }

        if (score >= 45) {
            return new ScoreClassification("比较适合", "天空光学条件整体不错，傍晚有较大机会出现层次和颜色都在线的晚霞。");
        }

        if (score >= 20) {
            return new ScoreClassification("一般", "部分关键因子还可以，但低云、透明度或边界结构可能限制最终表现。");
        }

        return new ScoreClassification("不适合", "当前天空光学状态不够理想，出现高质量晚霞的概率偏低。");
    }

    private TargetEvent getTargetEventTime(DailyData dailyData, String target) {
        String resolvedTarget = target == null || target.isBlank() ? "today-sunset" : target;
        String[] parts = resolvedTarget.split("-");
        String dayValue = parts.length > 0 ? parts[0] : "today";
        String eventValue = parts.length > 1 ? parts[1] : "sunset";
        int dayIndex = "tomorrow".equals(dayValue) ? 1 : 0;
        String eventKey = "sunrise".equals(eventValue) ? "sunrise" : "sunset";
        List<String> values = "sunrise".equals(eventKey) ? dailyData.sunrise() : dailyData.sunset();

        if (values == null || values.size() <= dayIndex) {
            return null;
        }

        return new TargetEvent(dayIndex, eventKey, values.get(dayIndex), resolvedTarget);
    }

    private int findNearestWeatherIndex(List<String> hourlyTimes, String targetTime) {
        if (hourlyTimes == null || hourlyTimes.isEmpty() || targetTime == null) {
            return -1;
        }

        long targetMinutes = parseIsoMinutes(targetTime);
        int closestIndex = 0;

        for (int index = 1; index < hourlyTimes.size(); index += 1) {
            long closestDifference = Math.abs(parseIsoMinutes(hourlyTimes.get(closestIndex)) - targetMinutes);
            long currentDifference = Math.abs(parseIsoMinutes(hourlyTimes.get(index)) - targetMinutes);

            if (currentDifference < closestDifference) {
                closestIndex = index;
            }
        }

        return closestIndex;
    }

    private long parseIsoMinutes(String isoString) {
        return LocalDateTime.parse(isoString).toEpochSecond(ZoneOffset.UTC) / 60;
    }

    private double calculateSolarAzimuth(double latitude, double longitude, String localIsoTime, int utcOffsetSeconds) {
        long utcEpochSeconds = LocalDateTime.parse(localIsoTime).toEpochSecond(ZoneOffset.UTC) - utcOffsetSeconds;
        double julianDay = utcEpochSeconds / 86400.0 + 2440587.5;
        double julianCentury = (julianDay - 2451545) / 36525;

        double geomMeanLongSun = normalizeDegrees(280.46646 + julianCentury * (36000.76983 + julianCentury * 0.0003032));
        double geomMeanAnomalySun = normalizeDegrees(357.52911 + julianCentury * (35999.05029 - 0.0001537 * julianCentury));
        double eccentricityEarthOrbit = 0.016708634 - julianCentury * (0.000042037 + 0.0000001267 * julianCentury);
        double sunEqOfCenter =
            Math.sin(degreesToRadians(geomMeanAnomalySun)) * (1.914602 - julianCentury * (0.004817 + 0.000014 * julianCentury))
                + Math.sin(degreesToRadians(2 * geomMeanAnomalySun)) * (0.019993 - 0.000101 * julianCentury)
                + Math.sin(degreesToRadians(3 * geomMeanAnomalySun)) * 0.000289;
        double sunTrueLong = geomMeanLongSun + sunEqOfCenter;
        double omega = 125.04 - 1934.136 * julianCentury;
        double sunAppLong = sunTrueLong - 0.00569 - 0.00478 * Math.sin(degreesToRadians(omega));
        double meanObliqEcliptic =
            23
                + (26
                    + (21.448 - julianCentury * (46.815 + julianCentury * (0.00059 - julianCentury * 0.001813))) / 60)
                / 60;
        double obliqCorr = meanObliqEcliptic + 0.00256 * Math.cos(degreesToRadians(omega));
        double sunDeclination = radiansToDegrees(
            Math.asin(Math.sin(degreesToRadians(obliqCorr)) * Math.sin(degreesToRadians(sunAppLong)))
        );
        double y = Math.pow(Math.tan(degreesToRadians(obliqCorr / 2)), 2);
        double equationOfTime = 4 * radiansToDegrees(
            y * Math.sin(2 * degreesToRadians(geomMeanLongSun))
                - 2 * eccentricityEarthOrbit * Math.sin(degreesToRadians(geomMeanAnomalySun))
                + 4
                * eccentricityEarthOrbit
                * y
                * Math.sin(degreesToRadians(geomMeanAnomalySun))
                * Math.cos(2 * degreesToRadians(geomMeanLongSun))
                - 0.5 * y * y * Math.sin(4 * degreesToRadians(geomMeanLongSun))
                - 1.25
                * eccentricityEarthOrbit
                * eccentricityEarthOrbit
                * Math.sin(2 * degreesToRadians(geomMeanAnomalySun))
        );
        long secondsOfDay = Math.floorMod(utcEpochSeconds, 86400);
        double utcMinutes = secondsOfDay / 60.0;
        double trueSolarTime = ((utcMinutes + equationOfTime + 4 * longitude) % 1440 + 1440) % 1440;
        double hourAngle = trueSolarTime / 4 < 0 ? trueSolarTime / 4 + 180 : trueSolarTime / 4 - 180;
        double hourAngleRad = degreesToRadians(hourAngle);
        double latitudeRad = degreesToRadians(latitude);
        double declinationRad = degreesToRadians(sunDeclination);
        double azimuth = radiansToDegrees(
            Math.atan2(
                Math.sin(hourAngleRad),
                Math.cos(hourAngleRad) * Math.sin(latitudeRad) - Math.tan(declinationRad) * Math.cos(latitudeRad)
            )
        );

        return normalizeDegrees(azimuth + 180);
    }

    private DestinationPoint destinationPoint(double latitude, double longitude, double bearingDegrees, double distanceKm) {
        double bearingRad = degreesToRadians(bearingDegrees);
        double latRad = degreesToRadians(latitude);
        double lonRad = degreesToRadians(longitude);
        double angularDistance = distanceKm / EARTH_RADIUS_KM;
        double destinationLat = Math.asin(
            Math.sin(latRad) * Math.cos(angularDistance)
                + Math.cos(latRad) * Math.sin(angularDistance) * Math.cos(bearingRad)
        );
        double destinationLon = lonRad + Math.atan2(
            Math.sin(bearingRad) * Math.sin(angularDistance) * Math.cos(latRad),
            Math.cos(angularDistance) - Math.sin(latRad) * Math.sin(destinationLat)
        );

        return new DestinationPoint(radiansToDegrees(destinationLat), radiansToDegrees(destinationLon));
    }

    private Double getValueAtIndex(List<Double> series, int index) {
        if (series == null || index < 0 || index >= series.size()) {
            return null;
        }

        return series.get(index);
    }

    private double toFraction(Double percentageValue) {
        return clamp((percentageValue == null ? 0 : percentageValue) / 100, 0, 1);
    }

    private double scoreBoundaryDistance(double distanceKm) {
        if (distanceKm >= MAX_SEARCH_DISTANCE_KM) {
            return 0;
        }

        if (distanceKm <= OPTIMAL_DISTANCE_KM) {
            return distanceKm / OPTIMAL_DISTANCE_KM;
        }

        return clamp(
            1 - (distanceKm - OPTIMAL_DISTANCE_KM) / (MAX_SEARCH_DISTANCE_KM - OPTIMAL_DISTANCE_KM),
            0,
            1
        );
    }

    private double scoreHighCloud(double highCloudFraction) {
        if (highCloudFraction >= 0.4 && highCloudFraction <= 0.8) {
            return 1;
        }

        if (highCloudFraction > 0.8) {
            return 0.7;
        }

        if (highCloudFraction >= 0.1) {
            return 0.6;
        }

        return 0.1;
    }

    private double scoreMidCloud(double midCloudFraction) {
        if (midCloudFraction >= 0.2 && midCloudFraction <= 0.5) {
            return 1;
        }

        if (midCloudFraction > 0.5 && midCloudFraction <= 0.8) {
            return 0.7;
        }

        if (midCloudFraction > 0.8) {
            return 0.3;
        }

        return 0.2;
    }

    private double scoreLowCloud(double lowCloudFraction) {
        if (lowCloudFraction <= 0.1) {
            return 1;
        }

        if (lowCloudFraction <= 0.3) {
            return 0.6;
        }

        if (lowCloudFraction <= 0.5) {
            return 0.1;
        }

        return 0;
    }

    private double scoreAerosolOpticalDepth(double aerosolOpticalDepth) {
        if (aerosolOpticalDepth < 0.3) {
            return 1;
        }

        if (aerosolOpticalDepth < 0.6) {
            return 0.5;
        }

        return 0;
    }

    private double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }

    private double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round(value * factor) / factor;
    }

    private double normalizeDegrees(double value) {
        return ((value % 360) + 360) % 360;
    }

    private double degreesToRadians(double value) {
        return value * Math.PI / 180;
    }

    private double radiansToDegrees(double value) {
        return value * 180 / Math.PI;
    }

    private record DestinationPoint(double latitude, double longitude) {
    }

    private record TargetEvent(int dayIndex, String eventKey, String eventTime, String target) {
    }

    private record ScoreClassification(String label, String description) {
    }

    private record OpenMeteoForecastResponse(
        double latitude,
        double longitude,
        String timezone,
        @JsonProperty("utc_offset_seconds") Integer utcOffsetSeconds,
        HourlyData hourly,
        DailyData daily
    ) {
    }

    private record OpenMeteoAirQualityResponse(HourlyAirQualityData hourly) {
    }

    private record HourlyData(
        List<String> time,
        @JsonProperty("temperature_2m") List<Double> temperature2m,
        @JsonProperty("relative_humidity_2m") List<Double> relativeHumidity2m,
        @JsonProperty("cloud_cover") List<Double> cloudCover,
        @JsonProperty("cloud_cover_low") List<Double> cloudCoverLow,
        @JsonProperty("cloud_cover_mid") List<Double> cloudCoverMid,
        @JsonProperty("cloud_cover_high") List<Double> cloudCoverHigh,
        @JsonProperty("wind_speed_10m") List<Double> windSpeed10m,
        List<Double> visibility
    ) {
    }

    private record HourlyAirQualityData(
        List<String> time,
        @JsonProperty("aerosol_optical_depth") List<Double> aerosolOpticalDepth
    ) {
    }

    private record DailyData(List<String> sunrise, List<String> sunset) {
    }
}
