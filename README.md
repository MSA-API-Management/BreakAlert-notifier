# BreakAlert
This is the third version of the Ndiff-Notifier (now BreakAlert). The project is written as a Spring Boot Application. API-Changes are analyzed by the OpenAI API model gpt-5-mini.

## Usage
To successfully launch the project the following environment variables need to be set beforehand:

- database

  The PostgreSQL-DB schema must be created separately and set here. The needed tables will be created automatically.

  Enter the DB-Name like this: jdbc:postgresql://localhost:port/notifierDB

- username

  Enter the DB username

- password

  Enter the DB password

- jwtExpiration

  Set JWT-Key Expiration time in seconds

- jwtKey

  Set the secret JWT-Key (HS256)

- mailName

  Enter your gmail address

- mailPassword

  Enter the generated API password for your Gmail-Account

- LLM_API_URL

  URL to a specific LLM API (such as OpenAI)

- LLM_API_KEY

  LLM API key to generate natural language change summaries

- LLM_API_MODEL

  LLM model used for handling the prompt
