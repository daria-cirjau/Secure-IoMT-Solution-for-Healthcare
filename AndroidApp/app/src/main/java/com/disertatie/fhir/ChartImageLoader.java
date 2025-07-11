package com.disertatie.fhir;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import android.widget.TextView;

import com.disertatie.R;

import java.io.File;

public class ChartImageLoader {

    private final Activity activity;
    private final ImageView imageView;
    private final TextView noDataText;

    public ChartImageLoader(Activity activity, ImageView imageView) {
        this.activity = activity;
        this.imageView = imageView;
        this.noDataText = activity.findViewById(R.id.noDataText);
    }

    public void loadImage(String path) {
        activity.runOnUiThread(() -> {
            File imgFile = new File(path);
            if (imgFile.exists() && imgFile.length() > 0) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                imageView.setImageBitmap(null);
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(ImageView.VISIBLE);
            }
        });
    }

    public void showNoDataMessage(boolean show) {
        activity.runOnUiThread(() -> {
            noDataText.setVisibility(show ? TextView.VISIBLE : TextView.GONE);
            imageView.setVisibility(show ? ImageView.GONE : ImageView.VISIBLE);
        });
    }
}
