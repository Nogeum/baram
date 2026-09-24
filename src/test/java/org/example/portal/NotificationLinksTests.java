package org.example.portal;

import org.example.portal.domain.Employee;
import org.example.portal.domain.Notification;
import org.example.portal.web.NotificationLinks;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationLinksTests {
    private final NotificationLinks links = new NotificationLinks();

    private String destination(String message, boolean manager) {
        var viewer = new Employee();
        viewer.setRole(Employee.Role.EMPLOYEE);
        viewer.setDepartmentManager(manager);
        var notification = new Notification();
        notification.setMessage(message);
        return links.destination(notification, viewer);
    }

    @Test void existingRequestsOpenApprovalAndResultsOpenPersonalHistory() {
        for (String message : new String[]{
                "김직원 님이 휴가를 신청했습니다.",
                "김직원 님이 초과근무를 신청했습니다. (2026-09-24)",
                "김직원 님이 출퇴근 정정을 신청했습니다. (2026-09-24)"}) {
            assertThat(destination(message, true)).isEqualTo("/manager/approval");
            assertThat(destination(message, false)).isEqualTo("/employee/main");
        }
        for (String kind : new String[]{"휴가", "초과근무", "출퇴근 정정"}) {
            for (String result : new String[]{"승인", "반려"}) {
                assertThat(destination(kind + " 신청이 " + result + "되었습니다. (2026-09-24)", true))
                        .isEqualTo("/employee/request-status");
            }
        }
    }

    @Test void scheduleTitleCannotBeMistakenForApprovalRequest() {
        assertThat(destination("새 일정이 배정되었습니다: 김직원 님이 휴가를 신청했습니다.", true))
                .isEqualTo("/employee/schedule");
        assertThat(destination("알 수 없는 이전 알림", false)).isEqualTo("/employee/main");
    }
}
