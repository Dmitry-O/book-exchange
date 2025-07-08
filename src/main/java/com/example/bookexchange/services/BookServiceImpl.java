package com.example.bookexchange.services;

import com.example.bookexchange.dto.BookCreateDTO;
import com.example.bookexchange.dto.BookDTO;
import com.example.bookexchange.mapper.BookMapper;
import com.example.bookexchange.models.Book;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.BookRepository;
import com.example.bookexchange.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class BookServiceImpl implements BookService {
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Override
    public BookDTO addUserBook(Long userId, BookCreateDTO dto) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new EntityNotFoundException("Benutzer mit ID " + userId + " wurde nicht gefunden")
        );

        Book book = BookMapper.toEntity(dto);
        book.setUser(user);
        Book savedBook = bookRepository.save(book);

        return BookMapper.fromEntity(savedBook);
    }

    @Override
    public List<BookDTO> findUserBooks(Long userId) {
        List<Book> books = bookRepository.findByUserId(userId);

        return books.stream()
                .map(BookMapper::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookDTO> findBooks() {
        List<Book> books = bookRepository.findAll();

        return books.stream()
                .map(BookMapper::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public String deleteUserBookById(Long userId, Long bookId) {
        bookRepository.deleteById(bookId);

        return "Dieser Buch mit ID " + bookId + " wurde entfernt";
    }

    @Transactional
    @Override
    public String updateUserBookById(Long userId, Long bookId, BookDTO dto) {
        Book book = bookRepository.findByIdAndUserId(userId, bookId).orElseThrow(() -> new EntityNotFoundException("Das Buch mit ID " + bookId + " oder mit user ID + " + userId + " wurde nicht gefunden"));

        if (dto.getName() != null) book.setName(dto.getName());
        if (dto.getDescription() != null) book.setDescription(dto.getDescription());
        if (dto.getAuthor() != null) book.setAuthor(dto.getAuthor());
        if (dto.getCategory() != null) book.setCategory(dto.getCategory());
        if (dto.getPublicationYear() != null) book.setPublicationYear(dto.getPublicationYear());
        if (dto.getPhotoBase64() != null) book.setPhotoBase64(dto.getPhotoBase64());
        if (dto.getCity() != null) book.setCity(dto.getCity());
        if (dto.getContactDetails() != null) book.setContactDetails(dto.getContactDetails());
        if (dto.getIsGift() != null) book.setIsGift(dto.getIsGift());

        return "Dieses Buch mit ID " + bookId + " wurde aktualisiert";
    }
}
