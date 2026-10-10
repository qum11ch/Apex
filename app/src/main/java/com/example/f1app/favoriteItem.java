package com.example.f1app;

public class favoriteItem {
    public final String value;
    public final String driverName;
    public final String familyName;
    public final String teamName;
    public final String subtitle;
    public final String imagePath;
    public final boolean empty;
    public final boolean team;
    public final String lastEntry;

    public favoriteItem(
            String value,
            String driverName,
            String familyName,
            String teamName,
            String subtitle,
            String imagePath,
            boolean empty,
            boolean team,
            String lastEntry) {

        this.value = value;
        this.driverName = driverName;
        this.familyName = familyName;
        this.teamName = teamName;
        this.subtitle = subtitle;
        this.imagePath = imagePath;
        this.empty = empty;
        this.team = team;
        this.lastEntry = lastEntry;
    }

    public static favoriteItem emptyDriver() {
        return new favoriteItem(
                null,
                "",
                "",
                "",
                "Пилот не выбран",
                null,
                true,
                false,
                null
        );
    }

    public static favoriteItem emptyTeam() {
        return new favoriteItem(
                null,
                "",
                "",
                "",
                "Команда не выбрана",
                null,
                true,
                true,
                null
        );
    }
}