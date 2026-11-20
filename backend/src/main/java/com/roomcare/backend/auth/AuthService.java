package com.roomcare.backend.auth;

import com.roomcare.backend.dto.AuthResponse;
import com.roomcare.backend.dto.LoginRequest;
import com.roomcare.backend.dto.RegisterRequest;
import com.roomcare.backend.entity.Property;
import com.roomcare.backend.entity.Role;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.repository.PropertyRepository;
import com.roomcare.backend.repository.TenantRepository;
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
    private final PropertyRepository propertyRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Đăng ký user mới (hỗ trợ LANDLORD và TENANT)
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String requestedRole = request.getRole() != null && !request.getRole().isBlank()
                ? request.getRole().trim().toUpperCase()
                : "LANDLORD";
        if (!"LANDLORD".equals(requestedRole) && !"TENANT".equals(requestedRole)) {
            throw new RuntimeException("Vai trò không hợp lệ. Chỉ hỗ trợ Chủ nhà hoặc Người thuê.");
        }
        Role role = Role.valueOf(requestedRole);

        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        // Kiểm tra phone đã tồn tại
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Số điện thoại đã được sử dụng");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim());
        user.setPhone(request.getPhone().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        if (role == Role.LANDLORD) {
            Property defaultProperty = new Property();
            defaultProperty.setLandlord(savedUser);
            defaultProperty.setName("Nhà trọ " + savedUser.getFullName());
            defaultProperty.setAddress("Chưa cập nhật địa chỉ");
            defaultProperty.setDescription("Nhà trọ mặc định");
            propertyRepository.save(defaultProperty);
        } else {
            tenantRepository.findFirstByEmailIgnoreCaseOrPhone(savedUser.getEmail(), savedUser.getPhone())
                    .ifPresent(tenant -> {
                        if (tenant.getUser() == null) {
                            tenant.setUser(savedUser);
                            tenantRepository.save(tenant);
                        }
                    });
        }

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
