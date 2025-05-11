package com.example.f1app;

import static com.example.f1app.MainActivity.getStringByName;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.shawnlin.numberpicker.NumberPicker;

import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;

public class predictPageActivity extends AppCompatActivity {
    private NumberPicker numberPicker;
    private RadioGroup radioGroup;
    private Button predictButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.predict_page);

        numberPicker = findViewById(R.id.numberpicker);
        predictButton = findViewById(R.id.predict_button);
        radioGroup = findViewById(R.id.radioGroup);
        predictButton = findViewById(R.id.predict_button);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(false);

        ImageButton backButton = (ImageButton) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("/schedule/season/2025")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ArrayList<String> raceListLocalized = new ArrayList<>();
                        ArrayList<String> raceList = new ArrayList<>();
                        long raceCount = snapshot.getChildrenCount();

                        numberPicker.setMinValue(0);
                        numberPicker.setMaxValue((int) raceCount);
                        numberPicker.setWrapSelectorWheel(true);

                        for (DataSnapshot raceSnaps: snapshot.getChildren()){
                            String raceName = raceSnaps.getKey();

                            if (!raceName.equals("Australian Grand Prix")){
                                String preprocessedGPName = raceName.toLowerCase();
                                preprocessedGPName = preprocessedGPName.replace(" ", "_");
                                String localizedGpName = getString(getStringByName(preprocessedGPName + "_text"));

                                raceListLocalized.add(localizedGpName);
                                raceList.add(raceName);
                            }
                        }

                        String[] values = raceListLocalized.toArray(new String[0]);
                        numberPicker.setMinValue(0);
                        numberPicker.setMaxValue(values.length - 1);
                        numberPicker.setDisplayedValues(values);

                        predictButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int selectedId = radioGroup.getCheckedRadioButtonId();
                                if (selectedId == -1) {
                                    Toast.makeText(predictPageActivity.this, "No answer has been selected",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    RadioButton radioButton = findViewById(selectedId);
                                    String value = radioButton.getTag().toString();
                                    String gpName = raceList.get(numberPicker.getValue());

                                    Intent i = new Intent(predictPageActivity.this, predictResultPage.class);
                                    i.putExtra("event", value);
                                    i.putExtra("gp", gpName);
                                    startActivity(i);
                                }
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("predictPageActivity", error.getMessage());
                    }
                });
    }
}
