# Smart Lead Qualification

A Spring Boot API that uses AI to qualify inbound messages as potential leads.

## Prerequisites and setup

- JDK 25 and Maven installed and available on your PATH.
- Groq and Hugging Face tokens with access to their configured models, if using those providers. Copy the example configuration and replace the placeholders with your tokens:

  ```sh
  cp .env-dummy .env
  ```

  Set `GROQ_TOKEN` and `HF_TOKEN` in `.env`. Keep them private: `.env` is ignored by Git; do not commit credentials.

The application tries Groq first using `qwen/qwen3.8-27b`, then Hugging Face using `meta-llama/Llama-3.3-70B-Instruct`, and finally Ollama. The Groq model is configurable in `application.properties`. An absent or blank `GROQ_TOKEN` skips the Groq request; an invalid token triggers fallback. An absent `HF_TOKEN` defaults to an empty value and does not skip the Hugging Face attempt. The example placeholders are not valid tokens.

For an optional local fallback, install Ollama, start its server and pull the configured model:

```sh
ollama serve
# In another terminal:
ollama pull llama3.2:latest
```

The fallback uses `http://localhost:11434/api/generate` and `llama3.2:latest`. If Ollama is already running, just pull the model.

## Run and test

From the project root:

```sh
mvn spring-boot:run
mvn test
```

The API listens at `http://localhost:8080/v1/api`.

## API

| Method | Route | Description |
| --- | --- | --- |
| GET | `/ai` | Generate text from the required `userInput` query parameter. |
| POST | `/messages` | Store and qualify a message; optionally create a lead. |
| GET | `/messages` | List stored messages. |
| GET | `/leads` | List qualified leads. |
| GET | `/leads/{leadId}` | Retrieve a lead by numeric ID. |

Examples (run with the application running):

```sh
curl -G 'http://localhost:8080/v1/api/ai' --data-urlencode 'userInput=Hello'

curl -i -X POST 'http://localhost:8080/v1/api/messages' \
  -H 'Content-Type: application/json' \
  -d '{"message":"I would like a demo for 100 users.","senderEmail":"alice@example.com"}'

curl 'http://localhost:8080/v1/api/messages?page=0&size=10'
curl 'http://localhost:8080/v1/api/leads?page=0&size=10'
```

`message` is required, nonblank, and at most 2,000 characters; `senderEmail` is optional and must be a valid email if supplied. A successful POST returns `201` with `message`, `analysis`, and `leadId`. If the AI qualifies the message as a lead, `leadId` is non-null and the response includes a `Location` header. Otherwise `leadId` is null and there is no `Location` header. AI results are not deterministic.

Use a real `leadId` from a successful POST or an `id` from the leads list:

```sh
LEAD_ID=42 # Replace with an ID from your response
curl "http://localhost:8080/v1/api/leads/$LEAD_ID"
```

Both list endpoints use zero-based `page` and `size` parameters; they default to 20 items sorted by `createdAt` descending, and the maximum page size is 25. Responses include `content` and `page` metadata. You can override sorting with, for example, `sort=createdAt,asc`.

## Web UI

A server-rendered interface (Thymeleaf and Bootstrap 5, loaded from a CDN) sits next to the API. Open these pages in a browser:

| Page | URL |
| --- | --- |
| Send a message | `http://localhost:8080/v1/api/ui/messages/new` |
| Stored messages | `http://localhost:8080/v1/api/ui/messages` |
| Leads | `http://localhost:8080/v1/api/ui/leads` |

On the list pages, click a column header to sort by it; click again to reverse the order. The sort is kept when you change page. `priority` sorts alphabetically (HIGH, LOW, MEDIUM), not by severity. The web pages use the same services as the API, so they follow the same rules. A message that the AI cannot analyse is still saved; the list page then shows a warning. Templates are in `src/main/resources/templates`.

## How it works and errors

`LeadIntakeService` saves each message before requesting AI qualification. It creates a lead ticket only when the analysis identifies a lead; the saved message remains even if qualification fails. Data is stored in an in-memory H2 database and is lost on restart.

The AI service tries Groq, Hugging Face, then Ollama on provider failure. `GlobalExceptionHandler` returns problem details for common failures: `400` for invalid input or malformed JSON, `404` for a missing lead, `503` when all AI providers are unavailable, and `502` when qualification receives unparseable AI output. Unsupported methods return `405`, and unexpected errors return `500`.