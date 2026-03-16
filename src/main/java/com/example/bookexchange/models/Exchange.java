package com.example.bookexchange.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Exchange extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @NotNull
    private ExchangeStatus status;

    @Column(nullable = false)
    private Boolean isReadBySender = Boolean.FALSE;

    @Column(nullable = false)
    private Boolean isReadByReceiver = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id")
    private User senderUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_id")
    private User receiverUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decliner_user_id")
    private User declinerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_book_id")
    private Book senderBook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_book_id")
    private Book receiverBook;
}
