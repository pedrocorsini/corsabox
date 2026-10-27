package com.corsini.corsabox.identity.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.corsini.corsabox.identity.domain.exception.EmailAlreadyInUseException;
import com.corsini.corsabox.identity.domain.exception.EmailNotFoundException;
import com.corsini.corsabox.identity.domain.exception.InvalidCredentialsException;
import com.corsini.corsabox.identity.domain.exception.InvalidRefreshTokenException;
import com.corsini.corsabox.identity.domain.exception.InvalidResetTokenException;
import com.corsini.corsabox.identity.domain.exception.InvalidVerificationCodeException;
import com.corsini.corsabox.identity.domain.exception.ResetLinkRecentlySentException;
import com.corsini.corsabox.identity.domain.exception.TooManyLoginAttemptsException;
import com.corsini.corsabox.identity.domain.exception.UserNotFoundException;
import com.corsini.corsabox.identity.domain.exception.UsernameAlreadyInUseException;
import com.corsini.corsabox.identity.domain.exception.VerificationCodeRecentlySentException;
import com.corsini.corsabox.identity.domain.exception.WrongCurrentPasswordException;

@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class UserExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({ EmailAlreadyInUseException.class, UsernameAlreadyInUseException.class })
    ProblemDetail handleConflict(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Email or username already in use");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalid(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Token de reset vencido, já usado ou substituído, e senha atual errada na
    // troca de senha.
    // 400 de propósito: 401 fica reservado para sessão expirada.
    @ExceptionHandler({ InvalidResetTokenException.class, WrongCurrentPasswordException.class,
            InvalidVerificationCodeException.class })
    ProblemDetail handleBadRequest(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler({ UserNotFoundException.class, EmailNotFoundException.class })
    ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({ VerificationCodeRecentlySentException.class, ResetLinkRecentlySentException.class,
            TooManyLoginAttemptsException.class })
    ProblemDetail handleTooMany(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ProblemDetail handleInvalidRefresh(InvalidRefreshTokenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
}