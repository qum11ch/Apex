package com.example.f1app;

import com.google.firebase.database.FirebaseDatabase;

final class teamCardColors {

    private teamCardColors() {
    }

    static void ensure(teamsList team, Runnable redraw) {
        String id = team.getTeamId();

        if (id == null || id.trim().isEmpty()) {
            return;
        }

        String cached = teamColors.get(id);

        if (cached != null) {
            team.setTeamColor(cached);
        }

        teamColors.fetch(
                FirebaseDatabase.getInstance().getReference(),
                id,
                color -> {
                    if (color != null) {
                        team.setTeamColor(color);
                        redraw.run();
                    }
                }
        );
    }
}