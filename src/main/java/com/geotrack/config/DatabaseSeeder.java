package com.geotrack.config;

import com.geotrack.entity.*;
import com.geotrack.entity.enums.Role;
import com.geotrack.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Optional;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final ShiftRepository shiftRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final OfficeRepository officeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.super-admin.email:superadmin@geotrack.local}")
    private String superAdminEmail;

    @Value("${app.seed.super-admin.password:SuperAdmin@123}")
    private String superAdminPassword;

    @Override
    public void run(String... args) {
        seedSuperAdmin();
        seedDemoAccounts();
        seedAllOrganizations();
        configureGauthamAccount();
    }

    private void seedAllOrganizations() {
        organizationRepository.findAll().forEach(this::seedDefaultsForOrganization);
    }

    private void configureGauthamAccount() {
        employeeRepository.findByEmail("c.gauthamtm@gmail.com").ifPresent(emp -> {
            emp.setRole(Role.ORG_ADMIN);
            if (emp.getOrganization() != null) {
                seedDefaultsForOrganization(emp.getOrganization());
                departmentRepository.findByOrganizationId(emp.getOrganization().getId()).stream().findFirst().ifPresent(emp::setDepartment);
                shiftRepository.findByOrganizationId(emp.getOrganization().getId()).stream().findFirst().ifPresent(emp::setShift);
                officeRepository.findByOrganizationId(emp.getOrganization().getId()).stream().findFirst().ifPresent(emp::setOffice);
                seedTeamForOrganization(emp.getOrganization());
            }
            employeeRepository.save(emp);
            System.out.println("Configured Gautham as ORG_ADMIN with defaults and team seeded.");
        });
    }

    private void seedTeamForOrganization(Organization org) {
        if (employeeRepository.findByEmail("marcus.manager@geotrack.com").isEmpty()) {
            Department eng = departmentRepository.findByOrganizationId(org.getId()).stream()
                    .filter(d -> "Engineering".equals(d.getName())).findFirst().orElse(null);
            Department ops = departmentRepository.findByOrganizationId(org.getId()).stream()
                    .filter(d -> "Operations".equals(d.getName())).findFirst().orElse(null);
            Department hr = departmentRepository.findByOrganizationId(org.getId()).stream()
                    .filter(d -> "Human Resources".equals(d.getName())).findFirst().orElse(null);
            Shift shift = shiftRepository.findByOrganizationId(org.getId()).stream().findFirst().orElse(null);
            Office office = officeRepository.findByOrganizationId(org.getId()).stream().findFirst().orElse(null);

            Employee marcus = createTeamMember("marcus.manager@geotrack.com", "Marcus", "Vance", "Manager@123", Role.MANAGER, org, eng, shift, office, null);
            Employee sarah = createTeamMember("sarah.ops@geotrack.com", "Sarah", "Jenkins", "Manager@123", Role.MANAGER, org, ops, shift, office, null);
            createTeamMember("alex.dev@geotrack.com", "Alex", "Rivera", "Employee@123", Role.EMPLOYEE, org, eng, shift, office, marcus);
            createTeamMember("priya.dev@geotrack.com", "Priya", "Sharma", "Employee@123", Role.EMPLOYEE, org, eng, shift, office, marcus);
            createTeamMember("david.ops@geotrack.com", "David", "Miller", "Employee@123", Role.EMPLOYEE, org, ops, shift, office, sarah);
            createTeamMember("elena.hr@geotrack.com", "Elena", "Rostova", "Employee@123", Role.EMPLOYEE, org, hr, shift, office, null);
        }
    }

    private Employee createTeamMember(String email, String first, String last, String pwd, Role role, Organization org, Department dept, Shift shift, Office office, Employee manager) {
        if (employeeRepository.findByEmail(email).isPresent()) return employeeRepository.findByEmail(email).get();
        Employee emp = new Employee();
        emp.setEmail(email);
        emp.setFirstName(first);
        emp.setLastName(last);
        emp.setPassword(passwordEncoder.encode(pwd));
        emp.setRole(role);
        emp.setStatus(com.geotrack.entity.enums.EmployeeStatus.ACTIVE);
        emp.setOrganization(org);
        emp.setDepartment(dept);
        emp.setShift(shift);
        emp.setOffice(office);
        emp.setManager(manager);
        return employeeRepository.save(emp);
    }

    private void seedSuperAdmin() {
        Optional<Employee> existingSuperAdmin = employeeRepository.findByEmail(superAdminEmail);
        
        if (existingSuperAdmin.isEmpty()) {
            Organization systemOrg = new Organization();
            systemOrg.setName("GeoTrack System");
            systemOrg.setEmail(superAdminEmail);
            systemOrg = organizationRepository.save(systemOrg);

            seedDefaultsForOrganization(systemOrg);

            Employee superAdmin = new Employee();
            superAdmin.setFirstName("Super");
            superAdmin.setLastName("Admin");
            superAdmin.setEmail(superAdminEmail);
            superAdmin.setPassword(passwordEncoder.encode(superAdminPassword));
            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setOrganization(systemOrg);
            
            employeeRepository.save(superAdmin);
            
            System.out.println("System initialized with SUPER_ADMIN: " + superAdminEmail);
        }
    }

    private void seedDemoAccounts() {
        if (employeeRepository.findByEmail("orgadmin@geotrack.com").isEmpty()) {
            Organization org = new Organization();
            org.setName("GeoTrack Demo Org");
            org.setEmail("admin@geotrack.com");
            org = organizationRepository.save(org);

            seedDefaultsForOrganization(org);

            createEmployee("orgadmin@geotrack.com", "Org", "Admin", Role.ORG_ADMIN, org);
            createEmployee("manager@geotrack.com", "Demo", "Manager", Role.MANAGER, org);
            createEmployee("alice@geotrack.com", "Alice", "Employee", Role.EMPLOYEE, org);

            System.out.println("Development accounts seeded successfully.");
        }
    }

    public void seedDefaultsForOrganization(Organization org) {
        Department engineering = null;
        if (departmentRepository.findByOrganizationId(org.getId()).isEmpty()) {
            engineering = new Department();
            engineering.setOrganization(org);
            engineering.setName("Engineering");
            engineering.setDescription("Software engineering, architecture, and technology");
            engineering = departmentRepository.save(engineering);

            Department hr = new Department();
            hr.setOrganization(org);
            hr.setName("Human Resources");
            hr.setDescription("Talent acquisition, employee success, and culture");
            departmentRepository.save(hr);

            Department sales = new Department();
            sales.setOrganization(org);
            sales.setName("Sales & Marketing");
            sales.setDescription("Business development, client acquisition, and branding");
            departmentRepository.save(sales);

            Department ops = new Department();
            ops.setOrganization(org);
            ops.setName("Operations");
            ops.setDescription("Field operations, administration, and facilities");
            departmentRepository.save(ops);
        }

        Shift generalShift = null;
        if (shiftRepository.findByOrganizationId(org.getId()).isEmpty()) {
            generalShift = new Shift();
            generalShift.setOrganization(org);
            generalShift.setName("General Shift");
            generalShift.setStartTime(LocalTime.of(9, 0));
            generalShift.setEndTime(LocalTime.of(18, 0));
            generalShift.setGracePeriodMinutes(15);
            generalShift = shiftRepository.save(generalShift);

            Shift morningShift = new Shift();
            morningShift.setOrganization(org);
            morningShift.setName("Morning Shift");
            morningShift.setStartTime(LocalTime.of(8, 0));
            morningShift.setEndTime(LocalTime.of(16, 30));
            morningShift.setGracePeriodMinutes(15);
            shiftRepository.save(morningShift);

            Shift eveningShift = new Shift();
            eveningShift.setOrganization(org);
            eveningShift.setName("Evening Shift");
            eveningShift.setStartTime(LocalTime.of(13, 0));
            eveningShift.setEndTime(LocalTime.of(21, 30));
            eveningShift.setGracePeriodMinutes(15);
            shiftRepository.save(eveningShift);
        }

        if (leaveTypeRepository.findByOrganizationId(org.getId()).isEmpty()) {
            LeaveType pto = new LeaveType();
            pto.setOrganization(org);
            pto.setName("Paid Time Off (PTO)");
            pto.setDescription("Annual earned vacation and personal leave");
            pto.setDaysAllowed(18);
            pto.setRequiresApproval(true);
            leaveTypeRepository.save(pto);

            LeaveType sick = new LeaveType();
            sick.setOrganization(org);
            sick.setName("Sick Leave");
            sick.setDescription("Medical and health recovery leave");
            sick.setDaysAllowed(12);
            sick.setRequiresApproval(false);
            leaveTypeRepository.save(sick);

            LeaveType casual = new LeaveType();
            casual.setOrganization(org);
            casual.setName("Casual Leave");
            casual.setDescription("Short-term urgent personal affairs leave");
            casual.setDaysAllowed(10);
            casual.setRequiresApproval(true);
            leaveTypeRepository.save(casual);

            LeaveType compOff = new LeaveType();
            compOff.setOrganization(org);
            compOff.setName("Compensatory Off");
            compOff.setDescription("Credit for approved overtime or weekend coverage");
            compOff.setDaysAllowed(5);
            compOff.setRequiresApproval(true);
            leaveTypeRepository.save(compOff);
        }

        Office hq = null;
        if (officeRepository.findByOrganizationId(org.getId()).isEmpty()) {
            hq = new Office();
            hq.setOrganization(org);
            hq.setName("Corporate Headquarters");
            hq.setAddress("100 Tech Park Avenue, Bengaluru");
            hq.setLatitude(12.9716);
            hq.setLongitude(77.5946);
            hq.setRadiusMeters(250);
            hq.setTimezone("Asia/Kolkata");
            officeRepository.save(hq);
        }
    }

    private void createEmployee(String email, String firstName, String lastName, Role role, Organization org) {
        Employee emp = new Employee();
        emp.setEmail(email);
        emp.setFirstName(firstName);
        emp.setLastName(lastName);
        emp.setPassword(passwordEncoder.encode("password123"));
        emp.setRole(role);
        emp.setStatus(com.geotrack.entity.enums.EmployeeStatus.ACTIVE);
        emp.setOrganization(org);

        departmentRepository.findByOrganizationId(org.getId()).stream().findFirst().ifPresent(emp::setDepartment);
        shiftRepository.findByOrganizationId(org.getId()).stream().findFirst().ifPresent(emp::setShift);
        officeRepository.findByOrganizationId(org.getId()).stream().findFirst().ifPresent(emp::setOffice);

        employeeRepository.save(emp);
    }
}
