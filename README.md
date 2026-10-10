# Geo-Track Backend

This is the Spring Boot (Java 21) backend for the Geo-Track HR platform.

## Requirements
- **Java 21**
- **Maven**
- **MySQL Server**

## Local Development Setup

1. **Database Configuration**
   Ensure you have a MySQL server running locally. Create a database named `geotrack` (or let Hibernate auto-create it).
   The local database credentials can be modified in `src/main/resources/application-dev.properties`:
   ```properties
   spring.datasource.username=root
   spring.datasource.password=Root
   ```

2. **Run the Application Locally**
   You can run the backend server using the Maven wrapper or your local Maven installation:
   ```bash
   mvn spring-boot:run
   ```
   The backend will start on `http://localhost:8085` by default.

3. **API Documentation (Postman)**
   The project includes a Postman collection (`GeoTrack_API.postman_collection.json`) in the parent directory to test API endpoints.

## Building for Production

To create an executable JAR file, run:
```bash
mvn clean package -DskipTests
```
The compiled JAR will be located in the `target/` directory.

## Deployment (Render)

This project is configured for deployment on Render as a "Web Service".
- A `Dockerfile` is provided for containerized builds.
- Configure your deployment to use the `prod` profile by setting the `SPRING_PROFILES_ACTIVE` environment variable to `prod`.
- Make sure to set the `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, and `JWT_SECRET` environment variables on Render.
