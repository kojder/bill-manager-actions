# Bill Manager - Setup Instructions

## Prerequisites

- Java 17
- Maven 3.9+ (wrapper included)
- Groq API key

## Quick Start

### 1. Get Groq API Key

1. Go to [Groq Console](https://console.groq.com/keys)
2. Sign in or create an account
3. Click **"Create API Key"**
4. Copy the generated key

### 2. Configure Environment

Edit the `.env` file and replace the placeholder:

```bash
GROQ_API_KEY=gsk_your_actual_key_here
```

The `.env` file is already in `.gitignore` - your key will **not** be committed to Git.

### 3. Run the Application

```bash
# Compile
./mvnw clean compile

# Run tests
./mvnw test

# Start application
./mvnw spring-boot:run
```

The application will start at `http://localhost:8080`

### 4. Verify

Check health endpoint:
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

## Configuration

All configuration is in [application.properties](src/main/resources/application.properties):

- **Groq API**: base URL, model, temperature
- **Actuator**: health and info endpoints

For environment-specific config, create `application-dev.properties` or use environment variables.

## Troubleshoties

### Error: "OpenAI API key must be set"

Make sure:
1. `.env` file exists in project root
2. `GROQ_API_KEY` is set in `.env`
3. spring-dotenv dependency is in `pom.xml` (already included)

### Port 8080 already in use

Change port in `application.properties`:
```properties
server.port=8081
```
