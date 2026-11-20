package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.room.CreateRoomRequest;
import com.roomcare.backend.dto.room.RoomResponse;
import com.roomcare.backend.dto.room.UpdateRoomRequest;
import com.roomcare.backend.entity.RoomStatus;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/landlord/rooms")
@RequiredArgsConstructor
public class RoomController {
    
    private final RoomService roomService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoomResponse> createRoom(
            @AuthenticationPrincipal User landlord,
            @Valid @RequestBody CreateRoomRequest request) {
        RoomResponse response = roomService.createRoom(landlord.getId(), request);
        return ApiResponse.success(response);
    }
    
    @GetMapping
    public ApiResponse<PageResponse<RoomResponse>> getAllRooms(
            @AuthenticationPrincipal User landlord,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PageResponse<RoomResponse> response = roomService.getAllRoomsByLandlord(
            landlord.getId(), page, size, sortBy, sortDir
        );
        return ApiResponse.success(response);
    }
    
    @GetMapping("/property/{propertyId}")
    public ApiResponse<PageResponse<RoomResponse>> getRoomsByProperty(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long propertyId,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer floor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "roomCode") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        PageResponse<RoomResponse> response = roomService.getRoomsByProperty(
            landlord.getId(), propertyId, status, minPrice, maxPrice, floor, 
            page, size, sortBy, sortDir
        );
        return ApiResponse.success(response);
    }
    
    @GetMapping("/{id}")
    public ApiResponse<RoomResponse> getRoomById(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id) {
        RoomResponse response = roomService.getRoomById(landlord.getId(), id);
        return ApiResponse.success(response);
    }
    
    @PutMapping("/{id}")
    public ApiResponse<RoomResponse> updateRoom(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomRequest request) {
        RoomResponse response = roomService.updateRoom(landlord.getId(), id, request);
        return ApiResponse.success(response);
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRoom(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id) {
        roomService.deleteRoom(landlord.getId(), id);
        return ApiResponse.success("Đã xóa phòng thành công");
    }
}
