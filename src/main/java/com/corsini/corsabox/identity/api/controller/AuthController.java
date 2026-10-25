    package com.corsini.corsabox.identity.api.controller;

    import org.springframework.http.HttpStatus;
    import org.springframework.web.bind.annotation.PostMapping;
    import org.springframework.web.bind.annotation.RequestBody;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.ResponseStatus;
    import org.springframework.web.bind.annotation.RestController;

import com.corsini.corsabox.identity.api.request.ConfirmRegistrationRequest;
import com.corsini.corsabox.identity.api.request.ForgotPasswordRequest;
    import com.corsini.corsabox.identity.api.request.LoginRequest;
import com.corsini.corsabox.identity.api.request.RegisterUserRequest;
import com.corsini.corsabox.identity.api.request.ResetPasswordRequest;
    import com.corsini.corsabox.identity.api.response.TokenResponse;
import com.corsini.corsabox.identity.api.response.UserResponse;
import com.corsini.corsabox.identity.application.dto.RegisterUserCommand;
import com.corsini.corsabox.identity.application.service.ConfirmRegistrationUseCase;
import com.corsini.corsabox.identity.application.service.LoginUseCase;
    import com.corsini.corsabox.identity.application.service.RequestPasswordResetUseCase;
    import com.corsini.corsabox.identity.application.service.ResetPasswordUseCase;
import com.corsini.corsabox.identity.application.service.StartRegistrationUseCase;

import jakarta.validation.Valid;

    @RestController
    @RequestMapping("/api/auth")
    public class AuthController {
        private final LoginUseCase loginUseCase;
        private final RequestPasswordResetUseCase requestPasswordReset;
        private final ResetPasswordUseCase resetPassword;
        private final StartRegistrationUseCase startRegistration;
        private final ConfirmRegistrationUseCase confirmRegistration;

        public AuthController(LoginUseCase loginUseCase,
                            RequestPasswordResetUseCase requestPasswordReset,
                            ResetPasswordUseCase resetPassword, ConfirmRegistrationUseCase confirmRegistration, StartRegistrationUseCase startRegistration) {
            this.loginUseCase = loginUseCase;
            this.requestPasswordReset = requestPasswordReset;
            this.resetPassword = resetPassword;
            this.startRegistration = startRegistration;
            this.confirmRegistration = confirmRegistration;
        }

        @PostMapping("/login")
        public TokenResponse login(@Valid @RequestBody LoginRequest request) {
            return new TokenResponse(loginUseCase.execute(request.email(), request.password()));
        }

        // 202 quando o link é enviado, 404 quando não existe conta com o email.
        @PostMapping("/forgot-password")
        @ResponseStatus(HttpStatus.ACCEPTED)
        public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
            requestPasswordReset.execute(request.email());
        }

        @PostMapping("/reset-password")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
            resetPassword.execute(request.token(), request.newPassword());
        }
        
        @PostMapping("/register")
        @ResponseStatus(HttpStatus.ACCEPTED)
        public void startRegistration(@Valid @RequestBody RegisterUserRequest request) {
            startRegistration.execute(new RegisterUserCommand(
                request.firstName(), request.lastName(), request.username(), request.email(), request.password()));
        }

        @PostMapping("/register/confirm")
        @ResponseStatus(HttpStatus.CREATED)
        public UserResponse confirmRegistration(@Valid @RequestBody ConfirmRegistrationRequest request) {
            return UserResponse.from(confirmRegistration.execute(request.email(), request.code()));
        }
    }