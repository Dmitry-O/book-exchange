package com.example.bookexchange.services;

import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private UserRepository userRepository;

    @Override
    public User getUser(Long userId) {
        Optional<User> user = userRepository.findById(userId);

        if (user.isEmpty()) {
            throw new IllegalArgumentException("Benutzer mit ID " + userId + " wurde nicht gefunden");
        }

        return user.get();
    }

    @Override
    public User createUser(User user) {
        String nickname = user.getNickname();

        Optional<User> userWithSameNickname = userRepository.findByNickname(nickname);

        if (userWithSameNickname.isPresent()) {
            throw new IllegalArgumentException("Es gibt bereits einen Benutzer mit diesem nickname. Wählen Sie bitte ein anderes");
        }

        return userRepository.save(user);
    }

    @Override
    public User updateUser(Long userId, User user) {
        Optional<User> oldUser = userRepository.findById(userId);

        if (oldUser.isEmpty()) {
            throw new IllegalArgumentException("Benutzer mit ID " + userId + " wurde nicht gefunden");
        }

        if (!oldUser.get().getNickname().equals(user.getNickname())) {
            String nickname = user.getNickname();

            Optional<User> userWithSameNickname = userRepository.findByNickname(nickname);

            if (userWithSameNickname.isPresent()) {
                throw new IllegalArgumentException("Es gibt bereits einen Benutzer mit diesem nickname. Wählen Sie bitte ein anderes");
            }
        }

        User userToBeUpdated = oldUser.get();
        userToBeUpdated.setEmail(user.getEmail());
        userToBeUpdated.setNickname(user.getNickname());
        userToBeUpdated.setPhotoBase64(user.getPhotoBase64());

        return userRepository.save(userToBeUpdated);
    }

    @Override
    public String deleteUser(Long userId) {
        Optional<User> user = userRepository.findById(userId);

        if (user.isEmpty()) {
            throw new IllegalArgumentException("Benutzer mit ID " + userId + " wurde nicht gefunden");
        }

        userRepository.delete(user.get());

        return "Benutzer wurde entfernt";
    }
}
