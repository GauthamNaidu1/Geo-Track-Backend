package com.geotrack.service;

import com.geotrack.dto.auth.*;
import com.geotrack.entity.Employee;
import com.geotrack.entity.Organization;
import com.geotrack.entity.RefreshToken;
import com.geotrack.repository.EmployeeRepository;
import com.geotrack.repository.OrganizationRepository;
import com.geotrack.repository.RefreshTokenRepository;
import com.geotrack.security.CustomUserDetails;
import com.geotrack.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (employeeRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        Organization org = null;
        if (request.getOrganizationName() != null && !request.getOrganizationName().trim().isEmpty()) {
            org = new Organization();
            org.setName(request.getOrganizationName());
            org.setEmail(request.getEmail());
            org = organizationRepository.save(org);
        }

        Employee employee = new Employee();
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPassword(passwordEncoder.encode(request.getPassword()));
        employee.setOrganization(org);
        
        if (request.getRole() != null) {
            employee.setRole(request.getRole());
        } else if (org != null) {
            employee.setRole(com.geotrack.entity.enums.Role.ORG_ADMIN);
        } else {
            employee.setRole(com.geotrack.entity.enums.Role.EMPLOYEE);
        }
        Employee saved = employeeRepository.save(employee);
        return mapToUserResponse(saved);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Employee employee = employeeRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        CustomUserDetails userDetails = new CustomUserDetails(employee);
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = createRefreshToken(employee);

        return new LoginResponse("Login successful", mapToUserResponse(employee), accessToken, refreshToken.getToken(), 900); // 15 mins
    }

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (refreshToken.getRevoked() || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("Refresh token was expired or revoked");
        }

        Employee employee = refreshToken.getEmployee();
        CustomUserDetails userDetails = new CustomUserDetails(employee);
        
        String accessToken = jwtService.generateToken(userDetails);
        
        refreshTokenRepository.delete(refreshToken); // rotate
        RefreshToken newRefreshToken = createRefreshToken(employee);

        return new LoginResponse("Token refreshed", mapToUserResponse(employee), accessToken, newRefreshToken.getToken(), 900);
    }

    @Transactional
    public void logout(String token) {
        if (token != null) {
            refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
        }
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), employee.getPassword())) {
            throw new IllegalArgumentException("Incorrect current password");
        }
        employee.setPassword(passwordEncoder.encode(request.getNewPassword()));
        employeeRepository.save(employee);
    }

    public UserResponse getCurrentUser(String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return mapToUserResponse(employee);
    }

    private RefreshToken createRefreshToken(Employee employee) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setEmployee(employee);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        return refreshTokenRepository.save(refreshToken);
    }

    private UserResponse mapToUserResponse(Employee employee) {
        return UserResponse.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .name(employee.getFirstName() + (employee.getLastName() != null ? " " + employee.getLastName() : ""))
                .email(employee.getEmail())
                .organizationId(employee.getOrganization() != null ? employee.getOrganization().getId() : null)
                .role(employee.getRole())
                .employeeCode("EMP-" + employee.getId().toString().substring(0, 6).toUpperCase())
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .officeName(employee.getOffice() != null ? employee.getOffice().getName() : null)
                .build();
    }
}
