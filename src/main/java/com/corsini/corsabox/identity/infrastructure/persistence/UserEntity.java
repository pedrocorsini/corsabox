package com.corsini.corsabox.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity 
@Table(name = "users")
@Getter 
@Setter 
@NoArgsConstructor  
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

    @Column
    private String passwordHash;

    @PrePersist 
    public void prePersist(){
        if(id == null){
            id = UUID.randomUUID();
        }
    }
}
