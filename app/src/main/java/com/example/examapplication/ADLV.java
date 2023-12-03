package com.example.examapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ADLV extends ArrayAdapter {

    private Context context;
    private List<Assignment> assignments;

    public ADLV(Context context,int adlv, List<Assignment> assignments) {
        super(context, R.layout.tgrcl, assignments);
        this.context = context;
        this.assignments = assignments;
    }

    Assignment getItemAtPosition(int position){
        return assignments.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Inflate your custom list item layout here
        LayoutInflater inflater = LayoutInflater.from(context);
        View listItemView = inflater.inflate(R.layout.adlv, parent, false);

        // Get the Group object at the current position
        Assignment currentAssignment = assignments.get(position);

        // Find the TextViews in your list item layout and set their text based on the Group object
        TextView ADLV_Name = listItemView.findViewById(R.id.ADLV_Name);
        TextView ADLV_Timing = listItemView.findViewById(R.id.ADLV_Timing);

        // Set the text for the TextViews
        ADLV_Name.setText(currentAssignment.Name);
        ADLV_Timing.setText(currentAssignment.Timing);

        return listItemView;
    }
}
