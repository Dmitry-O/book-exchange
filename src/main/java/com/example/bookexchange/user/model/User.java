package com.example.bookexchange.user.model;

import com.example.bookexchange.auth.model.RefreshToken;
import com.example.bookexchange.auth.model.VerificationToken;
import com.example.bookexchange.book.model.Book;
import com.example.bookexchange.common.audit.model.SoftDeletableEntity;
import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.report.model.Report;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.hibernate.annotations.BatchSize;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Getter
@Setter
@Entity
@Table(name = "app_user")
@AllArgsConstructor
@ToString(exclude = {"books", "sentExchanges", "receivedExchanges", "declinedExchanges", "refreshTokens", "reports", "verificationToken"})
public class User extends SoftDeletableEntity {

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

    @Column(name = "photo_url", length = 1024)
    private String photoUrl;

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
    private Set<RefreshToken> refreshTokens = new HashSet<>();

    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.LAZY)
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

    @JsonIgnore
    private String locale;

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
