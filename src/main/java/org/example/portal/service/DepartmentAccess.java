package org.example.portal.service;

import lombok.RequiredArgsConstructor;
import org.example.portal.domain.Employee;
import org.example.portal.repository.EmployeeRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

@Service @RequiredArgsConstructor
public class DepartmentAccess {
    private final EmployeeRepository employees;
    public boolean isManager(Employee e) { return e!=null && !e.isDeleted() && e.isActive() && e.getRole()==Employee.Role.EMPLOYEE && e.isDepartmentManager(); }
    public boolean sameDepartment(Employee manager,Employee employee) {
        return isManager(manager) && employee.getRole()==Employee.Role.EMPLOYEE && Objects.equals(manager.getDepartment(),employee.getDepartment());
    }
    public Employee manager(String login) {
        var e=employees.findByLoginId(login).orElseThrow(()->new AccessDeniedException("계정을 확인하세요."));
        if(!isManager(e)) throw new AccessDeniedException("부서 관리 권한이 필요합니다.");
        return e;
    }
    public void requireMember(Employee manager,Employee employee) {
        if(!sameDepartment(manager,employee)) throw new AccessDeniedException("소속 부서 직원만 관리할 수 있습니다.");
    }
    public boolean eligibleApprover(Employee approver,Employee applicant) {
        return !applicant.isDeleted() && sameDepartment(approver,applicant) && !approver.getId().equals(applicant.getId());
    }
    public List<Employee> approvers(Employee applicant) {
        return employees.findByDepartmentAndRoleOrderByNameAsc(applicant.getDepartment(),Employee.Role.EMPLOYEE).stream().filter(e->eligibleApprover(e,applicant)).toList();
    }
    public boolean canReview(Employee manager,Employee applicant,Employee selected) {
        return eligibleApprover(manager,applicant) && (!eligibleApprover(selected,applicant) || selected.getId().equals(manager.getId()) || (manager.isDivisionHead() && !selected.isDivisionHead()));
    }
    public void requireReview(Employee manager,Employee applicant,Employee selected) {
        if(!canReview(manager,applicant,selected)) throw new AccessDeniedException("본인 신청을 제외한 소속 부서의 지정 결재 건만 처리할 수 있습니다.");
    }
}
