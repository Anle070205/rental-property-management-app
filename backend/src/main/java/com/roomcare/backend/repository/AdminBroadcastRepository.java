package com.roomcare.backend.repository;

import com.roomcare.backend.entity.AdminBroadcast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminBroadcastRepository extends JpaRepository<AdminBroadcast, Long> {
    List<AdminBroadcast> findAllByOrderByUpdatedAtDescIdDesc();
}
