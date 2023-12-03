package com.example.examapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class SDLV extends ArrayAdapter {
    private Context context;
    private List<SubmissionDetails> submissionDetails;

    public SDLV(Context context,int tgrcl, List<SubmissionDetails> submissionDetails) {
        super(context, R.layout.tgrcl, submissionDetails);
        this.context = context;
        this.submissionDetails =submissionDetails;
    }

    SubmissionDetails getItemAtPosition(int position){
        return submissionDetails.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Inflate your custom list item layout here
        LayoutInflater inflater = LayoutInflater.from(context);
        View listItemView = inflater.inflate(R.layout.sdlv, parent, false);

        // Get the Group object at the current position
        SubmissionDetails currentSubmission = submissionDetails.get(position);

        // Find the TextViews in your list item layout and set their text based on the Group object
        TextView SDLV_UserName = listItemView.findViewById(R.id.SDLV_UserName);
        TextView SDLV_Email = listItemView.findViewById(R.id.SDLV_Email);

        // Set the text for the TextViews
        SDLV_UserName.setText(currentSubmission.UserName);
        SDLV_Email.setText(currentSubmission.Email);

        return listItemView;
    }
}
