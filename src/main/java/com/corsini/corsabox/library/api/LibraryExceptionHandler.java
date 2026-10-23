package com.corsini.corsabox.library.api;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.corsini.corsabox.library.domain.exception.AlreadyWatchedException;
import com.corsini.corsabox.library.domain.exception.EntryNotFoundException;

// basePackages limita este handler aos controllers do library, e a ordem alta
// garante que ele vença o UserExceptionHandler para DataIntegrityViolationException.
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.corsini.corsabox.library")
public class LibraryExceptionHandler {

    @ExceptionHandler(EntryNotFoundException.class)
    ProblemDetail handleNotFound(EntryNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AlreadyWatchedException.class)
    ProblemDetail handleConflict(AlreadyWatchedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Dois PUTs simultâneos do mesmo título batem na unique constraint (usuário, tmdbId, format).
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "This title was just updated in your library. Try again.");
    }
}