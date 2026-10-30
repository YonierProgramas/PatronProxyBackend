package edu.monitor.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.monitor.contract.*;
import edu.monitor.web.RequestContext;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

public class HttpServiceClient implements MicroserviceProxy<Object> {
  private final String base;
  private final Duration timeout;
  private final HttpClient client;
  private final ObjectMapper mapper;
  private final Set<String> operations;
  private final String internalToken;

  public HttpServiceClient(
      String base, int timeoutMs, ObjectMapper mapper, Set<String> operations) {
    this(base, timeoutMs, mapper, operations, "");
  }

  public HttpServiceClient(
      String base,
      int timeoutMs,
      ObjectMapper mapper,
      Set<String> operations,
      String internalToken) {
    this.internalToken = internalToken;
    URI uri = URI.create(base);
    if (!List.of("http", "https").contains(uri.getScheme())
        || uri.getHost() == null
        || uri.getUserInfo() != null) throw new IllegalArgumentException("Invalid service URL");
    this.base = base.replaceAll("/+$", "");
    this.timeout = Duration.ofMillis(timeoutMs);
    this.client =
        HttpClient.newBuilder()
            .connectTimeout(timeout)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    this.mapper = mapper;
    this.operations = operations;
  }

  public Object execute(String operation, Object... params) {
    if (!operations.contains(operation)) throw Inputs.unknown();
    try {
      var builder =
          HttpRequest.newBuilder(URI.create(base + "/internal/" + operation))
              .timeout(timeout)
              .header("Content-Type", "application/json")
              .header(
                  "X-Request-Id",
                  Optional.ofNullable(RequestContext.id())
                      .orElseGet(() -> UUID.randomUUID().toString()))
              .POST(
                  HttpRequest.BodyPublishers.ofString(
                      mapper.writeValueAsString(Map.of("params", params))));
      if (!internalToken.isEmpty())
        builder.header(edu.monitor.web.InternalServiceFilter.HEADER, internalToken);
      var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      var body = mapper.readTree(response.body());
      if (response.statusCode() >= 400)
        throw new ServiceException(
            response.statusCode(),
            body.path("errorCode").asText("UPSTREAM_ERROR"),
            body.path("errorMessage").asText("El servicio devolvió un error."));
      return mapper.convertValue(body.get("response"), Object.class);
    } catch (HttpTimeoutException e) {
      throw new ServiceException(
          504, "UPSTREAM_TIMEOUT", "El servicio superó el tiempo de espera.", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ServiceException(503, "SERVICE_UNAVAILABLE", "La solicitud fue interrumpida.", e);
    } catch (IOException e) {
      throw new ServiceException(
          503, "SERVICE_UNAVAILABLE", "No hay conexión con el servicio interno.", e);
    }
  }
}
