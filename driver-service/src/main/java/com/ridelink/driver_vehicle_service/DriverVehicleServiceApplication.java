package com.ridelink.driver_vehicle_service;

import com.ridelink.driver_vehicle_service.security.JwtUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DriverVehicleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriverVehicleServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner printToken(JwtUtil jwtUtil) {
        return args -> {
            System.out.println("\n==================================================");
            System.out.println(">>> SWAGGER TEST JWT TOKEN <<<");
            System.out.println(jwtUtil.generateToken("USR-1002", "DRIVER"));
            System.out.println("==================================================\n");
        };
    }
}
