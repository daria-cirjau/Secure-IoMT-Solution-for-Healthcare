package com.disertatie;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameField, passwordField;
    private static final String BASE_URL = "https://10.0.2.2:8443/api/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameField = findViewById(R.id.username);
        passwordField = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(v -> {
            String username = usernameField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (!username.isEmpty() && !password.isEmpty()) {
                loginUser(username, password);
            } else {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginUser(String username, String password) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("username", username);
                json.put("password", password);

                try (OutputStream os = new BufferedOutputStream(conn.getOutputStream())) {
                    os.write(json.toString().getBytes());
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject responseJson = new JSONObject(sb.toString());
                    boolean success = responseJson.optBoolean("success", false);
                    String doctorId = responseJson.optString("practitionerId", "");

                    runOnUiThread(() -> {
                        if (success && !doctorId.isEmpty()) {
                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(this, PatientListActivity.class);
                            intent.putExtra("doctorId", doctorId);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Invalid credentials.", Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Invalid credentials.", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Network error.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
