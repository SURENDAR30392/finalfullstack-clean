USE full_stack_backend;

ALTER TABLE video_progress
    ADD COLUMN IF NOT EXISTS student_name VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS instructor_name VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS course_name VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS enrolled_courses_count INT NULL,
    ADD COLUMN IF NOT EXISTS total_videos_in_course INT NULL,
    ADD COLUMN IF NOT EXISTS completed_videos_in_course INT NULL,
    ADD COLUMN IF NOT EXISTS completed_video_ids_data VARCHAR(4000) NULL;

ALTER TABLE video_progress
    MODIFY COLUMN video_id BIGINT NULL;
