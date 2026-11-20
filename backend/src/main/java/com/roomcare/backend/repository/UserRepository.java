package com.roomcare.backend.repository;

import com.roomcare.backend.entity.User;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User Repository - Data Access Layer cho User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Tìm user theo email
     */
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Tìm user theo phone
     */
    Optional<User> findByPhone(String phone);

    Optional<User> findFirstByPhoneIn(Collection<String> phones);

    /**
     * Tìm user theo email hoặc phone (cho login)
     */
    Optional<User> findByEmailOrPhone(String email, String phone);

    /**
     * Kiểm tra email đã tồn tại chưa
     */
    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Kiểm tra phone đã tồn tại chưa
     */
    boolean existsByPhone(String phone);

    boolean existsByPhoneIn(Collection<String> phones);
}
