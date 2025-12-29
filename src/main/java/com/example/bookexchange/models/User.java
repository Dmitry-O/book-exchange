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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Builder
@Data
@Entity
@Table(name = "APP_USER")
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @NotNull
    private Long id;

    @NotBlank
    @NotNull
    @Email
    private String email;

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

    public User() {

    }
}
