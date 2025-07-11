package com.disertatie;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.disertatie.fhir.ChartImageLoader;
import com.disertatie.fhir.FhirObservationHandler;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class PatientInfoActivity extends AppCompatActivity {
    private ImageView chartImage;
    private ChartImageLoader imageLoader;
    private TextView startDateText;
    private TextView endDateText;
    private Button fetchReportButton;
    private String startDateUTC = "";
    private String endDateUTC = "";
    private String patientName;
    private String patientId;

    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
    private final SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        setContentView(R.layout.activity_patient_info);

        initPython();
        initViews();
        loadPatientInfo();
        setupListeners();
        startDataSubscriber();
    }

    private void initPython() {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }

    private void initViews() {
        chartImage = findViewById(R.id.chartImage);
        imageLoader = new ChartImageLoader(this, chartImage);
        startDateText = findViewById(R.id.startDateText);
        endDateText = findViewById(R.id.endDateText);
        fetchReportButton = findViewById(R.id.fetchReportButton);
    }

    private void loadPatientInfo() {
        patientName = getIntent().getStringExtra("patientName");
        patientId = getIntent().getStringExtra("patientId");
        TextView nameText = findViewById(R.id.patientNameText);
        nameText.setText(patientName);
    }

    private void setupListeners() {
        findViewById(R.id.reportButton).setOnClickListener(v -> toggleDateSelectionUI());
        startDateText.setOnClickListener(v -> showDateTimePicker(true));
        endDateText.setOnClickListener(v -> showDateTimePicker(false));
        fetchReportButton.setOnClickListener(v -> handleFetchReport());
    }

    private void startDataSubscriber() {
        new Thread(() -> {
            FhirObservationHandler handler = new FhirObservationHandler();
            RabbitMQSubscriber subscriber = new RabbitMQSubscriber(handler, imageLoader);
            subscriber.start();
        }).start();
    }

    private void toggleDateSelectionUI() {
        findViewById(R.id.dateTimePickerLayout).setVisibility(View.VISIBLE);
        fetchReportButton.setVisibility(View.VISIBLE);
    }

    private void showDateTimePicker(boolean isStart) {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);

            TimePickerDialog.OnTimeSetListener timeSetListener = (timeView, hour, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);

                String dateUtc = serverFormat.format(calendar.getTime());
                String dateLocal = displayFormat.format(calendar.getTime());

                if (isStart) {
                    startDateUTC = dateUtc;
                    startDateText.setText(dateLocal);
                } else {
                    endDateUTC = dateUtc;
                    endDateText.setText(dateLocal);
                }
            };

            new TimePickerDialog(this, timeSetListener,
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true).show();
        };

        new DatePickerDialog(this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void handleFetchReport() {
        if (startDateUTC.isEmpty() || endDateUTC.isEmpty()) {
            Toast.makeText(this, "Please select both start and end times", Toast.LENGTH_SHORT).show();
            return;
        }

        new ObservationReportTask(this, patientId, patientName, startDateUTC, endDateUTC).start();
    }
}
