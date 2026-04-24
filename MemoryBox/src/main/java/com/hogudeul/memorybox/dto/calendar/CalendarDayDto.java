package com.hogudeul.memorybox.dto.calendar;

import java.util.ArrayList;
import java.util.List;

public class CalendarDayDto {
    private String date;
    private int dayNumber;
    private boolean currentMonth;
    private boolean today;
    private boolean sunday;
    private boolean holiday;
    private boolean hasPersonalEvent;
    private List<CalendarEventDto> events = new ArrayList<>();

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public boolean isCurrentMonth() {
        return currentMonth;
    }

    public void setCurrentMonth(boolean currentMonth) {
        this.currentMonth = currentMonth;
    }

    public boolean isToday() {
        return today;
    }

    public void setToday(boolean today) {
        this.today = today;
    }

    public boolean isSunday() {
        return sunday;
    }

    public void setSunday(boolean sunday) {
        this.sunday = sunday;
    }

    public boolean isHoliday() {
        return holiday;
    }

    public void setHoliday(boolean holiday) {
        this.holiday = holiday;
    }

    public boolean isHasPersonalEvent() {
        return hasPersonalEvent;
    }

    public void setHasPersonalEvent(boolean hasPersonalEvent) {
        this.hasPersonalEvent = hasPersonalEvent;
    }

    public List<CalendarEventDto> getEvents() {
        return events;
    }

    public void setEvents(List<CalendarEventDto> events) {
        this.events = events;
    }
}
