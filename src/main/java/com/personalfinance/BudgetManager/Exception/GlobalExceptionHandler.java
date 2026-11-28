package com.personalfinance.BudgetManager.Exception;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.personalfinance.BudgetManager.DTO.ApiError;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Set;
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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException exception, HttpServletRequest request){
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

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException exception, HttpServletRequest request){
        return buildError(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({ConcurrencyFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ApiError> handleConcurrentModification(Exception exception, HttpServletRequest request){
        log.warn("Concurrent modification for {}: {}", request.getRequestURI(), exception.getMessage());
        return buildError(HttpStatus.CONFLICT, "The data was changed by someone else at the same time. Refresh and try again.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException exception, HttpServletRequest request){
        log.warn("Data integrity violation for {}: {}", request.getRequestURI(), exception.getMostSpecificCause().getMessage());
        return buildError(HttpStatus.CONFLICT, "This item is in use and cannot be changed or deleted", request);
    }

    //  429 TOO MANY REQUESTS — za dużo nieudanych logowań
    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<ApiError> handleTooManyLoginAttempts(TooManyLoginAttemptsException exception, HttpServletRequest request){
        return buildError(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage(), request);
    }

    //  422 UNPROCESSABLE ENTITY — logika biznesowa

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiError> handleInsufficientFunds(InsufficientFundsException exception, HttpServletRequest request){
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    }

    @ExceptionHandler(InvitationException.class)
    public ResponseEntity<ApiError> handleInvitationException(InvitationException exception, HttpServletRequest request){
        return buildError(exception.getStatus(), exception.getMessage(), request);
    }


    //  400 BAD REQUEST — błędne dane wejściowe

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<ApiError> handleInvalidCurrentPassword(InvalidCurrentPasswordException exception, HttpServletRequest request){
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Invalid value for parameter: " + exception.getName(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        if (exception.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String field = mismatch.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."));
            return buildError(HttpStatus.BAD_REQUEST, "Invalid value for field: " + field, request);
        }
        return buildError(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException exception, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Missing required parameter: " + exception.getParameterName(), request);
    }

    @ExceptionHandler(DateTimeException.class)
    public ResponseEntity<ApiError> handleInvalidDate(DateTimeException exception, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Invalid date: " + exception.getMessage(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, violationMessages(exception.getConstraintViolations()), request);
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiError> handleTransactionSystem(TransactionSystemException exception, HttpServletRequest request) {
        if (exception.getRootCause() instanceof ConstraintViolationException violation) {
            return buildError(HttpStatus.BAD_REQUEST, violationMessages(violation.getConstraintViolations()), request);
        }
        return handleGenericException(exception, request);
    }

    private static String violationMessages(Set<ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .collect(Collectors.joining("; "));
    }


    //  404 / 405 / 415 — zły adres, metoda albo format żądania

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException exception, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "Not found", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        ResponseEntity<ApiError> response = buildError(HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method " + exception.getMethod() + " is not supported for this path", request);
        Set<HttpMethod> allowed = exception.getSupportedHttpMethods();
        if (allowed == null || allowed.isEmpty()) {
            return response;
        }
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .allow(allowed.toArray(HttpMethod[]::new))
                .body(response.getBody());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        return buildError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type must be application/json", request);
    }

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

        return buildError(HttpStatus.UNAUTHORIZED, "Invalid email or password ", request);
    }


    //  500 INTERNAL SERVER ERROR — nieprzewidziane błędy

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception exception, HttpServletRequest request) {
        log.error("Unhandled error for {}", request.getRequestURI(), exception);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.", request);
    }
}
