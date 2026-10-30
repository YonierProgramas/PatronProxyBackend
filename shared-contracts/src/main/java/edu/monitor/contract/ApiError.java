package edu.monitor.contract;

public record ApiError(String requestId, String errorCode, String errorMessage) {}
