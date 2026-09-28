package com.smartlb.authservice.service;

import com.smartlb.authservice.service.impl.EmailServiceImpl;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.*;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService = new EmailServiceImpl(
                mailSender,
                "noreply@smartlb.ai",
                "http://localhost:8081",
                "/api/v1/auth/verify-email",
                "/api/v1/auth/reset-password"
        );
    }

    @Test
    @DisplayName("Should build verification email and invoke JavaMailSender")
    void testSendVerificationEmail() {
        emailService.sendVerificationEmail("user@smartlb.io", "sample-verification-token");

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should build password reset email and invoke JavaMailSender")
    void testSendPasswordResetEmail() {
        emailService.sendPasswordResetEmail("user@smartlb.io", "sample-reset-token");

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should re-send verification email and invoke JavaMailSender")
    void testResendVerificationEmail() {
        emailService.resendVerificationEmail("user@smartlb.io", "sample-resend-token");

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
