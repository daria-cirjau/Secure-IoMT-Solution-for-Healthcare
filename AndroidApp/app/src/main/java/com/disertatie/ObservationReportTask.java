package com.disertatie;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.chaquo.python.Python;
import com.chaquo.python.PyObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class ObservationReportTask extends Thread {
    private final Context context;
    private final String patientId;
    private final String patientName;
    private final String dateAfter;
    private final String dateBefore;
    private final Handler mainHandler;

    public ObservationReportTask(Context context,
                                 String patientId,
                                 String patientName,
                                 String dateAfter,
                                 String dateBefore) {
        this.context     = context;
        this.patientId   = patientId;
        this.patientName = patientName;
        this.dateAfter   = dateAfter;
        this.dateBefore  = dateBefore;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void run() {
        String obsJson = fetchObservations();
        if (obsJson == null) return;

        try {
            Python py    = Python.getInstance();
            PyObject chart = py.getModule("chart_manager");
            String imgPath = chart.callAttr(
                    "generate_historical_image",
                    obsJson,
                    dateAfter,
                    dateBefore
            ).toString();

            mainHandler.post(() -> {
                try {
                    generatePdfFromImage(imgPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    showToast("PDF creation failed: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Report error: " + e.getMessage());
        }
    }

    private String fetchObservations() {
        try {
            String urlStr = "https://10.0.2.2:8443/api/observations"
                    + "?patientId=" + patientId
                    + "&dateAfter=" + dateAfter
                    + "&dateBefore=" + dateBefore;

            URL url = new URL(urlStr);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != 200) {
                showToast("Failed to fetch observations: " + conn.getResponseCode());
                return null;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Network error: " + e.getMessage());
            return null;
        }
    }

    private void generatePdfFromImage(String imgPath) throws IOException {
        Bitmap chartBitmap = BitmapFactory.decodeFile(imgPath);
        int width = chartBitmap.getWidth();
        int height = chartBitmap.getHeight();

        Date startDate = Date.from(Instant.parse(dateAfter));
        Date endDate = Date.from(Instant.parse(dateBefore));

        PdfDocument pdfDocument = createPdfWithChart(chartBitmap, width, height, startDate, endDate);
        File outputFile = buildOutputFile(startDate, endDate);

        try (FileOutputStream outStream = new FileOutputStream(outputFile)) {
            pdfDocument.writeTo(outStream);
        }

        pdfDocument.close();
        showToast("Report saved to: " + outputFile.getAbsolutePath());
    }

    private PdfDocument createPdfWithChart(Bitmap chartBitmap, int width, int height, Date startDate, Date endDate) {
        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height + 100, 1).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        drawHeader(canvas, startDate, endDate);
        canvas.drawBitmap(chartBitmap, 0, 100, null);

        pdf.finishPage(page);
        return pdf;
    }

    private void drawHeader(Canvas canvas, Date startDate, Date endDate) {
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(36);
        titlePaint.setFakeBoldText(true);

        Paint subPaint = new Paint();
        subPaint.setTextSize(20);

        SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String title = "Report for " + patientName;
        String subtitle = "Date: " + displayFormat.format(startDate) + " – " + displayFormat.format(endDate);

        canvas.drawText(title, 20, 40, titlePaint);
        canvas.drawText(subtitle, 20, 80, subPaint);
    }

    private File buildOutputFile(Date startDate, Date endDate) {
        SimpleDateFormat fnFormat = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault());
        String start = fnFormat.format(startDate);
        String end = fnFormat.format(endDate);
        String safeName = patientName.replaceAll("\\s+", "_");
        String fileName = String.format("report_%s_%s-%s.pdf", safeName, start, end);

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return new File(downloadsDir, fileName);
    }

    private void showToast(String message) {
        mainHandler.post(() ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        );
    }
}
