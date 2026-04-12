package com.example.bookexchange.common.storage;

import com.example.bookexchange.common.result.Result;
import org.springframework.stereotype.Service;

@Service
public interface ImageStorageService {

    Result<String> replaceUserProfileImage(Long userId, String photoBase64);

    Result<String> replaceBookImage(Long userId, Long bookId, String photoBase64);

    Result<Void> deleteUserProfileImage(Long userId);

    Result<Void> deleteBookImage(Long userId, Long bookId);

    Result<Void> deleteAllUserImages(Long userId);
}
