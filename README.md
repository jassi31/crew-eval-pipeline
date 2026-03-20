# AI Agent Evaluation Pipeline

Spring Boot prototype for ingesting multi-turn conversation logs, running modular evaluations, integrating feedback, and generating improvement suggestions for prompts, tools, and evaluators.

## Architecture Overview

- Modular monolith built with Java 17, Spring Boot, Maven, and PostgreSQL.
- Conversation ingestion persists both the raw payload and normalized relational tables.
- Evaluation is asynchronous:
  - `POST /api/v1/conversations` and `POST /api/v1/conversations/batch` persist data and enqueue `evaluation_jobs`.
  - A scheduled worker claims queued jobs and runs the evaluator stack.
- Evaluators in v1:
  - `LlmJudgeEvaluator` using a pluggable `JudgeProvider` with a mock default.
  - `ToolCallEvaluator` using `tool-registry.yml`.
  - `CoherenceEvaluator` for context retention and consistency.
  - `HeuristicEvaluator` for latency, payload quality, and structural checks.
- Feedback integration:
  - Supports user ratings, ops reviews, and multiple annotations.
  - Applies weighted voting using annotator weights and annotation confidence.
- Improvement loop:
  - Repeated failures create prompt, tool, or evaluator suggestions.
  - Calibration snapshots track automated-human alignment over time.

## Project Structure

```text
src/main/java/com/crew/evalpipeline
├── api           # REST controllers, DTOs, exception handling
├── config        # OpenAPI, app properties, clock
├── conversation  # Conversation entities, repositories, ingestion service
├── evaluation    # Jobs, evaluators, orchestration
├── feedback      # Feedback entities and consensus logic
├── meta          # Calibration snapshots and agreement reports
├── shared        # Auditing and enums
└── suggestion    # Suggestion persistence and generation
```

## Running Locally

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker (for PostgreSQL and optional app container)

### Option 1: Run against local PostgreSQL

1. Start PostgreSQL using Docker Compose:

   ```bash
   docker compose up -d postgres
   ```

2. Run the application:

   ```bash
   mvn spring-boot:run
   ```

3. Open Swagger UI:

   [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Option 2: Run the full stack in Docker

```bash
docker compose up --build
```

## Configuration

Important environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `OPENAI_API_KEY` optional, only used if `app.judge.provider=openai`
- `OPENAI_BASE_URL`
- `OPENAI_MODEL`

Key application settings live in [`src/main/resources/application.yml`](/Users/jassi31/Downloads/crew-eval-pipeline/src/main/resources/application.yml).

## API Endpoints

- `POST /api/v1/conversations`
- `POST /api/v1/conversations/batch`
- `GET /api/v1/conversations/{conversationId}`
- `POST /api/v1/conversations/{conversationId}/feedback`
- `POST /api/v1/evaluations/{conversationId}/run`
- `GET /api/v1/evaluations/{conversationId}`
- `GET /api/v1/evaluations`
- `GET /api/v1/suggestions`
- `GET /api/v1/meta/calibration`
- `GET /api/v1/meta/agreements`
- `GET /actuator/health`

## Sample Data

Seed payloads live under [`src/main/resources/demo`](/Users/jassi31/Downloads/crew-eval-pipeline/src/main/resources/demo).

- `sample-conversations.jsonl`: batch ingestion examples covering tool regression, context loss, and annotation disagreement.

## Scaling Strategy

- PostgreSQL-backed queue table is enough for the prototype and can scale horizontally with multiple app instances and pessimistic row locking.
- JSONB storage preserves full payload fidelity while normalized tables support search and reporting.
- Suggested production upgrades:
  - move queueing to Kafka/SQS
  - isolate workers from API nodes
  - add evaluator versioning and prompt version history
  - introduce metrics and alerting to track regression spikes

## Trade-offs

- Chose a single service over multiple services to keep delivery scope realistic.
- Used a mock LLM judge by default to keep the prototype runnable without external keys.
- Swagger is the demo surface instead of a custom UI so the backend can stay deeper and more credible.

## Testing

Run the test suite:

```bash
mvn test
```

Current test coverage focuses on evaluator logic, feedback consensus, suggestion generation, and controller-level ingestion behavior.
