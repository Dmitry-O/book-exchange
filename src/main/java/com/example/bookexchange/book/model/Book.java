package com.example.bookexchange.book.model;

import com.example.bookexchange.exchange.model.Exchange;
import com.example.bookexchange.common.audit.model.SoftDeletableEntity;
import com.example.bookexchange.user.model.User;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Book extends SoftDeletableEntity {

    @PrePersist
    public void prePersist() {
        if (isGift == null) {
            isGift = false;
        }

        if (isExchanged == null) {
            isExchanged = false;
        }
    }

    @Id
    @EqualsAndHashCode.Include
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

    @Column(nullable = false)
    private Boolean isGift = Boolean.FALSE;

    @Column(nullable = false)
    private Boolean isExchanged = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    private User user;

    @OneToMany(mappedBy = "senderBook")
    @JsonIgnore
    private List<Exchange> sentBookExchanges = new ArrayList<>();

    @OneToMany(mappedBy = "receiverBook")
    @JsonIgnore
    private List<Exchange> receivedBookExchanges  = new ArrayList<>();
}
