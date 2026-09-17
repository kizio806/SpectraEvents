package dev.spectraevents.application.service;

public record EntityReconciliationReport(
    long instancesRecovered,
    long entitiesReconnected,
    long orphansRemoved,
    long instancesFailed,
    long warnings) {}
