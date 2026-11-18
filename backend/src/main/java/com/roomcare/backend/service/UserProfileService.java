package com.roomcare.backend.service;

import com.roomcare.backend.dto.user.ChangePasswordRequest;
import com.roomcare.backend.dto.user.ProfileResponse;
import com.roomcare.backend.dto.user.UpdateProfileRequest;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public ProfileResponse get(User user) {
        return new ProfileResponse(user.getFullName(), user.getEmail(), user.getPhone(), user.getRole());
    }

    @Transactional
    public ProfileResponse update(User principal, UpdateProfileRequest request) {
        User user = findUser(principal);
        if (users.findByPhone(request.phone()).filter(other -> !other.getId().equals(user.getId())).isPresent()) {
            throw new RuntimeException("Số điện thoại đã được sử dụng");
        }
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone());
        try {
            users.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new RuntimeException("Số điện thoại đã được sử dụng", exception);
        }
        return get(user);
    }

    @Transactional
    public void changePassword(User principal, ChangePasswordRequest request) {
        User user = findUser(principal);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RuntimeException("Mật khẩu mới tối đa 72 byte");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    private User findUser(User principal) {
        return users.findById(principal.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
    }
}
