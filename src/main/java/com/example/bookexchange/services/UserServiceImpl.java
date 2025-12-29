package com.example.bookexchange.services;

import com.example.bookexchange.dto.UserCreateDTO;
import com.example.bookexchange.dto.UserDTO;
import com.example.bookexchange.dto.UserUpdateDTO;
import com.example.bookexchange.mappers.UserMapper;
import com.example.bookexchange.models.User;
import com.example.bookexchange.repositories.BookRepository;
import com.example.bookexchange.repositories.UserRepository;
import jakarta.persistence.EntityExistsException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserServiceImpl extends BaseServiceImpl<User, Long> implements UserService {

    private UserRepository userRepository;
    private UserMapper userMapper;

    private BookRepository bookRepository;

    @Override
    public UserDTO getUser(Long userId) {
        User user = findOrThrow(userRepository, userId, "Der Benutzer mit ID " + userId + " wurde nicht gefunden");

        return userMapper.userToUserDto(user);
    }

    @Override
    public UserDTO createUser(UserCreateDTO dto) {
        String email = dto.getEmail();
        String nickname = dto.getNickname();

        userRepository.findByEmail(email).ifPresent(user -> {
            throw new EntityExistsException("Es gibt bereits ein Benutzer mit diesem Email. Wählen Sie bitte ein anderes");
        });

        userRepository.findByNickname(nickname).ifPresent(user -> {
            throw new EntityExistsException("Es gibt bereits ein Benutzer mit diesem Nickname. Wählen Sie bitte einen anderen");
        });

        User savedUser = userRepository.save(userMapper.userDtoToUser(dto));

        return userMapper.userToUserDto((savedUser));
    }

    @Transactional
    @Override
    public void updateUser(Long userId, UserUpdateDTO dto) {
        User user = findOrThrow(userRepository, userId, "Der Benutzer mit ID " + userId + " wurde nicht gefunden");

        if (!user.getNickname().equals(dto.getNickname())) {
            String nickname = dto.getNickname();

            if (userRepository.findByNickname(nickname).isPresent()) {
                throw new EntityExistsException("Es gibt bereits ein Benutzer mit diesem nickname. Wählen Sie bitte einen anderen");
            }
        }

        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getPhotoBase64() != null) user.setPhotoBase64(dto.getPhotoBase64());

        userRepository.save(user);
    }

    @Override
    public void deleteUser(Long userId) {
        findOrThrow(userRepository, userId, "Der Benutzer mit ID " + userId + " wurde nicht gefunden");

        userRepository.deleteById(userId);
    }
}
