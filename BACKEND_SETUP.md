# Spring Boot Backend Setup Guide

## Overview

The backend **MUST** run as a separate project. This guide will help you set up a Spring Boot backend that works with your Android app.

## Project Structure

You have two options:

### Option 1: Create Backend in Separate Folder (Recommended)
Create a new Spring Boot project in a separate folder:
```
CommunitySwapHub2/          (Android App - Current)
CommunitySwapHubBackend/    (Spring Boot - New)
```

### Option 2: Create Backend in Same Repository
Create a `backend` folder in your current project:
```
CommunitySwapHub2/
  ├── app/                 (Android App)
  └── backend/              (Spring Boot - New)
```

## Quick Start

1. **Create Spring Boot Project**
   - Use Spring Initializr: https://start.spring.io/
   - Or use the files I'll create for you

2. **Run Backend**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   # Or
   ./gradlew bootRun
   ```

3. **Run Android App**
   - Backend should be on `http://localhost:8080`
   - Android app connects via `http://10.0.2.2:8080` (emulator)

## Required Dependencies

- Spring Boot Web
- Spring Data JPA
- PostgreSQL Driver (or H2 for testing)
- Spring Security (for authentication)
- JWT (for tokens)

Would you like me to create the complete Spring Boot backend project structure?










