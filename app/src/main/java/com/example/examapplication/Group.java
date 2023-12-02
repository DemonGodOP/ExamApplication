package com.example.examapplication;

public class Group {
    public String Group_Name,Subject_Name,Subject_Code,Description,Group_ID,Institute,TeacherName;
    Group(String Group_Name,String Subject_Name,String Subject_Code, String Description,String Group_ID,String Institute,String TeacherName){
        this.Group_Name=Group_Name;
        this. Subject_Code=Subject_Code;
        this.Subject_Name=Subject_Name;
        this.Description=Description;
        this.Institute=Institute;
        this.Group_ID=Group_ID;
        this.TeacherName=TeacherName;
    }
    Group(){};
}
