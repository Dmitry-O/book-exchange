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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Page<BookDTO> findUserBooks(Long userId, Integer pageIndex, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<Book> bookPage = bookRepository.findByUserIdAndIsExchanged(userId, false, pageable);

        return bookPage.map(BookMapper::fromEntity);
    }

    @Override
    public Page<BookDTO> findExchangedUserBooks(Long userId, Integer pageIndex, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<Book> bookPage = bookRepository.findByUserIdAndIsExchanged(userId, true, pageable);

        return bookPage.map(BookMapper::fromEntity);
    }

    @Override
    public Page<BookDTO> findBooks(BookSearchDTO dto, Integer pageIndex, Integer pageSize) {
        Specification<Book> specification = BookSpecificationBuilder.build(dto);

        Pageable pageable;

        if (dto.getSortBy() != null && dto.getSortDirection() != null) {
            Sort.Direction direction = dto.getSortDirection().equalsIgnoreCase(("desc"))
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;

            pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, dto.getSortBy()));
        } else {
            pageable = PageRequest.of(pageIndex, pageSize);
        }

        Page<Book> bookPage = bookRepository.findAll(specification, pageable);

        return bookPage.map(BookMapper::fromEntity);
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
