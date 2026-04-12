package com.example.bookexchange.common.storage;

import com.example.bookexchange.common.result.Result;
import org.springframework.stereotype.Service;

@Service
public interface ImageProcessingService {

    Result<ProcessedImage> process(String photoBase64);
}
