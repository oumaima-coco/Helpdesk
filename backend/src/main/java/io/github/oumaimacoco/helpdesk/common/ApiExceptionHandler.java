package io.github.oumaimacoco.helpdesk.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException exception) {
        return problem(HttpStatus.CONFLICT, "Business rule violated", exception.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail handleInvalidRequest(InvalidRequestException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", exception.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}