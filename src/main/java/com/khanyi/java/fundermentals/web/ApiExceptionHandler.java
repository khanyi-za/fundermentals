package com.khanyi.java.fundermentals.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

/**
 * Turns the engine's exceptions into HTTP responses, in ONE place.
 *
 * The engine knows nothing about HTTP; it just throws standard Java exceptions.
 * This class decides what each one means to a client:
 *
 *   NoSuchElementException (incl. TrackNotFoundException)  -> 404 Not Found
 *   IllegalArgumentException, IndexOutOfBoundsException     -> 400 Bad Request
 *   IllegalStateException ("Nothing to undo", ...)          -> 409 Conflict
 *
 * The body is a ProblemDetail (RFC 9457): {"status": 404, "detail": "No playlist with id 'x'", ...}
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail notFound(NoSuchElementException e) {
        return problem(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler({IllegalArgumentException.class, IndexOutOfBoundsException.class})
    public ProblemDetail badRequest(RuntimeException e) {
        return problem(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail conflict(IllegalStateException e) {
        return problem(HttpStatus.CONFLICT, e);
    }

    private static ProblemDetail problem(HttpStatus status, RuntimeException e) {
        log.debug("{} -> {} {}", e.getClass().getSimpleName(), status.value(), e.getMessage());
        return ProblemDetail.forStatusAndDetail(status, e.getMessage());
    }
}
