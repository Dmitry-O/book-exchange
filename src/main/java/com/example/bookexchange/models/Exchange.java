package com.example.bookexchange.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Exchange {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ExchangeStatus status;

    @ManyToOne
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User senderUser;

    @ManyToOne
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private User receiverUser;

    @ManyToOne
    @JoinColumn(name = "decliner_user_id")
    private User declinerUser;

    @ManyToOne
    @JoinColumn(name = "sender_book_id")
    private Book senderBook;

    @ManyToOne
    @JoinColumn(name = "receiver_book_id", nullable = false)
    private Book receiverBook;
}
