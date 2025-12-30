package com.example.bookexchange.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @NotNull
    @Size(min = 3, max = 25)
    @Column(length = 25)
    private String name;

    @NotBlank
    @NotNull
    @Size(min = 3, max = 255)
    private String description;

    @NotBlank
    @NotNull
    @Size(min = 3, max = 25)
    @Column(length = 25)
    private String author;

    @NotBlank
    @NotNull
    @Size(min = 3, max = 20)
    @Column(length = 20)
    private String category;

    @NotNull
    private Integer publicationYear;

    private String photoBase64;

    @NotBlank
    @NotNull
    @Size(min = 3, max = 25)
    @Column(length = 25)
    private String city;

    @NotBlank
    @NotNull
    private String contactDetails;

    private Boolean isGift = Boolean.FALSE;

    private Boolean isExchanged = Boolean.FALSE;

    @ManyToOne
    @JsonBackReference
    private User user;

    @OneToMany(mappedBy = "senderBook")
    @JsonIgnore
    private List<Exchange> sentBookExchanges = new ArrayList<>();

    @OneToMany(mappedBy = "receiverBook")
    @JsonIgnore
    private List<Exchange> receivedBookExchanges  = new ArrayList<>();
}
