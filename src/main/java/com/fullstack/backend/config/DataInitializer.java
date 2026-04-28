package com.fullstack.backend.config;

import com.fullstack.backend.service.CourseService;
import com.fullstack.backend.service.UserService;
import com.fullstack.backend.service.VideoLinkService;
import com.fullstack.backend.service.VideoProgressService;
import org.springframework.dao.DataAccessException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner seedDatabase(
            CourseService courseService,
            VideoLinkService videoLinkService,
            VideoProgressService videoProgressService,
            UserService userService,
            JdbcTemplate jdbcTemplate
    ) {
        return args -> {
            ensureVideoProgressColumns(jdbcTemplate);
            ensureEmailAuthTable(jdbcTemplate);
            ensureImageUrlTable(jdbcTemplate);
            ensureAssignmentLinkTable(jdbcTemplate);
            ensureProfileUrlTable(jdbcTemplate);
            userService.cleanupExpiredOtps();
            courseService.seedInitialData();
            videoLinkService.syncAllFromCourses();
            videoProgressService.backfillExistingProgressSummaries();
        };
    }

    private void ensureEmailAuthTable(JdbcTemplate jdbcTemplate) {
        runSqlQuietly(jdbcTemplate, """
                CREATE TABLE IF NOT EXISTS EMAILS (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    name VARCHAR(255) NOT NULL,
                    email VARCHAR(255) NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    role VARCHAR(255) NOT NULL,
                    provider VARCHAR(255) NOT NULL DEFAULT 'LOCAL',
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY UK_EMAILS_email (email)
                )
                """);

        runSqlQuietly(jdbcTemplate, "ALTER TABLE users ADD COLUMN provider VARCHAR(255) NULL");
        runSqlQuietly(jdbcTemplate, "ALTER TABLE users ADD COLUMN otp VARCHAR(20) NULL");
        runSqlQuietly(jdbcTemplate, "ALTER TABLE users ADD COLUMN otp_expiry DATETIME(6) NULL");
        runSqlQuietly(jdbcTemplate, "ALTER TABLE users ADD COLUMN verified BIT(1) NULL");
        runSqlQuietly(jdbcTemplate, "UPDATE users SET provider = 'LOCAL' WHERE provider IS NULL OR provider = ''");
        runSqlQuietly(jdbcTemplate, "UPDATE users SET verified = 0 WHERE verified IS NULL");
        runSqlQuietly(jdbcTemplate, "ALTER TABLE EMAILS ADD COLUMN otp_code VARCHAR(20) NULL");
        runSqlQuietly(jdbcTemplate, "ALTER TABLE EMAILS ADD COLUMN otp_expires_at DATETIME(6) NULL");
        runSqlQuietly(jdbcTemplate, "ALTER TABLE EMAILS ADD COLUMN otp_verified_until DATETIME(6) NULL");
        runSqlQuietly(jdbcTemplate, """
                UPDATE users u
                JOIN EMAILS e ON e.email = u.email
                SET
                    u.otp = e.otp_code,
                    u.otp_expiry = e.otp_expires_at
                WHERE e.otp_code IS NOT NULL OR e.otp_expires_at IS NOT NULL
                """);
        runSqlQuietly(jdbcTemplate, """
                INSERT INTO EMAILS (name, email, password, role, provider, created_at)
                SELECT u.name, u.email, u.password, u.role, COALESCE(NULLIF(u.provider, ''), 'LOCAL'), u.created_at
                FROM users u
                LEFT JOIN EMAILS e ON e.email = u.email
                WHERE e.id IS NULL
                """);
        runSqlQuietly(jdbcTemplate, "DROP TABLE IF EXISTS email_otp_tokens");
    }

    private void ensureVideoProgressColumns(JdbcTemplate jdbcTemplate) {
        addColumnIfMissing(jdbcTemplate, "ALTER TABLE courses ADD COLUMN approval_status VARCHAR(255) NULL");
        runSqlQuietly(jdbcTemplate, "UPDATE courses SET approval_status = 'APPROVED' WHERE approval_status IS NULL OR approval_status = ''");
        runSqlQuietly(jdbcTemplate, """
                UPDATE courses c
                SET c.approval_status = 'APPROVED'
                WHERE c.approval_status = 'PENDING'
                  AND EXISTS (
                      SELECT 1
                      FROM enrollments e
                      WHERE e.course_id = c.id
                  )
                """);
        addColumnIfMissing(jdbcTemplate, "ALTER TABLE course_videos ADD COLUMN approval_status VARCHAR(255) NULL");
        addColumnIfMissing(jdbcTemplate, "ALTER TABLE course_videos ADD COLUMN assignment_url VARCHAR(2000) NULL");
        runSqlQuietly(jdbcTemplate, "UPDATE course_videos SET approval_status = 'APPROVED' WHERE approval_status IS NULL OR approval_status = ''");
        addColumnIfMissing(jdbcTemplate, "ALTER TABLE video_progress ADD COLUMN student_name VARCHAR(255) NULL");
        addColumnIfMissing(jdbcTemplate, "ALTER TABLE video_progress ADD COLUMN instructor_name VARCHAR(255) NULL");
        addColumnIfMissing(jdbcTemplate, "ALTER TABLE video_progress ADD COLUMN course_name VARCHAR(255) NULL");
        addColumnIfMissing(jdbcTemplate, "ALTER TABLE video_progress ADD COLUMN enrolled_courses_count INT NULL");
        addColumnIfMissing(jdbcTemplate, "ALTER TABLE video_progress ADD COLUMN total_videos_in_course INT NULL");
        addColumnIfMissing(jdbcTemplate, "ALTER TABLE video_progress ADD COLUMN completed_videos_in_course INT NULL");
        addColumnIfMissing(jdbcTemplate, "ALTER TABLE video_progress ADD COLUMN completed_video_ids_data VARCHAR(4000) NULL");
        runSqlQuietly(jdbcTemplate, "ALTER TABLE video_progress MODIFY COLUMN video_id BIGINT NULL");

        runSqlQuietly(jdbcTemplate, """
                UPDATE video_progress vp
                JOIN users student ON student.id = vp.user_id
                JOIN courses course ON course.id = vp.course_id
                JOIN users instructor ON instructor.id = course.created_by
                SET
                    vp.student_name = student.name,
                    vp.instructor_name = instructor.name,
                    vp.course_name = course.title,
                    vp.enrolled_courses_count = (
                        SELECT COUNT(*)
                        FROM enrollments enrollment
                        WHERE enrollment.user_id = vp.user_id
                    ),
                    vp.total_videos_in_course = (
                        SELECT COUNT(*)
                        FROM course_videos course_video
                        WHERE course_video.course_id = vp.course_id
                    ),
                    vp.completed_videos_in_course = CASE
                        WHEN vp.completed = 1 THEN GREATEST(COALESCE(vp.completed_videos_in_course, 0), 1)
                        ELSE COALESCE(vp.completed_videos_in_course, 0)
                    END,
                    vp.completed_video_ids_data = CASE
                        WHEN (vp.completed_video_ids_data IS NULL OR vp.completed_video_ids_data = '')
                             AND vp.completed = 1
                             AND vp.video_id IS NOT NULL
                        THEN CAST(vp.video_id AS CHAR)
                        ELSE COALESCE(vp.completed_video_ids_data, '')
                    END
                """);
    }

    private void ensureImageUrlTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS image_url (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    course_id BIGINT NOT NULL,
                    course_name VARCHAR(255) NOT NULL,
                    image_url LONGTEXT NOT NULL,
                    uploaded_by_id BIGINT NOT NULL,
                    uploaded_by_name VARCHAR(255) NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id)
                )
                """);
        log.info("Ensured image_url table exists in schema full_stack_backend.");
    }

    private void ensureAssignmentLinkTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS assignment_links (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    course_id BIGINT NOT NULL,
                    course_name VARCHAR(255) NOT NULL,
                    video_id BIGINT NOT NULL,
                    video_title VARCHAR(255) NOT NULL,
                    assignment_url VARCHAR(2000) NOT NULL,
                    instructor_id BIGINT NOT NULL,
                    instructor_name VARCHAR(255) NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id)
                )
                """);
        log.info("Ensured assignment_links table exists in schema full_stack_backend.");
    }

    private void ensureProfileUrlTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS PROFILE_URL (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    user_name VARCHAR(255) NOT NULL,
                    user_role VARCHAR(255) NOT NULL,
                    profile_url VARCHAR(2000) NOT NULL,
                    created_at DATETIME(6) NOT NULL,
                    PRIMARY KEY (id)
                )
                """);
        log.info("Ensured PROFILE_URL table exists in schema full_stack_backend.");
    }

    private void addColumnIfMissing(JdbcTemplate jdbcTemplate, String sql) {
        runSqlQuietly(jdbcTemplate, sql);
    }

    private void runSqlQuietly(JdbcTemplate jdbcTemplate, String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (DataAccessException ignored) {
            // Ignore duplicate-column / already-updated cases so startup stays idempotent.
        }
    }
}
