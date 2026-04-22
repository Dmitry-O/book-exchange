package com.example.bookexchange.book.api;

import com.example.bookexchange.common.dto.PageQueryDTO;
import com.example.bookexchange.security.auth.CurrentUser;
import com.example.bookexchange.book.dto.BookCreateDTO;
import com.example.bookexchange.book.dto.BookDTO;
import com.example.bookexchange.book.dto.BookSearchDTO;
import com.example.bookexchange.book.dto.BookUpdateDTO;
import com.example.bookexchange.common.swagger.page_data_response.BookPageData;
import com.example.bookexchange.common.swagger.error_response.BadRequestErrorResponse;
import com.example.bookexchange.common.swagger.error_response.ConflictErrorResponse;
import com.example.bookexchange.common.swagger.error_response.NotFoundErrorResponse;
import com.example.bookexchange.common.swagger.error_response.UnauthorizedErrorResponse;
import com.example.bookexchange.common.web.ApiResponse;
import com.example.bookexchange.common.web.ResultResponseMapper;
import com.example.bookexchange.book.service.BookService;
import com.example.bookexchange.common.util.ParserUtil;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Books")
@RequiredArgsConstructor
@RestController
public class BookController {

    private final ParserUtil parserUtil;
    private final BookService bookService;
    private final ResultResponseMapper responseMapper;

    @UnauthorizedErrorResponse
    @BadRequestErrorResponse
    @NotFoundErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "The book has been created",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "ownerUserId": 1,
                                            "ownerNickname": "user_12345",
                                            "ownerPhotoUrl": "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/1/profile_photo_1712582410000.jpg",
                                            "name": "Charley Smash",
                                            "description": "An interesting book about ...",
                                            "author": "Frank Oester",
                                            "category": "Drama",
                                            "publicationYear": 1765,
                                            "photoUrl": "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/42/books/15_1712582410000.jpg",
                                            "city": "string",
                                            "contactDetails": "Juchostr. 12, 44320 Dortmund; 01512 0089 34 567",
                                            "isGift": true,
                                            "isExchanged": false
                                          },
                                          "message": "The book has been created",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PostMapping(BookPaths.BOOK_PATH_USER)
    public ResponseEntity<?> addUserBook(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Book data for the book creation", required = true)
            @Validated @RequestBody BookCreateDTO dto,

            HttpServletRequest request
    ) {
        return responseMapper.map(bookService.addUserBook(userId, dto), request);
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "A list of returned user's books",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookPageData.class)
            )
    )
    @GetMapping(BookPaths.BOOK_PATH_USER)
    public ResponseEntity<?> getUserBooks(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @ParameterObject
            @Validated @ModelAttribute PageQueryDTO queryDTO,

            HttpServletRequest request
    ) {
        return responseMapper.map(bookService.findUserBooks(userId, queryDTO), request);
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "The returned user's book",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookDTO.class)
            )
    )
    @GetMapping(BookPaths.BOOK_PATH_USER_BOOK_ID)
    public ResponseEntity<?> getUserBookById(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @Parameter(description = "Book ID", example = "1")
            @PathVariable Long bookId,

            HttpServletRequest request
    ) {
        return responseMapper.map(bookService.findUserBookById(userId, bookId), request);
    }

    @NotFoundErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "The returned public book",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookDTO.class)
            )
    )
    @GetMapping(BookPaths.BOOK_PATH_BOOK_ID)
    public ResponseEntity<?> getBookById(
            @Parameter(description = "Book ID", example = "1")
            @PathVariable Long bookId,

            HttpServletRequest request
    ) {
        return responseMapper.map(bookService.findBookById(bookId), request);
    }

    @UnauthorizedErrorResponse
    @NotFoundErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "A list of returned exchanged user's books",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookPageData.class)
            )
    )
    @GetMapping(BookPaths.BOOK_PATH_HISTORY)
    public ResponseEntity<?> getExchangedUserBooks(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @ParameterObject
            @Validated @ModelAttribute PageQueryDTO queryDTO,

            HttpServletRequest request
    ) {
        return responseMapper.map(bookService.findExchangedUserBooks(userId, queryDTO), request);
    }

    @UnauthorizedErrorResponse
    @BadRequestErrorResponse
    @NotFoundErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "A list of returned books",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookPageData.class)
            )
    )
    @GetMapping(BookPaths.BOOK_PATH_SEARCH)
    public ResponseEntity<?> getBooks(
            @CurrentUser Long currentUserId,

            @ParameterObject
            @Validated @ModelAttribute PageQueryDTO queryDTO,

            @ParameterObject
            @Validated @ModelAttribute BookSearchDTO dto,

            HttpServletRequest request
    ) {
        return responseMapper.map(bookService.findBooks(currentUserId, dto, queryDTO), request);
    }

    @UnauthorizedErrorResponse
    @ConflictErrorResponse
    @NotFoundErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "The book has been deleted",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": null,
                                          "message": "The book has been deleted",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @DeleteMapping(BookPaths.BOOK_PATH_USER_BOOK_ID)
    public ResponseEntity<?> deleteBookById(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @Parameter(description = "Book ID", example = "1")
            @PathVariable Long bookId,

            @Parameter(
                    name = "If-Match",
                    description = "Entity version for optimistic locking",
                    example = "\"3\"",
                    required = true
            )
            @RequestHeader("If-Match") String ifMatch,

            HttpServletRequest request
    ) {
        return responseMapper.map(bookService.deleteUserBookById(userId, bookId, parserUtil.ifMatchParser(ifMatch)), request);
    }

    @UnauthorizedErrorResponse
    @ConflictErrorResponse
    @NotFoundErrorResponse
    @BadRequestErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "The book has been updated",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookDTO.class),
                    examples = @ExampleObject(
                            value = """
                                        {
                                          "success": true,
                                          "data": {
                                            "id": 1,
                                            "ownerUserId": 1,
                                            "ownerNickname": "user_12345",
                                            "ownerPhotoUrl": "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/1/profile_photo_1712582410000.jpg",
                                            "name": "Charley Smash",
                                            "description": "An interesting book about ...",
                                            "author": "Frank Oester",
                                            "category": "Drama",
                                            "publicationYear": 1765,
                                            "photoUrl": "https://book-exchange-prod.s3.eu-central-1.amazonaws.com/users/42/books/15_1712582410000.jpg",
                                            "city": "string",
                                            "contactDetails": "Juchostr. 12, 44320 Dortmund; 01512 0089 34 567",
                                            "isGift": true,
                                            "isExchanged": false
                                          },
                                          "message": "The book has been updated",
                                          "error": null
                                        }
                                """
                    )
            )
    )
    @PatchMapping(BookPaths.BOOK_PATH_USER_BOOK_ID)
    public ResponseEntity<?> updateUserBookById(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @Parameter(description = "Book ID", example = "1")
            @PathVariable Long bookId,

            @Parameter(
                    name = "If-Match",
                    description = "Entity version for optimistic locking",
                    example = "\"3\"",
                    required = true
            )
            @RequestHeader("If-Match") String ifMatch,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Book data for the update", required = true)
            @Validated @RequestBody BookUpdateDTO dto,

            HttpServletRequest request
    ) {
        return responseMapper.map(bookService.updateUserBookById(userId, bookId, dto, parserUtil.ifMatchParser(ifMatch)), request);
    }

    @UnauthorizedErrorResponse
    @ConflictErrorResponse
    @NotFoundErrorResponse
    @BadRequestErrorResponse
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "The book photo has been deleted",
            headers = @Header(
                    name = "ETag",
                    description = "Entity version",
                    schema = @Schema(type = "string", example = "\"3\"")
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BookDTO.class)
            )
    )
    @DeleteMapping(BookPaths.BOOK_PATH_USER_BOOK_ID_PHOTO)
    public ResponseEntity<?> deleteUserBookPhoto(
            @Parameter(description = "User ID", example = "1")
            @CurrentUser Long userId,

            @Parameter(description = "Book ID", example = "1")
            @PathVariable Long bookId,

            @Parameter(
                    name = "If-Match",
                    description = "Entity version for optimistic locking",
                    example = "\"3\"",
                    required = true
            )
            @RequestHeader("If-Match") String ifMatch,

            HttpServletRequest request
    ) {
        return responseMapper.map(bookService.deleteUserBookPhoto(userId, bookId, parserUtil.ifMatchParser(ifMatch)), request);
    }
}
