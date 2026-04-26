package com.hogudeul.memorybox.calendar;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.property.DateEnd;
import biweekly.property.DateStart;
import biweekly.property.Summary;
import com.hogudeul.memorybox.config.AppProperties;
import com.hogudeul.memorybox.dto.calendar.CalendarDayDto;
import com.hogudeul.memorybox.dto.calendar.CalendarEventDto;
import com.hogudeul.memorybox.dto.calendar.CalendarMonthDto;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CalendarViewService {

    private static final Logger log = LoggerFactory.getLogger(CalendarViewService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final ZoneId CALENDAR_ZONE = ZoneId.of("Asia/Seoul");

    private final AppProperties appProperties;
    private final HttpClient httpClient;
    private final Clock clock;

    private volatile CacheEntry cacheEntry;

    public CalendarViewService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.clock = Clock.systemDefaultZone();
    }

    public CalendarLoadResult loadCalendarMonth(YearMonth targetMonth) {
        AppProperties.Calendar calendarProperties = appProperties.getCalendar();
        if (!calendarProperties.isEnabled()) {
            return CalendarLoadResult.disabled();
        }
        List<AppProperties.CalendarSource> validSources = calendarProperties.getSources().stream()
                .filter(source -> StringUtils.hasText(source.getUrl()))
                .collect(Collectors.toList());
        if (validSources.isEmpty()) {
            return CalendarLoadResult.noSources();
        }

        try {
            List<RawCalendarEvent> mergedEvents = getCachedOrFetch(validSources);
            CalendarMonthDto monthDto = buildMonthDto(targetMonth, mergedEvents);
            return CalendarLoadResult.ready(monthDto);
        } catch (Exception e) {
            log.error("캘린더 월 데이터 생성 실패: {}", targetMonth, e);
            return CalendarLoadResult.error();
        }
    }

    private List<RawCalendarEvent> getCachedOrFetch(List<AppProperties.CalendarSource> sources) {
        CacheEntry existing = cacheEntry;
        Instant now = clock.instant();
        if (existing != null && now.isBefore(existing.cachedAt.plus(CACHE_TTL))) {
            return existing.events;
        }

        synchronized (this) {
            CacheEntry latest = cacheEntry;
            Instant syncedNow = clock.instant();
            if (latest != null && syncedNow.isBefore(latest.cachedAt.plus(CACHE_TTL))) {
                return latest.events;
            }
            List<RawCalendarEvent> fetched = fetchAllSources(sources);
            cacheEntry = new CacheEntry(syncedNow, fetched);
            return fetched;
        }
    }

    private List<RawCalendarEvent> fetchAllSources(List<AppProperties.CalendarSource> sources) {
        List<RawCalendarEvent> merged = new ArrayList<>();
        for (AppProperties.CalendarSource source : sources) {
            try {
                merged.addAll(fetchSourceEvents(source));
            } catch (Exception e) {
                log.warn("ICS source 읽기 실패 - name: {}, type: {}, reason: {}",
                        source.getName(), source.getType(), e.getMessage());
            }
        }
        return dedupeAndSort(merged);
    }

    private List<RawCalendarEvent> fetchSourceEvents(AppProperties.CalendarSource source) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(source.getUrl()))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "text/calendar,text/plain;q=0.9,*/*;q=0.8")
                .header("User-Agent", "MemoryBox/1.0 (+calendar-fetch)")
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode());
        }

        List<ICalendar> calendars = Biweekly.parse(new ByteArrayInputStream(response.body())).all();
        List<RawCalendarEvent> events = new ArrayList<>();
        for (ICalendar calendar : calendars) {
            for (VEvent event : calendar.getEvents()) {
                RawCalendarEvent raw = parseEvent(source, event);
                if (raw != null) {
                    events.add(raw);
                }
            }
        }
        log.info("ICS source 로드 완료 - name: {}, type: {}, eventCount: {}", source.getName(), source.getType(), events.size());
        return events;
    }

    private RawCalendarEvent parseEvent(AppProperties.CalendarSource source, VEvent event) {
        Summary summary = event.getSummary();
        DateStart dateStart = event.getDateStart();
        if (dateStart == null || dateStart.getValue() == null) {
            return null;
        }

        String title = StringUtils.hasText(summary != null ? summary.getValue() : null)
                ? summary.getValue().trim() : "(제목 없음)";

        ZoneId zone = CALENDAR_ZONE;
        boolean allDay = !dateStart.getValue().hasTime();
        LocalDateTime startDateTime = allDay
                ? LocalDateTime.ofInstant(dateStart.getValue().toInstant(), zone).toLocalDate().atStartOfDay()
                : LocalDateTime.ofInstant(dateStart.getValue().toInstant(), zone);

        DateEnd dateEnd = event.getDateEnd();
        LocalDateTime endDateTime = null;
        if (dateEnd != null && dateEnd.getValue() != null) {
            endDateTime = dateEnd.getValue().hasTime()
                    ? LocalDateTime.ofInstant(dateEnd.getValue().toInstant(), zone)
                    : LocalDateTime.ofInstant(dateEnd.getValue().toInstant(), zone).toLocalDate().atStartOfDay();
        }

        String sourceName = StringUtils.hasText(source.getName()) ? source.getName().trim() : "외부 캘린더";
        String sourceType = StringUtils.hasText(source.getType()) ? source.getType().trim().toUpperCase(Locale.ROOT) : "PERSONAL";

        return new RawCalendarEvent(title, startDateTime, endDateTime, allDay, sourceName, sourceType);
    }

    private List<RawCalendarEvent> dedupeAndSort(List<RawCalendarEvent> rawEvents) {
        Set<String> dedupeKey = new HashSet<>();
        List<RawCalendarEvent> deduped = new ArrayList<>();
        for (RawCalendarEvent event : rawEvents) {
            CalendarEventDto dto = toEventDto(event);
            String key = dto.getDate() + "|" + dto.getTitle() + "|" + dto.getTimeText();
            if (dedupeKey.add(key)) {
                deduped.add(event);
            }
        }

        deduped.sort(Comparator
                .comparing((RawCalendarEvent event) -> event.start.toLocalDate())
                .thenComparing(event -> event.allDay ? LocalDateTime.of(event.start.toLocalDate(), java.time.LocalTime.MIN) : event.start)
                .thenComparing(event -> event.title));
        return deduped;
    }

    private CalendarMonthDto buildMonthDto(YearMonth targetMonth, List<RawCalendarEvent> mergedEvents) {
        LocalDate today = LocalDate.now(clock);

        Map<LocalDate, List<CalendarEventDto>> eventsByDate = new HashMap<>();
        for (RawCalendarEvent raw : mergedEvents) {
            CalendarEventDto eventDto = toEventDto(raw);
            LocalDate date = LocalDate.parse(eventDto.getDate(), DATE_FMT);
            eventsByDate.computeIfAbsent(date, ignored -> new ArrayList<>()).add(eventDto);
        }

        for (List<CalendarEventDto> events : eventsByDate.values()) {
            events.sort(Comparator.comparing(CalendarEventDto::getTimeText).thenComparing(CalendarEventDto::getTitle));
        }

        LocalDate firstDay = targetMonth.atDay(1);
        int shift = firstDay.getDayOfWeek().getValue() % 7;
        LocalDate gridStart = firstDay.minusDays(shift);

        LocalDate lastDay = targetMonth.atEndOfMonth();
        int tail = 6 - (lastDay.getDayOfWeek().getValue() % 7);
        LocalDate gridEnd = lastDay.plusDays(tail);

        List<CalendarDayDto> days = new ArrayList<>();
        for (LocalDate cursor = gridStart; !cursor.isAfter(gridEnd); cursor = cursor.plusDays(1)) {
            CalendarDayDto dayDto = new CalendarDayDto();
            dayDto.setDate(cursor.format(DATE_FMT));
            dayDto.setDayNumber(cursor.getDayOfMonth());
            dayDto.setCurrentMonth(cursor.getMonthValue() == targetMonth.getMonthValue());
            dayDto.setToday(cursor.isEqual(today));
            dayDto.setSunday(cursor.getDayOfWeek() == DayOfWeek.SUNDAY);

            List<CalendarEventDto> events = eventsByDate.getOrDefault(cursor, List.of());
            dayDto.setEvents(new ArrayList<>(events));
            dayDto.setHoliday(events.stream().anyMatch(e -> "HOLIDAY".equalsIgnoreCase(e.getSourceType())));
            dayDto.setHasPersonalEvent(events.stream().anyMatch(e -> "PERSONAL".equalsIgnoreCase(e.getSourceType())));
            days.add(dayDto);
        }

        LocalDate upcomingLimitDate = today.plusDays(35);
        List<CalendarEventDto> upcoming = mergedEvents.stream()
                .map(this::toEventDto)
                .filter(event -> {
                    LocalDate eventDate = LocalDate.parse(event.getDate(), DATE_FMT);
                    return !eventDate.isBefore(today) && !eventDate.isAfter(upcomingLimitDate);
                })
                .collect(Collectors.toList());

        CalendarMonthDto dto = new CalendarMonthDto();
        dto.setYear(targetMonth.getYear());
        dto.setMonth(targetMonth.getMonthValue());
        dto.setDays(days);
        dto.setUpcomingEvents(upcoming);
        return dto;
    }

    private CalendarEventDto toEventDto(RawCalendarEvent raw) {
        CalendarEventDto dto = new CalendarEventDto();
        dto.setTitle(raw.title);
        dto.setDate(raw.start.toLocalDate().format(DATE_FMT));
        dto.setAllDay(raw.allDay);
        dto.setTimeText(buildTimeText(raw));
        dto.setSourceName(raw.sourceName);
        dto.setSourceType(raw.sourceType);
        return dto;
    }

    private String buildTimeText(RawCalendarEvent raw) {
        if (raw.allDay) {
            return "종일";
        }
        int hour = raw.start.getHour();
        int minute = raw.start.getMinute();
        String meridiem = hour < 12 ? "오전" : "오후";
        int displayHour = hour % 12;
        if (displayHour == 0) {
            displayHour = 12;
        }
        if (minute == 0) {
            return meridiem + " " + displayHour + "시";
        }
        return meridiem + " " + displayHour + "시 " + minute + "분";
    }

    private record RawCalendarEvent(String title,
                                    LocalDateTime start,
                                    LocalDateTime end,
                                    boolean allDay,
                                    String sourceName,
                                    String sourceType) {
    }

    private record CacheEntry(Instant cachedAt, List<RawCalendarEvent> events) {
    }

    public static class CalendarLoadResult {
        public enum Status {
            READY,
            DISABLED,
            NO_SOURCES,
            ERROR
        }

        private final Status status;
        private final CalendarMonthDto monthDto;

        private CalendarLoadResult(Status status, CalendarMonthDto monthDto) {
            this.status = status;
            this.monthDto = monthDto;
        }

        public static CalendarLoadResult ready(CalendarMonthDto monthDto) {
            return new CalendarLoadResult(Status.READY, monthDto);
        }

        public static CalendarLoadResult disabled() {
            return new CalendarLoadResult(Status.DISABLED, null);
        }

        public static CalendarLoadResult noSources() {
            return new CalendarLoadResult(Status.NO_SOURCES, null);
        }

        public static CalendarLoadResult error() {
            return new CalendarLoadResult(Status.ERROR, null);
        }

        public Status getStatus() {
            return status;
        }

        public CalendarMonthDto getMonthDto() {
            return monthDto;
        }
    }
}
