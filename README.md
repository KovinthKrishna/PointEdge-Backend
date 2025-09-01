# PointEdge Backend

A Spring Boot (Java 21) REST API for PointEdge.

## Frontend Repository
- https://github.com/KovinthKrishna/PointEdge-Frontend

## Quick Start

Prerequisites: Java 21, Maven 3.9+, MySQL 8+

- Configure application.properties (or env vars):
  ```
  spring.datasource.url=jdbc:mysql://localhost:3306/pointedge
  spring.datasource.username=your_user
  spring.datasource.password=your_pass
  jwt.secret=your_jwt_secret
  stripe.apiKey=your_stripe_secret_key
  server.port=8080
  ```

- Install & build:
  mvn clean install

- Run (dev):
  mvn spring-boot:run

- Run tests:
  mvn test

- Build JAR:
  mvn -DskipTests package

- Run JAR:
  java -jar target/point-edge-0.0.1-SNAPSHOT.jar

## Project Structure

- src/main/java/... — controllers, services, repositories, entities/models, dto, config, security
- src/main/resources/ — application.properties, static/, templates/
- src/test/java/ — unit/integration tests
- pom.xml — Maven configuration

## Tech Stack

- Spring Boot 3.4 (Web, Security, Data JPA, Mail)
- JWT (jjwt)
- Hibernate Validator (Jakarta Validation)
- ModelMapper
- Lombok
- MySQL (mysql-connector-j)
- Stripe Java SDK
- iText PDF
- Logging: SLF4J + Logback
- Testing: Spring Boot Test, Mockito
