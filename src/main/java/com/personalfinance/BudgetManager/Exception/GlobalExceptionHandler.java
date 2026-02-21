package com.personalfinance.BudgetManager.Exception;

import com.personalfinance.BudgetManager.DTO.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ApiError> buildError(HttpStatus status, String message, HttpServletRequest request) {
        ApiError error = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase().toUpperCase().replace(" ", "_"),
                message,
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, status);
    }

    //  404 NOT FOUND — zasoby nie istnieją

    @ExceptionHandler(AccountException.class)
    public ResponseEntity<ApiError> handleAccountException(AccountException exception, HttpServletRequest request){
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiError> handleUserException(UserException exception, HttpServletRequest request){
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(CategoryException.class)
    public ResponseEntity<ApiError> handleCategoryException(CategoryException exception, HttpServletRequest request){
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(SubcategoryException.class)
    public ResponseEntity<ApiError> handleSubcategoryException(SubcategoryException exception, HttpServletRequest request){
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<ApiError> handleTransactionException(TransactionException exception, HttpServletRequest request){
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(TransferException.class)
    public ResponseEntity<ApiError> handleTransferException(TransferException exception, HttpServletRequest request){
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(GroupException.class)
    public ResponseEntity<ApiError> handleGroupException(GroupException exception, HttpServletRequest request){
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }


    //  409 CONFLICT — unikalnosc danych

    @ExceptionHandler(EmailException.class)
    public ResponseEntity<ApiError> handleEmailException(EmailException exception, HttpServletRequest request){
        return buildError(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    //  422 UNPROCESSABLE ENTITY — logika biznesowa

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiError> handleInsufficientFunds(InsufficientFundsException exception, HttpServletRequest request){
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    }

    @ExceptionHandler(InvitationException.class)
    public ResponseEntity<ApiError> handleInvitationException(InvitationException exception, HttpServletRequest request){
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    }


    //  400 BAD REQUEST — błędne dane wejściowe

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception, HttpServletRequest request){
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return buildError(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException exception, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalStateException(IllegalStateException exception, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }


    // 403 FORBIDDEN — brak uprawnień do zasobu

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, "You don't have permission to perform this operation", request);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiError> handleSecurityException(SecurityException exception, HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }


    // 401 UNAUTHORIZED — błędne dane logowania lub brak tokena

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException exception, HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
        public ResponseEntity<ApiError> handleInsufficientAuth(InsufficientAuthenticationException exception, HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "Session expired or missing token. Please log in again.", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleGenericAuthError(AuthenticationException exception, HttpServletRequest request) {

        return buildError(HttpStatus.UNAUTHORIZED, "Authentication error: " + exception.getMessage(), request);
    }


    //  500 INTERNAL SERVER ERROR — nieprzewidziane błędy

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception exception, HttpServletRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.", request);
    }
}
