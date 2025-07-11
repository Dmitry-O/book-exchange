package com.example.bookexchange.services;

import com.example.bookexchange.dto.UserCreateDTO;
import com.example.bookexchange.dto.UserDTO;
import com.example.bookexchange.mapper.UserMapper;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.UserRepository;
import jakarta.persistence.EntityExistsException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserServiceImpl extends BaseServiceImpl<User, Long> implements UserService {

    private UserRepository userRepository;

    @Override
    public UserDTO getUser(Long userId) {
        User user = findOrThrow(userRepository, userId, "Der Benutzer mit ID " + userId + " wurde nicht gefunden");

        return UserMapper.fromEntity(user);
    }

    @Override
    public UserDTO createUser(UserCreateDTO dto) {
        String nickname = dto.getNickname();

        userRepository.findByNickname(nickname).ifPresent(user -> {
            throw new EntityExistsException("Es gibt bereits einen Benutzer mit diesem Nickname. Wählen Sie bitte ein anderes.");
        });

        User savedUser = userRepository.save(UserMapper.toEntity(dto));

        return UserMapper.fromEntity(savedUser);
    }

    @Transactional
    @Override
    public String updateUser(Long userId, UserDTO dto) {
        User user = findOrThrow(userRepository, userId, "Der Benutzer mit ID " + userId + " wurde nicht gefunden");

        if (!user.getNickname().equals(dto.getNickname())) {
            String nickname = dto.getNickname();

            userRepository.findByNickname(nickname).ifPresent(existingUser -> {
                throw new EntityExistsException("Es gibt bereits einen Benutzer mit diesem nickname. Wählen Sie bitte ein anderes");
            });
        }

        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getPhotoBase64() != null) user.setPhotoBase64(dto.getPhotoBase64());

        userRepository.save(user);

        return "Dieser Benutzer mit ID " + userId + " wurde aktualisiert";

    }

    @Override
    public String deleteUser(Long userId) {
        User user = findOrThrow(userRepository, userId, "Der Benutzer mit ID " + userId + " wurde nicht gefunden");

        userRepository.delete(user);

        return "Der Benutzer wurde entfernt";
    }
}
