package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** circuitName -> circuitId from one read of "circuits" for the whole app run; no listener per row. */
final class circuitNames {

    private interface Waiter {
        void onReady(Map<String, String> ids);
    }

    private static Map<String, String> ids;
    private static boolean loading;
    private static final List<Waiter> WAITING = new ArrayList<>();

    private circuitNames() {
    }

    static void bind(Context context, String circuitName, TextView target) {
        target.setTag(circuitName);

        resolve(map -> {
            String id = map.get(circuitName);

            // a recycled row may already belong to another race
            if (id == null || !circuitName.equals(target.getTag())) {
                return;
            }

            int res = getStringByName(id + "_text");

            if (res != 0) {
                target.setText(context.getString(res));
            }
        });
    }

    private static void resolve(Waiter waiter) {
        if (ids != null) {
            waiter.onReady(ids);
            return;
        }

        WAITING.add(waiter);

        if (loading) {
            return;
        }

        loading = true;

        FirebaseDatabase.getInstance().getReference().child("circuits")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, String> map = new HashMap<>();

                        for (DataSnapshot circuit : snapshot.getChildren()) {
                            String name = circuit.child("circuitName").getValue(String.class);
                            String id = circuit.child("circuitId").getValue(String.class);

                            if (name != null && id != null) {
                                map.put(name, id);
                            }
                        }

                        ids = map;
                        loading = false;
                        flush();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        loading = false; // the next row tries again
                        WAITING.clear();
                    }
                });
    }

    private static void flush() {
        List<Waiter> copy = new ArrayList<>(WAITING);
        WAITING.clear();

        for (Waiter waiter : copy) {
            waiter.onReady(ids);
        }
    }
}