package com.fullstack.backend.entity;

import jakarta.persistence.Column;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_progress")
public class VideoProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"courses", "enrollments", "videoProgress", "password"})
    private User user;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({"videos", "enrollments"})
    private Course course;

    @ManyToOne
    @JoinColumn(name = "video_id")
    @JsonIgnoreProperties({"course", "videoProgress"})
    private CourseVideo video;

    @Column(length = 255)
    private String studentName;

    @Column(length = 255)
    private String instructorName;

    @Column(length = 255)
    private String courseName;

    private Integer enrolledCoursesCount;

    private Integer totalVideosInCourse;

    private Integer completedVideosInCourse;

    @Column(length = 4000)
    private String completedVideoIdsData;

    private Integer progressPercent;

    private Boolean completed;

    private LocalDateTime lastUpdatedAt;

    @PrePersist
    @PreUpdate
    public void onSave() {
        if (progressPercent == null) {
            progressPercent = 0;
        }
        if (enrolledCoursesCount == null) {
            enrolledCoursesCount = 0;
        }
        if (totalVideosInCourse == null) {
            totalVideosInCourse = 0;
        }
        if (completedVideosInCourse == null) {
            completedVideosInCourse = 0;
        }
        if (completedVideoIdsData == null) {
            completedVideoIdsData = "";
        }
        if (completed == null) {
            completed = false;
        }
        lastUpdatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public CourseVideo getVideo() {
        return video;
    }

    public void setVideo(CourseVideo video) {
        this.video = video;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Integer getEnrolledCoursesCount() {
        return enrolledCoursesCount;
    }

    public void setEnrolledCoursesCount(Integer enrolledCoursesCount) {
        this.enrolledCoursesCount = enrolledCoursesCount;
    }

    public Integer getTotalVideosInCourse() {
        return totalVideosInCourse;
    }

    public void setTotalVideosInCourse(Integer totalVideosInCourse) {
        this.totalVideosInCourse = totalVideosInCourse;
    }

    public Integer getCompletedVideosInCourse() {
        return completedVideosInCourse;
    }

    public void setCompletedVideosInCourse(Integer completedVideosInCourse) {
        this.completedVideosInCourse = completedVideosInCourse;
    }

    public String getCompletedVideoIdsData() {
        return completedVideoIdsData;
    }

    public void setCompletedVideoIdsData(String completedVideoIdsData) {
        this.completedVideoIdsData = completedVideoIdsData;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
