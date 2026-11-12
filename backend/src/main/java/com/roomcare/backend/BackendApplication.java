package com.roomcare.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application class for RoomCare Backend
 * 
 * @author RoomCare Team
 * @version 1.0.0
 */
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("RoomCare Backend is running on port 8080");
        System.out.println("========================================\n");
    }
}
