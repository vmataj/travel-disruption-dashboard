package com.vmataj.travel;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {PlannerController.class, DestinationController.class})
class PlannerErrors {
    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail problem(ResponseStatusException exception) {
        return exception.getBody();
    }
}
