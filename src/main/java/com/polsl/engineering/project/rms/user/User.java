package com.polsl.engineering.project.rms.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = {"username"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User {

    public static final int USERNAME_MAX_LENGTH = 100;
    public static final int USERNAME_MIN_LENGTH = 3;

    public static final int PASSWORD_MAX_LENGTH = 32;
    public static final int PASSWORD_MIN_LENGTH = 8;

    public static final int FIRST_NAME_MAX_LENGTH = 100;
    public static final int FIRST_NAME_MIN_LENGTH = 3;

    public static final int LAST_NAME_MAX_LENGTH = 100;
    public static final int LAST_NAME_MIN_LENGTH = 2;

    public static final int PHONE_NUMBER_MAX_LENGTH = 32;
    public static final int PHONE_NUMBER_MIN_LENGTH = 9;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "username", nullable = false, length = USERNAME_MAX_LENGTH)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false, length = FIRST_NAME_MAX_LENGTH)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = LAST_NAME_MAX_LENGTH)
    private String lastName;

    @Column(name = "phone_number", length = PHONE_NUMBER_MAX_LENGTH)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }

}
