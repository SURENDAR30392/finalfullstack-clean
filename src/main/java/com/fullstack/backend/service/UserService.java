package com.fullstack.backend.service;

import com.fullstack.backend.config.JwtUtil;
import com.fullstack.backend.dto.ApiMessageResponse;
import com.fullstack.backend.dto.AuthRequest;
import com.fullstack.backend.dto.AuthResponse;
import com.fullstack.backend.dto.ForgotPasswordOtpRequest;
import com.fullstack.backend.dto.GoogleAuthRequest;
import com.fullstack.backend.dto.RegisterRequest;
import com.fullstack.backend.dto.ResetPasswordRequest;
import com.fullstack.backend.dto.UserResponse;
import com.fullstack.backend.dto.VerifyOtpRequest;
import com.fullstack.backend.entity.Course;
import com.fullstack.backend.entity.EmailUser;
import com.fullstack.backend.entity.User;
import com.fullstack.backend.exception.BadRequestException;
import com.fullstack.backend.exception.ResourceNotFoundException;
import com.fullstack.backend.repository.CourseRepository;
import com.fullstack.backend.repository.EmailUserRepository;
import com.fullstack.backend.repository.EnrollmentRepository;
import com.fullstack.backend.repository.UserRepository;
import com.fullstack.backend.repository.VideoProgressRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private static final String PROVIDER_LOCAL = "LOCAL";
    private static final String PROVIDER_GOOGLE = "GOOGLE";
    private static final String PROVIDER_SYSTEM = "SYSTEM";

    private final UserRepository userRepository;
    private final EmailUserRepository emailUserRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final VideoProgressRepository videoProgressRepository;
    private final VideoLinkService videoLinkService;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final ModelMapper modelMapper;
    private final long otpExpiryMinutes;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(
            UserRepository userRepository,
            EmailUserRepository emailUserRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            VideoProgressRepository videoProgressRepository,
            VideoLinkService videoLinkService,
            EmailService emailService,
            JwtUtil jwtUtil,
            ModelMapper modelMapper,
            @Value("${app.auth.otp.expiry-minutes:5}") long otpExpiryMinutes
    ) {
        this.userRepository = userRepository;
        this.emailUserRepository = emailUserRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.videoProgressRepository = videoProgressRepository;
        this.videoLinkService = videoLinkService;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.modelMapper = modelMapper;
        this.otpExpiryMinutes = otpExpiryMinutes;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (emailUserRepository.findByEmail(email).isPresent() || userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("Email already registered.");
        }

        String normalizedRole = normalizeRole(request.role());
        String encodedPassword = passwordEncoder.encode(request.password());

        EmailUser emailUser = new EmailUser();
        emailUser.setName(request.name().trim());
        emailUser.setEmail(email);
        emailUser.setPassword(encodedPassword);
        emailUser.setRole(normalizedRole);
        emailUser.setProvider(PROVIDER_LOCAL);
        emailUserRepository.save(emailUser);

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setRole(normalizedRole);
        user.setProvider(PROVIDER_LOCAL);
        user.setVerified(false);
        User savedUser = userRepository.save(user);

        return new AuthResponse("User registered successfully.", generateToken(savedUser), toUserResponse(savedUser));
    }

    public AuthResponse login(AuthRequest request) {
        String email = normalizeEmail(request.email());
        EmailUser emailUser = resolveAuthUser(email);

        if (!isPasswordValid(request.password().trim(), emailUser.getPassword())) {
            throw new BadRequestException("Invalid email or password.");
        }

        User user = syncDomainUser(emailUser);
        return new AuthResponse("Login successful.", generateToken(user), toUserResponse(user));
    }

    @Transactional
    public void requestPasswordResetOtp(ForgotPasswordOtpRequest request) {
        String email = normalizeEmail(request.email());
        EmailUser emailUser = resolveAuthUser(email);

        String otp = generateOtp();
        LocalDateTime otpExpiry = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
        emailUser.setOtpCode(otp);
        emailUser.setOtpExpiresAt(otpExpiry);
        emailUser.setOtpVerifiedUntil(null);
        emailUserRepository.save(emailUser);

        User user = syncDomainUser(emailUser);
        user.setOtp(otp);
        user.setOtpExpiry(otpExpiry);
        userRepository.save(user);

        boolean emailDelivered = emailService.sendOtpEmail(email, otp);
        if (!emailDelivered) {
            LOGGER.warn("OTP generated for {} but the email could not be delivered.", email);
        }
    }

    @Transactional
    public ApiMessageResponse verifyPasswordResetOtp(VerifyOtpRequest request) {
        String email = normalizeEmail(request.email());
        String submittedOtp = request.otp().trim();
        EmailUser emailUser = resolveAuthUser(email);
        User user = syncDomainUser(emailUser);

        if (emailUser.getOtpCode() == null || emailUser.getOtpExpiresAt() == null) {
            if (user.getOtp() == null) {
                throw new BadRequestException("OTP already used. Please request a new OTP.");
            }
            throw new BadRequestException("OTP not found. Please request a new OTP.");
        }

        if (emailUser.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            clearOtpFields(emailUser);
            emailUserRepository.save(emailUser);
            clearUserOtpFields(user);
            userRepository.save(user);
            throw new BadRequestException("OTP expired. Please request a new OTP.");
        }

        if (!emailUser.getOtpCode().equals(submittedOtp)) {
            throw new BadRequestException("Invalid OTP. Please try again.");
        }

        LocalDateTime otpVerifiedUntil = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
        clearOtpFields(emailUser);
        emailUser.setOtpVerifiedUntil(otpVerifiedUntil);
        emailUserRepository.saveAndFlush(emailUser);
        user.setVerified(true);
        clearUserOtpFields(user);
        userRepository.saveAndFlush(user);
        return new ApiMessageResponse("OTP verified successfully. You can reset your password now.");
    }

    @Transactional
    public ApiMessageResponse resetPasswordWithOtp(ResetPasswordRequest request) {
        String email = normalizeEmail(request.email());
        EmailUser emailUser = resolveAuthUser(email);
        if (emailUser.getOtpVerifiedUntil() == null || emailUser.getOtpVerifiedUntil().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP verification required before resetting password.");
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        emailUser.setPassword(encodedPassword);
        clearOtpFields(emailUser);
        emailUserRepository.save(emailUser);

        userRepository.findByEmail(email).ifPresent(user -> {
            user.setPassword(encodedPassword);
            clearUserOtpFields(user);
            userRepository.save(user);
        });

        return new ApiMessageResponse("Password reset successfully. Please log in with your new password.");
    }

    @Transactional
    public AuthResponse completeGoogleLogin(String name, String email, String requestedRole) {
        String normalizedEmail = normalizeEmail(email);
        if (!normalizedEmail.endsWith("@gmail.com")) {
            throw new BadRequestException("Only Gmail accounts are allowed for Google login.");
        }

        EmailUser emailUser = emailUserRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> userRepository.findByEmail(normalizedEmail)
                        .map(this::syncEmailStoreFromLegacyUser)
                        .orElseGet(() -> createEmailUser(name, normalizedEmail, normalizeRoleOrDefault(requestedRole), PROVIDER_GOOGLE)));

        emailUser.setName(name == null || name.isBlank() ? emailUser.getName() : name.trim());
        emailUser.setProvider(PROVIDER_GOOGLE);
        emailUserRepository.save(emailUser);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> createDomainUserFromEmailUser(emailUser));

        user.setName(emailUser.getName());
        user.setRole(user.getRole() == null || user.getRole().isBlank() ? emailUser.getRole() : user.getRole());
        user.setProvider(PROVIDER_GOOGLE);
        user.setVerified(true);
        User savedUser = userRepository.save(user);

        if (!savedUser.getRole().equals(emailUser.getRole())) {
            emailUser.setRole(savedUser.getRole());
            emailUserRepository.save(emailUser);
        }

        return new AuthResponse("Google login successful.", generateToken(savedUser), toUserResponse(savedUser));
    }

    public AuthResponse googleLogin(GoogleAuthRequest request) {
        return completeGoogleLogin(request.name(), request.email(), request.role());
    }

    public User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    public List<UserResponse> getUsersByRole(String role) {
        String normalizedRole = normalizeRoleOrDefault(role);

        return userRepository.findAll()
                .stream()
                .filter(user -> normalizedRole.equalsIgnoreCase(user.getRole()))
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserEntity(userId);

        List<Course> createdCourses = courseRepository.findByCreatedById(userId);
        for (Course course : createdCourses) {
            videoLinkService.deleteByCourseId(course.getId());
        }

        videoProgressRepository.deleteByUserId(userId);
        enrollmentRepository.deleteByUserId(userId);
        emailUserRepository.findByEmail(user.getEmail()).ifPresent(emailUserRepository::delete);
        userRepository.delete(user);
    }

    @Transactional
    public User getOrCreateInstructor(String name) {
        String email = name.toLowerCase().replace(" ", ".") + "@lms.local";
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User nextUser = new User();
                    nextUser.setName(name);
                    nextUser.setEmail(email);
                    nextUser.setPassword(passwordEncoder.encode("ChangeMe@123"));
                    nextUser.setRole("INSTRUCTOR");
                    nextUser.setProvider(PROVIDER_SYSTEM);
                    nextUser.setVerified(true);
                    return userRepository.save(nextUser);
                });

        emailUserRepository.findByEmail(email).orElseGet(() -> {
            EmailUser emailUser = new EmailUser();
            emailUser.setName(user.getName());
            emailUser.setEmail(user.getEmail());
            emailUser.setPassword(user.getPassword());
            emailUser.setRole(user.getRole());
            emailUser.setProvider(user.getProvider() == null || user.getProvider().isBlank() ? PROVIDER_SYSTEM : user.getProvider());
            return emailUserRepository.save(emailUser);
        });

        return user;
    }

    public void cleanupExpiredOtps() {
        List<EmailUser> users = emailUserRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (EmailUser emailUser : users) {
          boolean changed = false;

          if (emailUser.getOtpExpiresAt() != null && emailUser.getOtpExpiresAt().isBefore(now)) {
              emailUser.setOtpCode(null);
              emailUser.setOtpExpiresAt(null);
              changed = true;
              userRepository.findByEmail(emailUser.getEmail()).ifPresent(user -> {
                  clearUserOtpFields(user);
                  userRepository.save(user);
              });
          }

          if (emailUser.getOtpVerifiedUntil() != null && emailUser.getOtpVerifiedUntil().isBefore(now)) {
              emailUser.setOtpVerifiedUntil(null);
              changed = true;
          }

          if (changed) {
              emailUserRepository.save(emailUser);
          }
        }
    }

    private EmailUser resolveAuthUser(String email) {
        return emailUserRepository.findByEmail(email)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(this::syncEmailStoreFromLegacyUser)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email)));
    }

    private EmailUser syncEmailStoreFromLegacyUser(User user) {
        EmailUser emailUser = new EmailUser();
        emailUser.setName(user.getName());
        emailUser.setEmail(user.getEmail());
        emailUser.setPassword(user.getPassword());
        emailUser.setRole(user.getRole());
        emailUser.setProvider(user.getProvider() == null || user.getProvider().isBlank() ? PROVIDER_LOCAL : user.getProvider());
        emailUser.setOtpCode(user.getOtp());
        emailUser.setOtpExpiresAt(user.getOtpExpiry());
        return emailUserRepository.save(emailUser);
    }

    private User syncDomainUser(EmailUser emailUser) {
        return userRepository.findByEmail(emailUser.getEmail())
                .map(existingUser -> {
                    existingUser.setName(emailUser.getName());
                    existingUser.setRole(emailUser.getRole());
                    existingUser.setPassword(emailUser.getPassword());
                    existingUser.setProvider(emailUser.getProvider());
                    if (existingUser.getOtp() == null && emailUser.getOtpCode() != null) {
                        existingUser.setOtp(emailUser.getOtpCode());
                    }
                    if (existingUser.getOtpExpiry() == null && emailUser.getOtpExpiresAt() != null) {
                        existingUser.setOtpExpiry(emailUser.getOtpExpiresAt());
                    }
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> createDomainUserFromEmailUser(emailUser));
    }

    private User createDomainUserFromEmailUser(EmailUser emailUser) {
        User user = new User();
        user.setName(emailUser.getName());
        user.setEmail(emailUser.getEmail());
        user.setPassword(emailUser.getPassword());
        user.setRole(emailUser.getRole());
        user.setProvider(emailUser.getProvider());
        user.setOtp(emailUser.getOtpCode());
        user.setOtpExpiry(emailUser.getOtpExpiresAt());
        return userRepository.save(user);
    }

    private EmailUser createEmailUser(String name, String email, String role, String provider) {
        EmailUser emailUser = new EmailUser();
        emailUser.setName(name == null || name.isBlank() ? email.split("@")[0] : name.trim());
        emailUser.setEmail(email);
        emailUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        emailUser.setRole(role);
        emailUser.setProvider(provider);
        return emailUserRepository.save(emailUser);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeRole(String role) {
        return role.trim().toUpperCase();
    }

    private String normalizeRoleOrDefault(String role) {
        if (role == null || role.isBlank()) {
            return "STUDENT";
        }
        return normalizeRole(role);
    }

    private String generateOtp() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private void clearOtpFields(EmailUser emailUser) {
        emailUser.setOtpCode(null);
        emailUser.setOtpExpiresAt(null);
        emailUser.setOtpVerifiedUntil(null);
    }

    private void clearUserOtpFields(User user) {
        user.setOtp(null);
        user.setOtpExpiry(null);
    }

    private String generateToken(User user) {
        return jwtUtil.generateToken(user.getEmail());
    }

    private boolean isPasswordValid(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        return storedPassword.equals(rawPassword);
    }

    private UserResponse toUserResponse(User user) {
        return modelMapper.map(user, UserResponse.class);
    }
}
