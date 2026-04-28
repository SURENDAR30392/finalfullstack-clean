package com.fullstack.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MILLIS = 1000L;

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String mailPassword;
    private final int otpExpiryMinutes;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String fromEmail,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${app.auth.otp.expiry-minutes:5}") int otpExpiryMinutes
    ) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.mailPassword = mailPassword;
        this.otpExpiryMinutes = otpExpiryMinutes;
    }

    public boolean sendOtpEmail(String toEmail, String otp) {
        if (!isMailConfigured()) {
            LOGGER.warn("Skipping OTP email because Gmail SMTP is not configured.");
            return false;
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                mailSender.send(buildOtpMessage(toEmail, otp));
                LOGGER.info("OTP email sent to {} on attempt {}", toEmail, attempt);
                return true;
            } catch (MailException exception) {
                LOGGER.warn("Failed to send OTP email to {} on attempt {}", toEmail, attempt, exception);
                if (attempt < MAX_ATTEMPTS) {
                    pauseBeforeRetry();
                }
            }
        }

        LOGGER.error("OTP email could not be delivered to {} after {} attempts.", toEmail, MAX_ATTEMPTS);
        return false;
    }

    private SimpleMailMessage buildOtpMessage(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("OTP Verification - LMS");
        message.setText("Your OTP is: " + otp + ". It is valid for " + otpExpiryMinutes + " minutes.");
        return message;
    }

    private boolean isMailConfigured() {
        return fromEmail != null
                && !fromEmail.isBlank()
                && !"YOUR_GMAIL".equalsIgnoreCase(fromEmail)
                && mailPassword != null
                && !mailPassword.isBlank()
                && !"YOUR_APP_PASSWORD".equalsIgnoreCase(mailPassword);
    }

    private void pauseBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            LOGGER.warn("OTP email retry sleep interrupted.");
        }
    }
}
