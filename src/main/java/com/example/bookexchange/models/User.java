package com.example.bookexchange.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Builder
@Data
@Entity
@Table(name = "APP_USER")
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String email;
    private String nickname;
    private String photoBase64;

    @OneToMany(mappedBy = "user")
    @JsonManagedReference
    private Set<Book> books = new HashSet<>();

    public User() {

    }
}
