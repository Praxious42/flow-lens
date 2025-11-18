# 8. Deployment & Operations

This chapter describes how FlowLens is deployed and operated in a typical
developer setup. The focus is on:

- Running the **backend service** locally (with and without Docker).
- Installing and configuring the **IntelliJ plugin**.
- Basic **monitoring, logging, and troubleshooting**.

The primary target is a **single-developer environment** used for demos and
evaluation.

---

## 8.1 Environments

For the initial version, FlowLens assumes a simple environment model:

- **Local Dev / Demo**
    - IntelliJ IDEA with the FlowLens plugin installed.
    - Backend running on the same machine:
        - As a plain Spring Boot process, and/or
        - In a Docker container (optionally via `docker-compose`).

Future versions could introduce additional environments (e.g. shared demo
backend), but they are out of scope for this iteration.

---

## 8.2 Backend Service Deployment

### 8.2.1 Running as a Spring Boot Application

**Prerequisites**

- Java (e.g. JDK 17+).
- Build tool (Maven/Gradle, as chosen in the project).

**Build**

```bash
# Example with Maven Wrapper
./mvnw clean package
# or with Gradle Wrapper
./gradlew clean bootJar
