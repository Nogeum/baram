package org.example.portal.web;

import org.example.portal.domain.Employee;
import org.example.portal.domain.Notification;
import org.springframework.stereotype.Component;

@Component("notificationLinks")
public class NotificationLinks {
    public String destination(Notification notification, Employee viewer) {
        if(notification.getTargetPath()!=null && notification.getTargetPath().matches("/groupware/[a-z-]+(/[0-9]+)?")) return notification.getTargetPath();
        if (viewer.getRole() == Employee.Role.ADMIN) return "/admin/notification";
        String message = notification.getMessage();
        if (message.startsWith("새 일정이 배정되었습니다: ")) return "/employee/schedule";
        if (message.startsWith("휴가 신청이 ") || message.startsWith("초과근무 신청이 ")
                || message.startsWith("출퇴근 정정 신청이 ")) return "/employee/request-status";
        if (viewer.isDepartmentManager() && (message.endsWith(" 님이 휴가를 신청했습니다.")
                || message.matches("(?s).* 님이 (초과근무를|출퇴근 정정을) 신청했습니다\\. \\(\\d{4}-\\d{2}-\\d{2}\\)$"))) {
            return "/manager/approval";
        }
        return "/employee/main";
    }
}
