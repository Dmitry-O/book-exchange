package com.example.bookexchange.services;

import com.example.bookexchange.dto.BookCreateDTO;
import com.example.bookexchange.dto.BookDTO;
import com.example.bookexchange.dto.BookSearchDTO;
import com.example.bookexchange.mapper.BookMapper;
import com.example.bookexchange.models.Book;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.BookRepository;
import com.example.bookexchange.repositories.UserRepository;
import com.example.bookexchange.specification.BookSpecificationBuilder;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class BookServiceImpl extends BaseServiceImpl<User, Long> implements BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Override
    public BookDTO addUserBook(Long userId, BookCreateDTO dto) {
        User user = findOrThrow(userRepository, userId, "Der Benutzer mit ID " + userId + " wurde nicht gefunden");

        Book book = BookMapper.toEntity(dto);
        book.setUser(user);
        Book savedBook = bookRepository.save(book);

        return BookMapper.fromEntity(savedBook);
    }

    @Override
    public List<BookDTO> findUserBooks(Long userId) {
        List<Book> books = bookRepository.findByUserIdAndIsExchanged(userId, false);

        return books.stream()
                .map(BookMapper::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookDTO> findExchangedUserBooks(Long userId) {
        List<Book> books = bookRepository.findByUserIdAndIsExchanged(userId, true);

        return books.stream()
                .map(BookMapper::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookDTO> findBooks(BookSearchDTO dto) {
        Specification<Book> specification = BookSpecificationBuilder.build(dto);

        Sort sort = Sort.unsorted();

        if (dto.getSortBy() != null && dto.getSortDirection() != null) {
            Sort.Direction direction = dto.getSortDirection().equalsIgnoreCase(("desc"))
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;

            sort = Sort.by(direction, dto.getSortBy());
        }

        List<Book> books = bookRepository.findAll(specification, sort);

        return books.stream()
                .map(BookMapper::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public String deleteUserBookById(Long userId, Long bookId) {
        bookRepository.deleteByIdAndUserId(bookId, userId);

        return "Dieses Buch mit ID " + bookId + " wurde entfernt";
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
        if (dto.getIsGift() != null) book.setIsGift(dto.getIsGift());

        return "Dieses Buch mit ID " + bookId + " wurde aktualisiert";
    }
}
