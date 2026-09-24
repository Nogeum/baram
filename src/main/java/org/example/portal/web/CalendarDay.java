package org.example.portal.web;

import org.example.portal.domain.Schedule;
import java.time.LocalDate;
import java.util.List;

public record CalendarDay(LocalDate date, boolean currentMonth, List<Schedule> events) {}
