package com.smartlb.authservice.service;

/**
 * Asynchronous notification service interface responsible for dispatching system emails,
 * email verification links, and password recovery tokens.
 */
public interface EmailService {

    /**
     * Dispatches an initial email verification link to a newly registered user.
     *
     * @param toEmail destination user email address
     * @param token unique verification token string
     */
    void sendVerificationEmail(String toEmail, String token);

    /**
     * Dispatches a password recovery email containing a password reset token.
     *
     * @param toEmail destination user email address
     * @param token unique password recovery token string
     */
    void sendPasswordResetEmail(String toEmail, String token);

    /**
     * Re-sends an active email verification link to a user requesting re-verification.
     *
     * @param toEmail destination user email address
     * @param token unique verification token string
     */
    void resendVerificationEmail(String toEmail, String token);
}
