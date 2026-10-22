package com.corsini.corsabox.identity.api.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.corsini.corsabox.identity.api.request.RegisterUserRequest;
import com.corsini.corsabox.identity.api.request.UpdateProfileRequest;
import com.corsini.corsabox.identity.api.response.UserResponse;
import com.corsini.corsabox.identity.api.response.UserSummaryResponse;
import com.corsini.corsabox.identity.application.dto.RegisterUserCommand;
import com.corsini.corsabox.identity.application.dto.UpdateProfileCommand;
import com.corsini.corsabox.identity.application.service.GetCurrentUserUseCase;
import com.corsini.corsabox.identity.application.service.ListUserUseCase;
import com.corsini.corsabox.identity.application.service.RegisterUserUseCase;
import com.corsini.corsabox.identity.application.service.UpdateProfileUseCase;

import jakarta.validation.Valid;


@RestController 
@RequestMapping("/api/users")
public class UserController {
    private final RegisterUserUseCase registerUser;
    private final ListUserUseCase listUsers;
    private final GetCurrentUserUseCase getCurrentUser;
    private final UpdateProfileUseCase updateProfile;


    public UserController(RegisterUserUseCase registerUser, ListUserUseCase listUsers, GetCurrentUserUseCase getCurrentUser, UpdateProfileUseCase updateProfile) {
        this.registerUser = registerUser;
        this.listUsers = listUsers;
        this.getCurrentUser = getCurrentUser;
        this.updateProfile = updateProfile;
    }

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        var command = new RegisterUserCommand(
            request.firstName(),
            request.lastName(),
            request.username(),
            request.email(),
            request.password());   

        var user = registerUser.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @GetMapping
    public Page<UserSummaryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return listUsers.execute(pageable).map(UserSummaryResponse::from);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(getCurrentUser.execute(UUID.fromString(jwt.getSubject())));
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        var command = new UpdateProfileCommand(
            UUID.fromString(jwt.getSubject()),
            request.firstName(),
            request.lastName(),
            request.username());
            
        return UserResponse.from(updateProfile.execute(command));
    }
} 
