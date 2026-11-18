package com.jigmnnaga.domain.entity;

import java.util.Optional;

public enum DepartureUpcomingStage {
    H24(24 * 60, 23 * 60),
    H12(12 * 60, 11 * 60),
    H6(6 * 60, 5 * 60),
    H3(3 * 60, 2 * 60),
    H1(60, 0);

    private final int upperMinutes; // inclusive
    private final int lowerMinutes; // exclusive

    DepartureUpcomingStage(int upperMinutes, int lowerMinutes) {
        this.upperMinutes = upperMinutes;
        this.lowerMinutes = lowerMinutes;
    }

    public static Optional<DepartureUpcomingStage> from(long minutesUntil) {
        for (DepartureUpcomingStage stage : values()) {
            if (minutesUntil > stage.lowerMinutes && minutesUntil <= stage.upperMinutes) {
                return Optional.of(stage);
            }
        }
        return Optional.empty();
    }
}
