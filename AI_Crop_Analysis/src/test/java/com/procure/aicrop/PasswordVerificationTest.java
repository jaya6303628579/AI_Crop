package com.procure.aicrop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PasswordVerificationTest {

    @Test
    public void verifyPasswordMatch() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        String storedHash = "$2a$10$.z1qcwOcIWOs3bOkgBQDl.gv8RSXlAMWoQfMi5VjXe5AAoZvL4ite";
        String enteredPassword = "J123456789";

        boolean matches = encoder.matches(enteredPassword, storedHash);

        System.out.println("\n========================================");
        System.out.println("PASSWORD VERIFICATION TEST");
        System.out.println("========================================");
        System.out.println("Entered Password: " + enteredPassword);
        System.out.println("Database Hash: " + storedHash);
        System.out.println("Match Result: " + matches);
        System.out.println("========================================\n");

        if (matches) {
            System.out.println("[PASS] PASSWORD IS CORRECT!");
            System.out.println("The password 'J123456789' matches the hash.");
            System.out.println("The issue is NOT with password validation.");
            assertTrue(matches, "Password should match the hash");
        } else {
            System.out.println("[FAIL] PASSWORD IS INCORRECT!");
            System.out.println("The password 'J123456789' does NOT match the hash.");
            System.out.println("You need to enter the CORRECT original password.");
            fail("Password does not match the hash in database");
        }
        System.out.println("========================================\n");
    }
}
