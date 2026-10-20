package com.corsini.corsabox.identity.infrastructure.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity 
@Data 
@RequiredArgsConstructor 
public class UserEntity {
    @Id 
    private UUID id;

    @NotBlank 
    @Column(nullable = false)
    private String firstName;

    @NotBlank 
    @Column(nullable = false)
    private String lastName;

    @NotBlank 
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank 
    @Email 
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, updatable = false)
    private Instant createdOn;

    @PrePersist 
    public void prePersist(){
        if(id == null){
            id = UUID.randomUUID();
        }
    }
}
