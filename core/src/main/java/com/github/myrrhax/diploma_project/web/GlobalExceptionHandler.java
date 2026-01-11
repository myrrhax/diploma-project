package com.github.myrrhax.diploma_project.web;

import com.github.myrrhax.diploma_project.model.ApplicationException;
import com.github.myrrhax.diploma_project.web.dto.ErrorResponseDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponseDTO> handle(ApplicationException ex) {
        log.error("An application error occurred while processing the request: {}", ex.getMessage(), ex);

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
}
