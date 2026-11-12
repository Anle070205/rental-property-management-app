package com.roomcare.backend.auth;

import com.roomcare.backend.dto.AuthResponse;
import com.roomcare.backend.dto.LoginRequest;
import com.roomcare.backend.dto.RegisterRequest;
import com.roomcare.backend.entity.Role;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.repository.UserRepository;
import com.roomcare.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service - Business logic cho authentication
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Đăng ký user mới
     * CHỈ CHO PHÉP ĐĂNG KÝ LANDLORD
     * TENANT được tạo bởi Landlord khi tạo hợp đồng
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Chỉ cho phép đăng ký LANDLORD
        if (!"LANDLORD".equals(request.getRole())) {
            throw new RuntimeException("Chỉ Chủ nhà mới có thể đăng ký tài khoản. Tài khoản Người thuê sẽ được tạo bởi Chủ nhà.");
        }

        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        // Kiểm tra phone đã tồn tại
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Số điện thoại đã được sử dụng");
        }

        // Tạo user mới (LANDLORD only)
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.LANDLORD);
        user.setIsActive(true);

        // Lưu vào database
        User savedUser = userRepository.save(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        // Return response
        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }

    /**
     * Đăng nhập
     */
    public AuthResponse login(LoginRequest request) {
        // Tìm user theo email hoặc phone
        User user = userRepository.findByEmailOrPhone(
                request.getEmailOrPhone(),
                request.getEmailOrPhone()
        ).orElseThrow(() -> new RuntimeException("Email/Số điện thoại hoặc mật khẩu không đúng"));

        // Kiểm tra password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Email/Số điện thoại hoặc mật khẩu không đúng");
        }

        // Kiểm tra account active
        if (!user.getIsActive()) {
            throw new RuntimeException("Tài khoản đã bị khóa");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        // Return response
        return new AuthResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
