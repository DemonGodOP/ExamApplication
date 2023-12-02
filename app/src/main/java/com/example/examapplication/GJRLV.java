package com.example.examapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class GJRLV extends ArrayAdapter {
    private Context context;
    private List<ParticipantDetails> participantDetails;

    public GJRLV(Context context,int gjrlv, List<ParticipantDetails> participantDetails) {
        super(context, R.layout.tgrcl, participantDetails);
        this.context = context;
        this.participantDetails = participantDetails;
    }

    ParticipantDetails getItemAtPosition(int position){
        return participantDetails.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Inflate your custom list item layout here
        LayoutInflater inflater = LayoutInflater.from(context);
        View listItemView = inflater.inflate(R.layout.gjrlv, parent, false);

        // Get the Group object at the current position
        ParticipantDetails currentParticipant = participantDetails.get(position);

        // Find the TextViews in your list item layout and set their text based on the Group object
        TextView GJR_UserName = listItemView.findViewById(R.id.GJR_UserName);
        TextView GJR_Email = listItemView.findViewById(R.id.GJR_Email);

        // Set the text for the TextViews
        GJR_UserName.setText(currentParticipant.UserName);
        GJR_Email.setText(currentParticipant.Email);

        return listItemView;
    }
}
