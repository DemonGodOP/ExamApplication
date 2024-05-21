package com.example.examapplication;

import java.util.ArrayList;
import java.util.List;

public class Assignment {
    public List<String> Questions;
    public boolean Active;

    public String Name,Timing,Assignment_ID,Duration;

    Assignment(){};

    Assignment(List<String> Questions,boolean Active,String Name,String Timing,String Assignment_ID,String Duration){
        this.Questions=Questions;
        this.Active=Active;
        this.Name=Name;
        this.Timing=Timing;
        this.Assignment_ID=Assignment_ID;
        this.Duration=Duration;
    }

}
