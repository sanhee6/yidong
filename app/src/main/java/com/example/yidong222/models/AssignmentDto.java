package com.example.yidong222.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class AssignmentDto implements Serializable {
    @SerializedName("id")
    private Integer id;

    @SerializedName("course_id")
    private Integer courseId;

    @SerializedName("course_name")
    private String courseName;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("due_date")
    private String deadline;

    @SerializedName("max_score")
    private Integer maxScore;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // 添加状态字段
    @SerializedName("status")
    private String status;

    // 添加完成状态字段
    @SerializedName("completed")
    private Boolean completed;

    public AssignmentDto() {
    }

    public AssignmentDto(Integer courseId, String title, String description, String deadline, Integer maxScore) {
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.maxScore = maxScore;
        this.status = "未完成";
        this.completed = false;
    }

    // 将AssignmentDto转换为Assignment对象，用于应用内部使用
    public Assignment toAssignment() {
        Assignment assignment = new Assignment(
                title != null ? title : "未命名作业",
                courseName != null ? courseName : "未知课程",
                formatDeadline(),
                description != null ? description : "",
                true); // isNew参数设置为true表示这是一个新创建的作业

        if (id != null) {
            assignment.setId(id);
        }

        if (courseId != null) {
            assignment.setCourseId(courseId);
        }

        if (status != null) {
            assignment.setStatus(status);
        } else if (completed != null && completed) {
            assignment.setStatus("完成");
        }

        return assignment;
    }

    // 格式化截止日期
    private String formatDeadline() {
        if (deadline == null) {
            return "未设定截止日期";
        }
        try {
            // 简单格式化，如果需要更复杂的格式可以使用SimpleDateFormat
            String[] parts = deadline.split("T");
            String date = parts[0];
            String time = parts.length > 1 ? parts[1].substring(0, 5) : "";
            return date + " " + time;
        } catch (Exception e) {
            return deadline;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.completed = "完成".equals(status);
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
        this.status = completed ? "完成" : "未完成";
    }
}