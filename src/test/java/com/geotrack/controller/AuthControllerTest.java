package com.geotrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geotrack.dto.auth.LoginRequest;
import com.geotrack.dto.auth.RefreshTokenRequest;
import com.geotrack.dto.auth.RegisterRequest;
import com.geotrack.entity.Employee;
import com.geotrack.entity.Organization;
import com.geotrack.entity.RefreshToken;
import com.geotrack.entity.enums.Role;
import com.geotrack.repository.EmployeeRepository;
import com.geotrack.repository.OrganizationRepository;
import com.geotrack.repository.RefreshTokenRepository;
import com.geotrack.security.CustomUserDetails;
import com.geotrack.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Organization testOrg;
    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        employeeRepository.deleteAll();
        organizationRepository.deleteAll();

        testOrg = new Organization();
        testOrg.setName("Test Org");
        testOrg.setEmail("org@test.com");
        testOrg = organizationRepository.save(testOrg);

        testEmployee = new Employee();
        testEmployee.setFirstName("John");
        testEmployee.setLastName("Doe");
        testEmployee.setEmail("john.doe@test.com");
        testEmployee.setPassword(passwordEncoder.encode("password123"));
        testEmployee.setRole(Role.EMPLOYEE);
        testEmployee.setOrganization(testOrg);
        testEmployee = employeeRepository.save(testEmployee);
    }

    @Test
    void testSuccessfulRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setEmail("jane.smith@test.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane.smith@test.com"));
    }

    @Test
    void testDuplicateRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@test.com"); // Already exists
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSuccessfulLogin() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("john.doe@test.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void testWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("john.doe@test.com");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testNonexistentUser() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("nobody@test.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRefreshToken() throws Exception {
        RefreshToken token = new RefreshToken();
        token.setEmployee(testEmployee);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        token.setRevoked(false);
        refreshTokenRepository.save(token);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(token.getToken());

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
                
        assertFalse(refreshTokenRepository.findByToken(token.getToken()).isPresent(), "Old token should be rotated/deleted");
    }

    @Test
    void testRevokedRefreshToken() throws Exception {
        RefreshToken token = new RefreshToken();
        token.setEmployee(testEmployee);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        token.setRevoked(true); // Revoked
        refreshTokenRepository.save(token);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(token.getToken());

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogout() throws Exception {
        RefreshToken token = new RefreshToken();
        token.setEmployee(testEmployee);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        token.setRevoked(false);
        refreshTokenRepository.save(token);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(token.getToken());

        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertFalse(refreshTokenRepository.findByToken(token.getToken()).isPresent());
    }

    @Test
    void testAuthMe() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(testEmployee);
        String token = jwtService.generateToken(userDetails);

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.doe@test.com"))
                .andExpect(jsonPath("$.firstName").value("John"));
    }
}
