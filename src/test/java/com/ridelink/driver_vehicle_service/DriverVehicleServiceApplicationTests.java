package com.ridelink.driver_vehicle_service;

import com.ridelink.driver_vehicle_service.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DriverVehicleServiceApplicationTests {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void contextLoads() {
    }

    @Test
    void printValidToken() {
        String token = jwtUtil.generateToken("USR-1002", "DRIVER");
        System.out.println("\n==================================================");
        System.out.println(">>> COPY THIS VALID JWT TOKEN FOR SWAGGER / POSTMAN <<<");
        System.out.println(token);
        System.out.println("==================================================\n");
    }
}
