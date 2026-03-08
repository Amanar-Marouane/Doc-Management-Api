package com.example.backend.exception;

import com.example.backend.util.AppLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<Map<String, Object>> handleResourceNotFound(
                        ResourceNotFoundException ex, HttpServletRequest request) {
                AppLogger.warn(String.format("[404] %s - %s", request.getRequestURI(), ex.getMessage()));
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(errorBody(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), request));
        }

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<Map<String, Object>> handleBusinessException(
                        BusinessException ex, HttpServletRequest request) {
                AppLogger.warn(String.format("[400] %s - %s: %s", request.getRequestURI(), ex.getCode(),
                                ex.getMessage()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(errorBody(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), request));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidation(
                        MethodArgumentNotValidException ex, HttpServletRequest request) {
                Map<String, String> fieldErrors = ex.getBindingResult().getAllErrors().stream()
                                .filter(e -> e instanceof FieldError)
                                .map(e -> (FieldError) e)
                                .collect(Collectors.toMap(FieldError::getField, fe -> {
                                        String msg = fe.getDefaultMessage();
                                        return msg != null ? msg : "Valeur invalide";
                                }));

                Map<String, Object> body = new HashMap<>();
                body.put("timestamp", LocalDateTime.now().toString());
                body.put("status", HttpStatus.BAD_REQUEST.value());
                body.put("code", "VALIDATION_ERROR");
                body.put("message", "La requête contient des données invalides");
                body.put("errors", fieldErrors);
                body.put("path", request.getRequestURI());

                AppLogger.warn(String.format("[400] Validation failed at %s: %s", request.getRequestURI(),
                                fieldErrors));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<Map<String, Object>> handleAccessDenied(
                        AccessDeniedException ex, HttpServletRequest request) {
                AppLogger.warn(String.format("[403] Access denied at %s", request.getRequestURI()));
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(errorBody(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                                                "Vous n'avez pas les droits nécessaires pour effectuer cette action",
                                                request));
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<Map<String, Object>> handleMaxUploadSize(
                        MaxUploadSizeExceededException ex, HttpServletRequest request) {
                AppLogger.warn(String.format("[400] File too large at %s", request.getRequestURI()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(errorBody(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE",
                                                "Le fichier dépasse la taille maximale autorisée", request));
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<Map<String, Object>> handleTypeMismatch(
                        MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
                String message = String.format("Paramètre '%s' invalide: '%s'", ex.getName(), ex.getValue());
                AppLogger.warn(String.format("[400] Type mismatch at %s: %s", request.getRequestURI(), message));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(errorBody(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", message, request));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleUnexpected(
                        Exception ex, HttpServletRequest request) {
                AppLogger.error(String.format("[500] Unexpected error at %s: %s - %s",
                                request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage()));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                                                "Une erreur interne s'est produite", request));
        }

        private Map<String, Object> errorBody(HttpStatus status, String code, String message,
                        HttpServletRequest request) {
                Map<String, Object> body = new HashMap<>();
                body.put("timestamp", LocalDateTime.now().toString());
                body.put("status", status.value());
                body.put("code", code);
                body.put("message", message);
                body.put("path", request.getRequestURI());
                return body;
        }
}
