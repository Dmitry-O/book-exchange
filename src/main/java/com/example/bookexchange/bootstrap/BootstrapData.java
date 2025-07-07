package com.example.bookexchange.bootstrap;

import com.example.bookexchange.models.Book;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.BookRepository;
import com.example.bookexchange.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

//@Component
public class BootstrapData {
    //implements
//} CommandLineRunner {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public BootstrapData(UserRepository userRepository, BookRepository bookRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

//    @Override
    public void run(String... args) throws Exception {
//        User user1 = new User();
//        user1.setEmail("test.example.com");
//        User user1_saved = userRepository.save(user1);
//
//        Book book1 = new Book();
//        book1.setName("Book 1");
//        book1.setAuthor("John");
//        book1.setDescription("Book 1 Description");
//        book1.setCategory("Humor");
//        Book book1_saved = bookRepository.save(book1);
//
//        book1_saved.setUser(user1_saved);
//
//        bookRepository.save(book1_saved);
    }
}
