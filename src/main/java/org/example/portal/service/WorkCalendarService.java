package org.example.portal.service;
import lombok.RequiredArgsConstructor;
import org.example.portal.domain.*;
import org.example.portal.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class WorkCalendarService {
    private final CompanyHolidayRepository holidays;
    private final EmployeeRepository employees;
    private final LeaveRequestRepository leaves;
    private final Clock clock;
    public Set<LocalDate> closedDates(){return holidays.findAll().stream().map(CompanyHoliday::getHolidayDate).collect(Collectors.toSet());}
    public boolean workingDay(LocalDate date,Set<LocalDate> closed){return date.getDayOfWeek()!=DayOfWeek.SATURDAY && date.getDayOfWeek()!=DayOfWeek.SUNDAY && !closed.contains(date);}
    public int days(LocalDate start,LocalDate end){var closed=closedDates();return (int)start.datesUntil(end.plusDays(1)).filter(d->workingDay(d,closed)).count();}
    @Transactional public void save(Employee actor,LocalDate date,String name,String kind){
        requireAdmin(actor);
        if(date==null || name==null || name.isBlank() || name.length()>120 || !Set.of("PUBLIC","COMPANY").contains(kind))throw new BusinessException("휴무일 날짜·이름·종류를 확인하세요.");
        employees.lockByRole(Employee.Role.EMPLOYEE);
        if(holidays.findAll().stream().anyMatch(h->h.getHolidayDate().equals(date)))throw new BusinessException("이미 등록된 휴무일입니다.");
        var h=new CompanyHoliday();h.setHolidayDate(date);h.setName(name.trim());h.setKind(kind);h.setAuthor(actor);h.setCreatedAt(LocalDateTime.now(clock));holidays.saveAndFlush(h);
        recalculate();
    }
    @Transactional public void delete(Employee actor,Long id){
        requireAdmin(actor);employees.lockByRole(Employee.Role.EMPLOYEE);
        var h=holidays.lockById(id).orElseThrow(()->new BusinessException("휴무일이 없습니다."));
        holidays.delete(h);holidays.flush();recalculate();
    }
    private void recalculate(){
        var closed=closedDates();
        for(var r:leaves.findAll()){
            if(r.getStatus()!=LeaveRequest.Status.PENDING && r.getStatus()!=LeaveRequest.Status.APPROVED)continue;
            int days=(int)r.getStartDate().datesUntil(r.getEndDate().plusDays(1)).filter(d->workingDay(d,closed)).count();
            r.setChargedUnits(r.getKind()==LeaveRequest.Kind.SICK ? 0 : r.getKind()==LeaveRequest.Kind.ANNUAL ? days*2 : days>0 ? 1 : 0);
        }
    }
    private void requireAdmin(Employee actor){if(actor.getRole()!=Employee.Role.ADMIN)throw new org.springframework.security.access.AccessDeniedException("관리자만 휴무일을 관리할 수 있습니다.");}
}
