package com.fullstack.backend.service;

import com.fullstack.backend.dto.ApiMessageResponse;
import com.fullstack.backend.dto.InstructorStudentResponse;
import com.fullstack.backend.entity.Course;
import com.fullstack.backend.entity.Enrollment;
import com.fullstack.backend.entity.User;
import com.fullstack.backend.exception.BadRequestException;
import com.fullstack.backend.repository.EnrollmentRepository;
import com.fullstack.backend.repository.VideoProgressRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final VideoProgressRepository videoProgressRepository;
    private final UserService userService;
    private final CourseService courseService;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            VideoProgressRepository videoProgressRepository,
            UserService userService,
            CourseService courseService
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.videoProgressRepository = videoProgressRepository;
        this.userService = userService;
        this.courseService = courseService;
    }

    @Transactional
    public ApiMessageResponse enrollStudent(String studentEmail, Long courseId) {
        User student = userService.getUserEntityByEmail(studentEmail);
        Course course = courseService.getCourseEntity(courseId);
        Long studentId = student.getId();

        if (!"STUDENT".equalsIgnoreCase(student.getRole())) {
            throw new BadRequestException("Only users with STUDENT role can enroll.");
        }

        if (!"APPROVED".equalsIgnoreCase(course.getApprovalStatus())) {
            throw new BadRequestException("This course is waiting for admin approval.");
        }

        if (enrollmentRepository.findByUserIdAndCourseId(studentId, courseId).isPresent()) {
            return new ApiMessageResponse("Student already enrolled.");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setUser(student);
        enrollment.setCourse(course);
        enrollmentRepository.save(enrollment);

        return new ApiMessageResponse("Enrolled Successfully");
    }

    @Transactional
    public List<InstructorStudentResponse> getInstructorStudents(Long instructorId) {
        userService.getUserEntity(instructorId);

        return enrollmentRepository.findByCourseCreatedById(instructorId)
                .stream()
                .map(enrollment -> {
                    User student = enrollment.getUser();
                    Course course = enrollment.getCourse();
                    int progressPercent = videoProgressRepository.findByUserIdAndCourseId(student.getId(), course.getId())
                            .map(progress -> progress.getProgressPercent() == null ? 0 : progress.getProgressPercent())
                            .orElse(0);

                    return new InstructorStudentResponse(
                            student.getId(),
                            student.getName(),
                            student.getEmail(),
                            course.getTitle(),
                            progressPercent
                    );
                })
                .toList();
    }
}
