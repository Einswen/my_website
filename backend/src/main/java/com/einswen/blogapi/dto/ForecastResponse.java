package com.einswen.blogapi.dto;

public record ForecastResponse(
    double latitude,
    double longitude,
    String timezone,
    int utcOffsetSeconds,
    String target,
    int targetDayIndex,
    String eventKey,
    String eventTime,
    String snapshotTime,
    String sunsetTime,
    Double temperature,
    Double humidity,
    Double cloudCover,
    Double cloudCoverLow,
    Double cloudCoverMid,
    Double cloudCoverHigh,
    Double windSpeed,
    Double visibility,
    Double aerosolOpticalDepth,
    double boundaryDistanceKm,
    double solarAzimuth,
    long minutesToTarget,
    long minutesToSunset,
    OpticalState opticalState
) {
}
