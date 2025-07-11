package com.disertatie.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;

import com.disertatie.R;
import com.disertatie.entity.PatientItem;

import java.util.List;

public class PatientAdapter extends ArrayAdapter<PatientItem> {

    public PatientAdapter(Context context, List<PatientItem> patients) {
        super(context, 0, patients);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        PatientItem patient = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_patient, parent, false);
        }

        TextView nameView = convertView.findViewById(R.id.patientName);
        TextView infoView = convertView.findViewById(R.id.patientInfo);

        nameView.setText(patient.getFullName());
        infoView.setText("Patient ID: " + patient.getId());

        return convertView;
    }
}

