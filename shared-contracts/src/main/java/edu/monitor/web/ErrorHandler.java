package edu.monitor.web;

import edu.monitor.contract.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ErrorHandler {
  @ExceptionHandler(ServiceException.class)
  ResponseEntity<ApiError> service(ServiceException e) {
    return ResponseEntity.status(e.status())
        .body(new ApiError(RequestContext.id(), e.code(), e.getMessage()));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    IllegalArgumentException.class
  })
  ResponseEntity<ApiError> invalid(Exception e) {
    return ResponseEntity.badRequest()
        .body(
            new ApiError(
                RequestContext.id(),
                "INVALID_INPUT",
                "Verifica los parámetros y el formato de la solicitud."));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unexpected(Exception e) {
    return ResponseEntity.internalServerError()
        .body(
            new ApiError(
                RequestContext.id(), "INTERNAL_ERROR", "No se pudo completar la solicitud."));
  }
}
