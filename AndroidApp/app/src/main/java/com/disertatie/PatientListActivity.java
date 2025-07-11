package com.disertatie;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.disertatie.adapter.PatientAdapter;
import com.disertatie.entity.PatientItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class PatientListActivity extends AppCompatActivity {

    private ListView patientListView;
    private PatientAdapter adapter;
    private List<PatientItem> patientItems = new ArrayList<>();
    private String doctorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        doctorId = getIntent().getStringExtra("doctorId");

        setContentView(R.layout.activity_patient_list);

        TextView title = findViewById(R.id.titleText);
        title.setText("Patients");

        patientListView = findViewById(R.id.patientListView);
        adapter = new PatientAdapter(this, patientItems);
        patientListView.setAdapter(adapter);

        patientListView.setOnItemClickListener((parent, view, position, id) -> {
            PatientItem patient = adapter.getItem(position);
            Intent intent = new Intent(this, PatientInfoActivity.class);
            intent.putExtra("patientId", patient.getId());
            intent.putExtra("patientName", patient.getFullName());

            startActivity(intent);
        });

        fetchPatients();
    }

    private void fetchPatients() {
        new Thread(() -> {
            try {
                URL url = new URL("https://10.0.2.2:8443/api/patients?doctorId=" + doctorId);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONArray jsonArray = new JSONArray(response.toString());
                    List<PatientItem> list = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String id = obj.getString("id");
                        String name = obj.getString("name");
                        list.add(new PatientItem(name, id));
                    }

                    runOnUiThread(() -> {
                        patientItems.clear();
                        patientItems.addAll(list);
                        adapter.notifyDataSetChanged();
                    });

                } else {
                    showError("HTTP error: " + responseCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("Network error.");
            }
        }).start();
    }

    private void showError(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

}
