package org.example.portal.config;
import lombok.RequiredArgsConstructor;
import org.example.portal.service.GroupwareService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
@Configuration @EnableScheduling @RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="portal.task-reminders.enabled",havingValue="true",matchIfMissing=true)
public class TaskReminders {
    private final GroupwareService groupware;
    @Scheduled(initialDelay=60000,fixedDelay=3600000) public void remind(){groupware.remindTasks();}
}
