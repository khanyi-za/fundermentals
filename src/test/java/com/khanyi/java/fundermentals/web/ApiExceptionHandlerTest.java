package com.khanyi.java.fundermentals.web;

import com.khanyi.java.fundermentals.data.TrackNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void missingThingsAre404() {
        assertProblem(404, "No playlist with id 'x'", handler.notFound(new NoSuchElementException("No playlist with id 'x'")));
        assertProblem(404, "No track with id 'y'", handler.notFound(new TrackNotFoundException("y")));
    }

    @Test
    void badInputIs400() {
        assertProblem(400, "name must not be blank", handler.badRequest(new IllegalArgumentException("name must not be blank")));
        assertProblem(400, "Index 9 out of bounds for length 3", handler.badRequest(new IndexOutOfBoundsException("Index 9 out of bounds for length 3")));
    }

    @Test
    void actionsThatDontFitTheCurrentStateAre409() {
        assertProblem(409, "Nothing to undo", handler.conflict(new IllegalStateException("Nothing to undo")));
    }

    private static void assertProblem(int status, String detail, ProblemDetail problem) {
        assertEquals(status, problem.getStatus());
        assertEquals(detail, problem.getDetail());
    }
}
