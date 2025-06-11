package com.example.yidong222.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Date;

public class ExamDto implements Serializable {
    @SerializedName("id")
    private Integer id;

    @SerializedName("course_id")
    private Integer courseId;

    @SerializedName("course_name")
    private String courseName;

    @SerializedName("title")
    private String title;

    @SerializedName("exam_date")
    private String examDate;

    @SerializedName("duration")
    private Integer duration;

    @SerializedName("location")
    private String location;

    @SerializedName("seat_number")
    private String seatNumber;

    @SerializedName("description")
    private String description;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public ExamDto() {
    }

    public ExamDto(Integer courseId, String title, String examDate, Integer duration, String location,
            String description) {
        this.courseId = courseId;
        this.title = title;
        this.examDate = examDate;
        this.duration = duration;
        this.location = location;
        this.description = description;
    }

    // 将ExamDto转换为Exam对象，用于应用内部使用
    public Exam toExam() {
        Exam exam = new Exam(
                courseName != null ? courseName : "未知课程",
                title != null ? title : "未命名考试",
                getFormattedDate(), // 使用格式化的日期
                getFormattedTime(),
                location != null ? location : "",
                seatNumber != null ? seatNumber : "" // 使用API提供的座位号
        );

        if (id != null) {
            exam.setId(id);
        }

        // 设置课程ID
        if (courseId != null) {
            exam.setCourseId(courseId);
        }

        if (duration != null) {
            exam.setDuration(duration);
        }

        if (description != null) {
            exam.setDescription(description);
        }

        return exam;
    }

    // 获取格式化的日期字符串
    public String getFormattedDate() {
        if (examDate == null) {
            return "";
        }

        try {
            // 处理ISO格式 (2023-04-16T01:00:00.000Z)
            if (examDate.contains("T")) {
                return examDate.split("T")[0];
            }

            // 处理普通格式 (2023-04-16 01:00:00)
            if (examDate.contains(" ")) {
                return examDate.split(" ")[0];
            }

            return examDate;
        } catch (Exception e) {
            return "";
        }
    }

    // 获取格式化的时间字符串
    public String getFormattedTime() {
        if (examDate == null) {
            return "";
        }

        try {
            // 处理ISO格式 (2023-04-16T01:00:00.000Z)
            if (examDate.contains("T")) {
                String timePart = examDate.split("T")[1];
                return timePart.substring(0, 5); // 获取HH:MM格式
            }

            // 处理普通格式 (2023-04-16 01:00:00)
            if (examDate.contains(" ")) {
                String timePart = examDate.split(" ")[1];
                return timePart.substring(0, 5); // 获取HH:MM格式
            }

            return "";
        } catch (Exception e) {
            return "";
        }
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getExamDate() {
        return examDate;
    }

    public void setExamDate(String examDate) {
        this.examDate = examDate;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}