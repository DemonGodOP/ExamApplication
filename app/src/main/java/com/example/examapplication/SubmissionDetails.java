package com.example.examapplication;

import java.util.List;

public class SubmissionDetails {
    public String UserName,UserID,Email;
    public List<String> Answers;

    SubmissionDetails(){};

    SubmissionDetails(String UserName,String UserID,String Email,List<String>Answers){
        this.UserName=UserName;
        this.UserID=UserID;
        this.Email=Email;
        this.Answers=Answers;
    }

}
