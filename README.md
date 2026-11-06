# BreakAlert
This is the third version of the Ndiff-Notifier (now BreakAlert). The project is written as a Spring Boot Application. API changes are analyzed and summarized using a Large Language Model (default: OpenAI `gpt-5-mini-2025-08-07`, but configurable to any OpenAI-compatible API).

## Usage
To successfully launch the project, you can pass the following environment variables (e.g., via Docker).

### Required Variables
If these variables are not provided, the application will fail to start.

- **`DATABASE_URL`**
  The PostgreSQL connection string. The schema must be created beforehand, but the required tables will be generated automatically.
  *Example: `jdbc:postgresql://localhost:5432/notifierDB`*

- **`DATABASE_USERNAME`**
  The database username.

- **`DATABASE_PASSWORD`**
  The database password.

- **`JWT_KEY`**
  The secret key used to sign JSON Web Tokens (must be valid for HS256).

### Optional Variables
These variables have built-in fallbacks or safe degradation if left empty.

- **`JWT_EXPIRATION`**
  Set the JWT expiration time in milliseconds.

- **`MAIL_ADDRESS`**
  Your Gmail address used for sending email notifications. *(If left empty, email sending is safely disabled).*

- **`MAIL_PASSWORD`**
  The generated App Password for your Gmail account.

- **`LLM_API_URL`**
  URL to a specific LLM API.
  *(Default: `https://api.openai.com/v1/chat/completions`)*

- **`LLM_API_KEY`**
  LLM API key to generate natural language change summaries. Leave blank if using a local unauthenticated model like Ollama.

- **`LLM_API_MODEL`**
  The specific LLM model used for handling the prompt.
  *(Default: `gpt-5-mini-2025-08-07`)*