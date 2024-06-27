# Ndiff-NotifierV2
This is the second version of the Ndiff-Notifier. The project is written as a Spring Boot Application. API-Changes are analyzed by ChatGPT.

## Usage
To successfully launch the project the following environment variables need to be set beforehand:

- database

  The PostgreSQL-DB schema must be created separately and set here. The needed tables will be created automatically.

  Enter the DB-Name like this: jdbc:postgresql://localhost:port/notifierDB

- username

  Enter the DB username

- password

  Enter the DB password

- GPT_API_KEY

  Enter the API-Key from OpenAI

- jwtExpiration

  Set JWT-Key Expiration time in seconds

- jwtKey

  Set the secret JWT-Key (HS256)

- mailName

  Enter your gmail address

- mailPassword

  Enter the generated API password for your Gmail-Account

  
