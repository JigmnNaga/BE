package com.jigmnnaga.application.mapper;

import com.jigmnnaga.application.dto.response.ScheduleHeatmapResponse;
import com.jigmnnaga.application.dto.response.ScheduleHeatmapResponse.DateSlotsResponse;
import com.jigmnnaga.application.dto.response.ScheduleHeatmapResponse.HeatmapMemberResponse;
import com.jigmnnaga.application.dto.response.ScheduleHeatmapResponse.SlotResponse;
import com.jigmnnaga.application.dto.response.ScheduleHeatmapResponse.TimeRangeCandidateResponse;
import com.jigmnnaga.domain.entity.Appointment;
import com.jigmnnaga.domain.entity.AppointmentCandidateDate;
import com.jigmnnaga.domain.entity.AppointmentParticipant;
import com.jigmnnaga.domain.entity.Member;
import com.jigmnnaga.domain.entity.ParticipantAvailability;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ScheduleHeatmapMapper {

    private static final int MINUTES_PER_HOUR = 60;
    private static final int MAX_TOP_CANDIDATES = 5;

    public ScheduleHeatmapResponse toHeatmapResponse(Appointment appointment, List<AppointmentParticipant> participants,
                                                     int slotMinutes) {
        List<LocalDate> candidateDates = appointment.getCandidateDates().stream()
                .map(AppointmentCandidateDate::getCandidateDate)
                .sorted()
                .toList();
        List<ParticipantAvailability> availabilities = participants.stream()
                .flatMap(participant -> participant.getAvailabilities().stream())
                .toList();

        List<DateSlots> dateSlots = candidateDates.stream()
                .map(date -> new DateSlots(date, toSlots(date, participants, availabilities, slotMinutes)))
                .toList();

        return ScheduleHeatmapResponse.builder()
                .slotMinutes(slotMinutes)
                .totalMemberCount(participants.size())
                .dates(dateSlots.stream()
                        .map(dateSlot -> DateSlotsResponse.builder()
                                .date(dateSlot.date())
                                .slots(dateSlot.slots().stream().map(this::toSlotResponse).toList())
                                .build())
                        .toList())
                .topCandidates(toTopCandidates(dateSlots))
                .build();
    }

    private List<Slot> toSlots(LocalDate date, List<AppointmentParticipant> participants,
                               List<ParticipantAvailability> availabilities, int slotMinutes) {
        if (availabilities.isEmpty()) {
            return List.of();
        }

        int rangeStart = availabilities.stream()
                .mapToInt(availability -> minutesOf(availability.getStartTime()))
                .min()
                .orElseThrow() / slotMinutes * slotMinutes;
        int latestEnd = availabilities.stream()
                .mapToInt(availability -> minutesOf(availability.getEndTime()))
                .max()
                .orElseThrow();
        int rangeEnd = (latestEnd + slotMinutes - 1) / slotMinutes * slotMinutes;

        List<Slot> slots = new ArrayList<>();
        for (int slotStart = rangeStart; slotStart < rangeEnd; slotStart += slotMinutes) {
            int slotEnd = slotStart + slotMinutes;
            List<Member> members = availableMembers(date, participants, slotStart, slotEnd);
            slots.add(new Slot(slotStart, slotEnd, members));
        }
        return slots;
    }

    private List<Member> availableMembers(LocalDate date, List<AppointmentParticipant> participants,
                                          int slotStart, int slotEnd) {
        return participants.stream()
                .filter(participant -> participant.getAvailabilities().stream()
                        .filter(availability -> availability.getAvailableDate().equals(date))
                        .anyMatch(availability -> covers(availability, slotStart, slotEnd)))
                .map(AppointmentParticipant::getMember)
                .toList();
    }

    private boolean covers(ParticipantAvailability availability, int slotStart, int slotEnd) {
        return minutesOf(availability.getStartTime()) <= slotStart && slotEnd <= minutesOf(availability.getEndTime());
    }

    private List<TimeRangeCandidateResponse> toTopCandidates(List<DateSlots> dateSlots) {
        List<TimeRange> ranges = new ArrayList<>();
        for (DateSlots dateSlot : dateSlots) {
            TimeRange current = null;
            for (Slot slot : dateSlot.slots()) {
                if (slot.members().isEmpty()) {
                    current = null;
                    continue;
                }
                if (current != null && current.end() == slot.start() && current.members().equals(slot.members())) {
                    current = new TimeRange(current.date(), current.start(), slot.end(), current.members());
                    ranges.set(ranges.size() - 1, current);
                    continue;
                }
                current = new TimeRange(dateSlot.date(), slot.start(), slot.end(), slot.members());
                ranges.add(current);
            }
        }

        return ranges.stream()
                .sorted(Comparator.comparingInt((TimeRange range) -> range.members().size()).reversed()
                        .thenComparing(Comparator.comparingInt((TimeRange range) -> range.end() - range.start()).reversed())
                        .thenComparing(TimeRange::date)
                        .thenComparingInt(TimeRange::start))
                .limit(MAX_TOP_CANDIDATES)
                .map(range -> TimeRangeCandidateResponse.builder()
                        .date(range.date())
                        .startTime(formatMinutes(range.start()))
                        .endTime(formatMinutes(range.end()))
                        .availableCount(range.members().size())
                        .members(toMemberResponses(range.members()))
                        .build())
                .toList();
    }

    private SlotResponse toSlotResponse(Slot slot) {
        return SlotResponse.builder()
                .startTime(formatMinutes(slot.start()))
                .endTime(formatMinutes(slot.end()))
                .availableCount(slot.members().size())
                .members(toMemberResponses(slot.members()))
                .build();
    }

    private List<HeatmapMemberResponse> toMemberResponses(List<Member> members) {
        return members.stream()
                .map(member -> HeatmapMemberResponse.builder()
                        .memberId(member.getId())
                        .nickname(member.getNickname())
                        .profileImageUrl(member.getProfileImageUrl())
                        .build())
                .toList();
    }

    private int minutesOf(LocalTime time) {
        return time.getHour() * MINUTES_PER_HOUR + time.getMinute();
    }

    private String formatMinutes(int minutes) {
        return String.format("%02d:%02d", minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR);
    }

    private record DateSlots(LocalDate date, List<Slot> slots) {
    }

    private record Slot(int start, int end, List<Member> members) {
    }

    private record TimeRange(LocalDate date, int start, int end, List<Member> members) {
    }
}
