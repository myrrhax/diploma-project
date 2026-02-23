package com.github.myrrhax.diploma_project.web;

import com.github.myrrhax.diploma_project.model.exception.ApplicationException;
import com.github.myrrhax.diploma_project.model.dto.ErrorResponseDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final MessageSource messageSource;

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponseDTO> handle(ApplicationException ex) {
        log.error("An application error occurred while processing the request: {}", ex.getMessage());

        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponseDTO(ex.getMessage(), null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handle(ConstraintViolationException ex) {
        log.error("Validation exception occurred while processing the request: {}", ex.getMessage(), ex);
        var violations = ex.getConstraintViolations().stream()
                .collect(Collectors.groupingBy(
                        violation -> {
                            String path = violation.getPropertyPath().toString();
                            return path.substring(path.lastIndexOf('.') + 1);
                        },
                        Collectors.mapping(
                                ConstraintViolation::getMessage,
                                Collectors.toList()
                        )
                ));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO("Validation Failed", violations));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handle(Exception ex) {
        log.error("A fatal exception", ex);

        return ResponseEntity.internalServerError().build();
    }

    @MessageExceptionHandler(ApplicationException.class)
    @SendToUser("/queue/errors")
    public ErrorResponseDTO handleApplicationException(ApplicationException ex) {
        log.error("An application error occurred while processing websocket command: {}", ex.getMessage());
        String message = messageSource.getMessage(ex.getMessage(), ex.getArgs(), Locale.getDefault());

        return new ErrorResponseDTO(message, Collections.emptyMap());
    }
}
