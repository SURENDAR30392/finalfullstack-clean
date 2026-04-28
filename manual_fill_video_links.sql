USE full_stack_backend;

DELETE FROM video_links;

INSERT INTO video_links (
    category,
    course_id,
    course_name,
    created_at,
    instructor_name,
    topic_name,
    video_id,
    video_title,
    youtube_link
)
SELECT
    c.category,
    c.id AS course_id,
    c.title AS course_name,
    NOW() AS created_at,
    u.name AS instructor_name,
    cv.topic AS topic_name,
    cv.id AS video_id,
    cv.title AS video_title,
    cv.youtube_link
FROM courses c
JOIN users u ON u.id = c.created_by
JOIN course_videos cv ON cv.course_id = c.id
ORDER BY c.title, cv.id;

SELECT * FROM video_links ORDER BY course_name, topic_name;
