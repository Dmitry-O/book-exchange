package com.example.bookexchange.common.jobs;

public interface BookPhotoCleanupService {

    void deleteExpiredSoftDeletedBookPhotos();
}
