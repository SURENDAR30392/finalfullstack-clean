# Spring Boot Backend

## Run

```bash
mvn spring-boot:run
```

Run this command inside `LMS-FULLSTACK/backend`.

By default the backend listens on port `2026`.

If port `2026` is already in use, start it on another port:

```powershell
$env:SERVER_PORT=2027
mvn spring-boot:run
```

## Database

This project connects to:

- Database: `full_stack_backend`
- Username: `admin`
- Password: `Kavitha@123`

Tables are created automatically by Hibernate because `spring.jpa.hibernate.ddl-auto=update` is enabled.

## Main APIs

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/courses`
- `POST /api/instructor/courses`
- `PUT /api/instructor/courses/{id}`
- `DELETE /api/instructor/courses/{id}`
- `GET /api/videos/{courseId}`
- `POST /api/videos`
- `POST /api/init/load-data`

## Sample load-data payload

```json
{
  "courses": [
    {
      "title": "React JS Course",
      "description": "Frontend course from UI",
      "category": "Frontend",
      "instructor": "Telusko",
      "playlist": [
        {
          "title": "React Introduction",
          "topic": "React Introduction",
          "videoId": "react-01"
        },
        {
          "title": "Hooks",
          "topic": "Hooks",
          "youtubeLink": "https://www.youtube.com/watch?v=O6P86uwfdR0"
        }
      ]
    }
  ]
}
```
