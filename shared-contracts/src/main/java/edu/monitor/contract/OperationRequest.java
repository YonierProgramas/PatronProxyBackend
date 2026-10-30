package edu.monitor.contract;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record OperationRequest(@NotNull @Size(max = 1) List<Map<String, Object>> params) {}
