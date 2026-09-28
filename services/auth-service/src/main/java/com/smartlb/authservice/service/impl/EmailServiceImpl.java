package com.smartlb.authservice.service.impl;

import com.smartlb.authservice.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service implementation for dispatching HTML system emails asynchronously via
 * Spring's {@link JavaMailSender} (SMTP).
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String baseUrl;
    private final String verificationPath;
    private final String resetPasswordPath;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.email.from}") String fromAddress,
            @Value("${app.base-url}") String baseUrl,
            @Value("${app.email.verification-path}") String verificationPath,
            @Value("${app.email.reset-password-path}") String resetPasswordPath) {
        this.mailSender      = mailSender;
        this.fromAddress     = fromAddress;
        this.baseUrl         = baseUrl;
        this.verificationPath = verificationPath;
        this.resetPasswordPath = resetPasswordPath;
    }

    /**
     * {@inheritDoc}
     * Sends an HTML email with a clickable verification link constructed from the base URL and token.
     */
    @Override
    @Async
    public void sendVerificationEmail(String toEmail, String token) {
        String link    = buildLink(verificationPath, token);
        String subject = "Verify your SmartLB-AI account";
        String body    = buildVerificationEmailBody(link);
        sendHtmlEmail(toEmail, subject, body);
    }

    /**
     * {@inheritDoc}
     * Sends an HTML password reset email containing the reset link and a 1-hour expiry notice.
     */
    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        String link    = buildLink(resetPasswordPath, token);
        String subject = "Reset your SmartLB-AI password";
        String body    = buildPasswordResetEmailBody(link);
        sendHtmlEmail(toEmail, subject, body);
    }

    /**
     * {@inheritDoc}
     * Identical to {@link #sendVerificationEmail} — re-sends with a fresh token link.
     */
    @Override
    @Async
    public void resendVerificationEmail(String toEmail, String token) {
        sendVerificationEmail(toEmail, token);
    }

    // ─────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────

    /**
     * Dispatches an HTML MIME email message.
     *
     * @param toEmail   recipient address
     * @param subject   email subject line
     * @param htmlBody  full HTML body content
     */
    private void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} with subject: {}", toEmail, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Builds an absolute action URL from the path and token query parameter.
     *
     * @param path  server-relative endpoint path
     * @param token unique action token
     * @return full action URL string
     */
    private String buildLink(String path, String token) {
        return baseUrl + path + "?token=" + token;
    }

    /**
     * Produces the HTML body content for the email verification message.
     *
     * @param verificationLink full verification URL
     * @return HTML string
     */
    private String buildVerificationEmailBody(String verificationLink) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
              <h2>Welcome to SmartLB-AI!</h2>
              <p>Thank you for registering. Please verify your email address to activate your account.</p>
              <p>
                <a href="%s" style="background-color:#4F46E5;color:white;padding:12px 24px;
                   text-decoration:none;border-radius:6px;display:inline-block;">
                  Verify Email Address
                </a>
              </p>
              <p>This link expires in 24 hours. If you did not create an account, please ignore this email.</p>
            </body>
            </html>
            """.formatted(verificationLink);
    }

    /**
     * Produces the HTML body content for the password reset message.
     *
     * @param resetLink full password reset URL
     * @return HTML string
     */
    private String buildPasswordResetEmailBody(String resetLink) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
              <h2>SmartLB-AI Password Reset</h2>
              <p>We received a request to reset your password. Click the button below to set a new password.</p>
              <p>
                <a href="%s" style="background-color:#EF4444;color:white;padding:12px 24px;
                   text-decoration:none;border-radius:6px;display:inline-block;">
                  Reset Password
                </a>
              </p>
              <p>This link expires in 1 hour. If you did not request a password reset, please ignore this email.</p>
            </body>
            </html>
            """.formatted(resetLink);
    }
}
