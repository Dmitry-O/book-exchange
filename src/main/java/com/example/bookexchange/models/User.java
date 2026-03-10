package com.example.bookexchange.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Builder
@Data
@Entity
@Table(name = "APP_USER")
@AllArgsConstructor
@ToString(exclude = {"books", "sentExchanges", "receivedExchanges", "declinedExchanges", "refreshTokens", "reports", "verificationToken"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @NotNull
    @Email
    private String email;

    private String password;

    @NotBlank
    @NotNull
    @Size(min = 5, max = 20)
    @Column(length = 20)
    private String nickname;

    private String photoBase64;

    @OneToMany(mappedBy = "user")
    @JsonManagedReference
    private Set<Book> books = new HashSet<>();

    @OneToMany(mappedBy = "senderUser")
    @JsonIgnore
    private List<Exchange> sentExchanges = new ArrayList<>();

    @OneToMany(mappedBy = "receiverUser")
    @JsonIgnore
    private List<Exchange> receivedExchanges = new ArrayList<>();

    @OneToMany(mappedBy = "declinerUser")
    @JsonIgnore
    private List<Exchange> declinedExchanges = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles = new HashSet<>();

    @JsonIgnore
    private Instant bannedUntil;

    @JsonIgnore
    @Column(nullable = false)
    private boolean bannedPermanently = false;

    @JsonIgnore
    private String banReason;

    @OneToMany(mappedBy = "reporter")
    @JsonManagedReference
    private Set<Report> reports = new HashSet<>();

    @JsonIgnore
    @Column(nullable = false)
    private boolean emailVerified = false;

    @OneToMany(mappedBy = "user")
    @JsonManagedReference
    private Set<VerificationToken> verificationToken = new HashSet<>();

    public User() {

    }

    public void addRole(UserRole role) {
        if (roles == null) {
            roles = new HashSet<>();
        }

        roles.add(role);
    }

    public void removeRole(UserRole role) {
        if (roles != null) {
            roles.remove(role);
        }
    }
}
