package com.geotrack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geotrack.dto.auth.CreateEmployeeRequest;
import com.geotrack.dto.auth.CreateOrganizationRequest;
import com.geotrack.dto.auth.RegisterRequest;
import com.geotrack.entity.Employee;
import com.geotrack.entity.Organization;
import com.geotrack.entity.enums.Role;
import com.geotrack.repository.EmployeeRepository;
import com.geotrack.repository.OrganizationRepository;
import com.geotrack.security.JwtService;
import com.geotrack.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String superAdminToken;
    private String orgAdminToken;
    private String employeeToken;
    
    private Organization org1;
    private Organization org2;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
        organizationRepository.deleteAll();

        // Setup Org 1
        org1 = new Organization();
        org1.setName("Org 1");
        org1.setEmail("org1@test.com");
        org1 = organizationRepository.save(org1);

        // Setup Org 2
        org2 = new Organization();
        org2.setName("Org 2");
        org2.setEmail("org2@test.com");
        org2 = organizationRepository.save(org2);

        // Setup SUPER_ADMIN
        Employee superAdmin = new Employee();
        superAdmin.setFirstName("Super");
        superAdmin.setLastName("Admin");
        superAdmin.setEmail("super@test.com");
        superAdmin.setPassword(passwordEncoder.encode("password"));
        superAdmin.setRole(Role.SUPER_ADMIN);
        superAdmin.setOrganization(org1);
        superAdmin = employeeRepository.save(superAdmin);
        superAdminToken = "Bearer " + jwtService.generateToken(new CustomUserDetails(superAdmin));

        // Setup ORG_ADMIN
        Employee orgAdmin = new Employee();
        orgAdmin.setFirstName("Org");
        orgAdmin.setLastName("Admin");
        orgAdmin.setEmail("orgadmin@test.com");
        orgAdmin.setPassword(passwordEncoder.encode("password"));
        orgAdmin.setRole(Role.ORG_ADMIN);
        orgAdmin.setOrganization(org1);
        orgAdmin = employeeRepository.save(orgAdmin);
        orgAdminToken = "Bearer " + jwtService.generateToken(new CustomUserDetails(orgAdmin));

        // Setup EMPLOYEE
        Employee employee = new Employee();
        employee.setFirstName("Emp");
        employee.setLastName("Loyee");
        employee.setEmail("emp@test.com");
        employee.setPassword(passwordEncoder.encode("password"));
        employee.setRole(Role.EMPLOYEE);
        employee.setOrganization(org1);
        employee = employeeRepository.save(employee);
        employeeToken = "Bearer " + jwtService.generateToken(new CustomUserDetails(employee));
    }

    @Test
    void testPublicRegistrationCannotCreateAdminsOrChooseOrg() throws Exception {
        // Construct a raw JSON to bypass DTO field restrictions (since they are removed, Jackson ignores them)
        // We will assert that even if passed, the server forces EMPLOYEE and null organization.
        String payload = """
            {
                "firstName": "Hacker",
                "lastName": "Man",
                "email": "hacker@test.com",
                "password": "password123",
                "role": "SUPER_ADMIN",
                "organizationId": "%s"
            }
        """.formatted(org1.getId().toString());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.organizationId").isEmpty());
                
        // Verification constraints 1, 2, 3
        Employee saved = employeeRepository.findByEmail("hacker@test.com").orElseThrow();
        assertEquals(Role.EMPLOYEE, saved.getRole());
        assertNull(saved.getOrganization());
    }

    @Test
    void testEmployeeCannotCreateOrgAdmin() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setFirstName("New");
        request.setLastName("User");
        request.setEmail("new1@test.com");
        request.setPassword("password");
        request.setRole(Role.ORG_ADMIN);
        
        mockMvc.perform(post("/api/employees")
                .header("Authorization", employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // Constraint 4
    }

    @Test
    void testEmployeeCannotCreateSuperAdmin() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setFirstName("New");
        request.setLastName("User");
        request.setEmail("new2@test.com");
        request.setPassword("password");
        request.setRole(Role.SUPER_ADMIN);
        
        mockMvc.perform(post("/api/employees")
                .header("Authorization", employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // Constraint 5
    }

    @Test
    void testOrgAdminCannotCreateSuperAdmin() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setFirstName("New");
        request.setLastName("User");
        request.setEmail("new3@test.com");
        request.setPassword("password");
        request.setRole(Role.SUPER_ADMIN);
        
        mockMvc.perform(post("/api/employees")
                .header("Authorization", orgAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // Constraint 6
    }

    @Test
    void testOrgAdminCanCreateEmployeesOnlyInOwnOrganization() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setFirstName("New");
        request.setLastName("User");
        request.setEmail("new4@test.com");
        request.setPassword("password");
        request.setRole(Role.EMPLOYEE);
        request.setOrganizationId(org2.getId()); // Try to assign to org2
        
        mockMvc.perform(post("/api/employees")
                .header("Authorization", orgAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(org1.getId().toString())); // Forced to org1
                
        // Constraint 7
    }

    @Test
    void testSuperAdminCanCreateOrganizations() throws Exception {
        CreateOrganizationRequest request = new CreateOrganizationRequest();
        request.setName("New Org");
        request.setEmail("neworg@test.com");
        
        mockMvc.perform(post("/api/organizations")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()); // Constraint 8
    }
    
    @Test
    void testOrgAdminCannotCreateOrganizations() throws Exception {
        CreateOrganizationRequest request = new CreateOrganizationRequest();
        request.setName("New Org");
        request.setEmail("neworg@test.com");
        
        mockMvc.perform(post("/api/organizations")
                .header("Authorization", orgAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSuperAdminCanCreateOrgAdmin() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setFirstName("New");
        request.setLastName("Admin");
        request.setEmail("newadmin@test.com");
        request.setPassword("password");
        request.setRole(Role.ORG_ADMIN);
        request.setOrganizationId(org2.getId());
        
        mockMvc.perform(post("/api/employees")
                .header("Authorization", superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ORG_ADMIN"))
                .andExpect(jsonPath("$.organizationId").value(org2.getId().toString())); // Constraint 9
    }
}
