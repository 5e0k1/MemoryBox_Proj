package com.hogudeul.memorybox.dto.calendar;

import java.util.ArrayList;
import java.util.List;

public class CalendarMonthDto {
    private int year;
    private int month;
    private List<CalendarDayDto> days = new ArrayList<>();
    private List<CalendarEventDto> upcomingEvents = new ArrayList<>();

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public List<CalendarDayDto> getDays() {
        return days;
    }

    public void setDays(List<CalendarDayDto> days) {
        this.days = days;
    }

    public List<CalendarEventDto> getUpcomingEvents() {
        return upcomingEvents;
    }

    public void setUpcomingEvents(List<CalendarEventDto> upcomingEvents) {
        this.upcomingEvents = upcomingEvents;
    }
}
