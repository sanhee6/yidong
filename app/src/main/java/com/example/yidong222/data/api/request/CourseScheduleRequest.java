package com.example.yidong222.data.api.request;

import com.google.gson.annotations.SerializedName;

public class CourseScheduleRequest {
    @SerializedName("course_name")
    private String courseName;

    @SerializedName("teacher_name")
    private String teacherName;

    @SerializedName("class_time")
    private String classTime;

    private String classroom;

    public CourseScheduleRequest() {
    }

    public CourseScheduleRequest(String courseName, String teacherName, String classTime, String classroom) {
        this.courseName = courseName;
        this.teacherName = teacherName;
        this.classTime = classTime;
        this.classroom = classroom;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getClassTime() {
        return classTime;
    }

    public void setClassTime(String classTime) {
        this.classTime = classTime;
    }

    public String getClassroom() {
        return classroom;
    }

    public void setClassroom(String classroom) {
        this.classroom = classroom;
    }
}