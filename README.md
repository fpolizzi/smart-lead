# Smart Lead Qualification

A Spring Boot API that uses AI to qualify inbound messages as potential leads.

## Prerequisites and setup

- JDK 25 and Maven installed and available on your PATH.
- A Hugging Face token with access to the configured model. Copy the example configuration and replace the placeholder with your token:

  ```sh
  cp .env-dummy .env
  ```

  Set `HF_TOKEN` in `.env`. Keep it private: `.env` is ignored by Git; do not commit credentials.

The application tries Hugging Face first, using `meta-llama/Llama-3.3-70B-Instruct`, then Ollama if the provider fails. An absent `HF_TOKEN` defaults to an empty value; the example placeholder is not a valid token. Neither skips the Hugging Face attempt.

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

## How it works and errors

`LeadIntakeService` saves each message before requesting AI qualification. It creates a lead ticket only when the analysis identifies a lead; the saved message remains even if qualification fails. Data is stored in an in-memory H2 database and is lost on restart.

The AI service tries Hugging Face first and falls back to Ollama on provider failure. `GlobalExceptionHandler` returns problem details for common failures: `400` for invalid input or malformed JSON, `404` for a missing lead, `503` when all AI providers are unavailable, and `502` when qualification receives unparseable AI output. Unsupported methods return `405`, and unexpected errors return `500`.