package com.example.bookexchange.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String author;
    private String category;
    private Integer publicationYear;
    private String photoBase64;
    private String city;
    private String contactDetails;
    private Boolean isGift;

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
