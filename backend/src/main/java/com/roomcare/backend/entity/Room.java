package com.roomcare.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "room_code"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;
    
    @Column(name = "room_code", nullable = false, length = 50)
    private String roomCode;
    
    @Column(nullable = false)
    private Integer floor;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal area; // m2
    
    @Column(name = "rent_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal rentPrice;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal deposit;
    
    @Column(nullable = false)
    private Integer capacity; // Số người tối đa
    
    @Column(columnDefinition = "TEXT")
    private String amenities; // Tiện nghi (JSON string hoặc text)
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
