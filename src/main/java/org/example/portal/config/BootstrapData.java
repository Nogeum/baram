package org.example.portal.config;
import lombok.RequiredArgsConstructor;
import org.example.portal.domain.Employee;
import org.example.portal.repository.EmployeeRepository;
import org.example.portal.service.PortalService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="portal.bootstrap.enabled", havingValue="true")
public class BootstrapData implements CommandLineRunner {
    private final PortalService service;
    private final EmployeeRepository employees;
    @Value("${portal.bootstrap.admin-password:}") private String adminPassword;
    @Value("${portal.bootstrap.employee-password:}") private String employeePassword;
    @Override @Transactional public void run(String... args) {
        if (employees.count()!=0) return;
        if (adminPassword.isBlank()) throw new IllegalStateException("초기 관리자 비밀번호를 설정하세요.");
        service.createEmployee("admin",adminPassword,"관리자","경영지원팀","관리자",Employee.Role.ADMIN,"","",service.today());
        if(!employeePassword.isBlank()) service.createEmployee("employee",employeePassword,"김직원","개발팀","사원",Employee.Role.EMPLOYEE,"","",service.today());
        service.publish("admin","사내 포탈에 오신 것을 환영합니다","출퇴근 체크와 일정 확인, 휴가 신청을 이곳에서 진행하세요.");
    }
}
