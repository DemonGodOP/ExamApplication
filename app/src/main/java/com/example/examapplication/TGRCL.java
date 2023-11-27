package com.example.examapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class TGRCL extends ArrayAdapter {
    private Context context;
    private List<Group> groups;

    public TGRCL(Context context,int tgrcl, List<Group> groups) {
        super(context, R.layout.tgrcl, groups);
        this.context = context;
        this.groups = groups;
    }

    Group getItemAtPosition(int position){
        return groups.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Inflate your custom list item layout here
        LayoutInflater inflater = LayoutInflater.from(context);
        View listItemView = inflater.inflate(R.layout.tgrcl, parent, false);

        // Get the Group object at the current position
        Group currentGroup = groups.get(position);

        // Find the TextViews in your list item layout and set their text based on the Group object
        TextView T_GroupName = listItemView.findViewById(R.id.T_GroupName);
        TextView T_GroupSubjectCode = listItemView.findViewById(R.id.T_GroupSubjectCode);

        // Set the text for the TextViews
        T_GroupName.setText(currentGroup.Group_Name);
        T_GroupSubjectCode.setText(currentGroup.Subject_Code);

        return listItemView;
    }
}
