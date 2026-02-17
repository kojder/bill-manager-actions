# Bill-Manager

## 1. Project Objective

Build an application for automated bill analysis (images/PDF) using LLMs (Groq) and test remote, automated Code Review mechanisms provided by Claude Code Actions (`anthropics/claude-code-action`). The project serves as a testing ground for context management (CLAUDE.md review guidelines) for AI within the Continuous Integration (CI) pipeline.

## 2. Architecture & Technologies

### Backend (Spring Boot)

- **Runtime**: Java 17, Spring Boot 3.5.x
- **Spring AI** (OpenAI Starter): Communication with Groq API (OpenAI protocol compatibility)
- **Image Processing**: imgscalr or java.awt for optimizing image dimensions before LLM submission
- **Persistence**: Integration with a local Supabase instance (PostgreSQL + PostgREST)
- **Principles**: SOLID, Clean Code, Java 17 best practices

### Frontend (Web)

- Simple "Localhost First" interface
- Drag & Drop functionality for PDF files and images (JPG/PNG)

### AI & Data Infrastructure

- **LLM**: Groq (Llama 3 / Mixtral models) – content analysis and data extraction to JSON
- **Storage**: Supabase (local Docker container)

## 3. Application Workflow

1. **Upload**: User uploads a file via the browser
2. **Pre-processing**: Backend verifies MIME type and scales the image to optimal dimensions (e.g., 1200px width) to save tokens/costs
3. **AI Analysis**:
   - Utilize `ChatModel` interface (Spring AI)
   - Prompt enforcing Structured Output (extraction of: items, unit prices, total amount, category tags)
4. **Storage**: Save raw JSON output to Supabase

## 4. Automated Code Review Strategy (Claude Code Actions)

The primary goal is to configure the repository so that Claude acts as a Senior Developer, ignoring trivial formatting issues.

### A. Noise Filtering (CI Pipeline)

To ensure Claude does not focus on syntax, the GitHub Actions pipeline must include:

- **Linter/Checkstyle**: Automated formatting check. If Checkstyle fails, the pipeline terminates before invoking Claude
- **SonarQube**: Static analysis for technical debt and security
- **Unit Tests**: Requirement of min. 80% coverage for business logic (scaling, JSON mapping)

### B. Context Management (`CLAUDE.md`)

Claude Code Action reads `CLAUDE.md` automatically from the repository root. Review instructions include:

- **Scope**: Skip formatting issues (handled by linter). Focus on logical errors, security vulnerabilities, and API performance
- **Architecture**: Require Java Records for DTOs and Spring AI interfaces instead of direct HTTP clients
- **Security**: Ensure Groq/Supabase API keys are neither logged nor hardcoded

### C. Path-specific Review Rules

Path-specific review rules are defined in `CLAUDE.md` under "Path-Specific Review Rules":

| Path Pattern | Review Focus |
|-------------|--------------|
| `**/ai/**` | Timeout, Retry (Exponential Backoff), token limits |
| `**/upload/**` | MIME validation, file size limits, path traversal |
| `**/config/**` | Secrets exposure, hardcoded values, env separation |

## 5. start.spring.io Configuration

- **Project**: Maven
- **Language**: Java 17
- **Spring Boot**: 3.5.x
- **Dependencies**:
  - Lombok
  - Spring Boot DevTools
  - Spring Web
  - OpenAI (Spring AI)
  - Validation

## 6. Milestones

1. **Milestone 1**: Repository initialization, Checkstyle configuration, and `CLAUDE.md` review guidelines setup
2. **Milestone 2**: Implementation of Upload and Image Processing modules (SOLID). First PR to test Claude's response to logical errors vs. style issues
3. **Milestone 3**: Spring AI (Groq) integration and Structured Output implementation
4. **Milestone 4**: Supabase integration and finalization of CI/CD pipeline with "Gated Code Review"
