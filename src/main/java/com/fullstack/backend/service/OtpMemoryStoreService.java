package com.fullstack.backend.service;

import com.fullstack.backend.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OtpMemoryStoreService {

    private final Map<String, String> otpStore = new ConcurrentHashMap<>();
    private final Map<String, Long> otpExpiry = new ConcurrentHashMap<>();
    private final Map<String, Long> verifiedResetStore = new ConcurrentHashMap<>();
    private final long expiryMillis;

    public OtpMemoryStoreService(@Value("${app.auth.otp.expiry-minutes:5}") int expiryMinutes) {
        this.expiryMillis = expiryMinutes * 60L * 1000L;
    }

    public String generateOtp(String email) {
        cleanup(email);
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        otpStore.put(email, otp);
        otpExpiry.put(email, System.currentTimeMillis() + expiryMillis);
        verifiedResetStore.remove(email);
        return otp;
    }

    public void verifyOtp(String email, String otp) {
        cleanup(email);

        String storedOtp = otpStore.get(email);
        Long expiry = otpExpiry.get(email);

        if (storedOtp == null || expiry == null) {
            throw new BadRequestException("OTP not found. Please request a new OTP.");
        }

        if (expiry < System.currentTimeMillis()) {
            clear(email);
            throw new BadRequestException("OTP expired. Please request a new OTP.");
        }

        if (!storedOtp.equals(otp.trim())) {
            throw new BadRequestException("Invalid OTP. Please try again.");
        }

        verifiedResetStore.put(email, System.currentTimeMillis() + expiryMillis);
        otpStore.remove(email);
        otpExpiry.remove(email);
    }

    public boolean isResetAllowed(String email) {
        cleanup(email);
        Long verifiedUntil = verifiedResetStore.get(email);
        return verifiedUntil != null && verifiedUntil >= System.currentTimeMillis();
    }

    public void clear(String email) {
        otpStore.remove(email);
        otpExpiry.remove(email);
        verifiedResetStore.remove(email);
    }

    public void cleanupAll() {
        otpStore.keySet().forEach(this::cleanup);
        verifiedResetStore.keySet().forEach(this::cleanup);
    }

    private void cleanup(String email) {
        Long otpExpiresAt = otpExpiry.get(email);
        if (otpExpiresAt != null && otpExpiresAt < System.currentTimeMillis()) {
            otpStore.remove(email);
            otpExpiry.remove(email);
        }

        Long verifiedUntil = verifiedResetStore.get(email);
        if (verifiedUntil != null && verifiedUntil < System.currentTimeMillis()) {
            verifiedResetStore.remove(email);
        }
    }
}
