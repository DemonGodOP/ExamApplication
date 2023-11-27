package com.example.examapplication;

public class Group {
    public String Group_Name,Subject_Name,Subject_Code,Description,Group_ID,Institute;
    Group(String Group_Name,String Subject_Name,String Subject_Code, String Description,String Group_ID,String Institute){
        this.Group_Name=Group_Name;
        this. Subject_Code=Subject_Code;
        this.Subject_Name=Subject_Name;
        this.Description=Description;
        this.Institute=Institute;
        this.Group_ID=Group_ID;
    }
    Group(){};

    /*public String getGroupName(){
        return Group_Name;
    }

    public String getSubjectCode(){
        return Subject_Code;
    }

    public String getGroupId(){
        return Group_ID;
    }

    public String getDescription(){ return Description; }

    public String getSubjectName(){
        return Subject_Name;
    }

    public String getInstitute(){
        return Institute;
    }*/
}
