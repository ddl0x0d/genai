# Chat

[LangChain4j](https://docs.langchain4j.dev/)-based console chatbot with simple
[RAG](https://learnprompting.org/docs/retrieval_augmented_generation/introduction) capabilities.

## Prerequisites

- ☕ [Java 21](https://adoptium.net/)
- 🐋 [Docker](https://docs.docker.com/engine/install/)

## Build

```shell
../gradlew :chat:build
```

## Run

There are several possible options:

1. Run with IDE: just run [`main.kt`](src/main/kotlin/com/github/ddl0x0d/genai/chat/main.kt).

2. Run with Java:

   ```shell
   java -jar build/libs/chat-all.jar --help
   ```

3. Run with Gradle:

   ```shell
   ../gradlew :chat:run --args='--help'
   ```

4. Run in Docker:

   ```shell
   docker compose run --rm chat --help
   ```

## Usage

All the run options above will output the same help message:

```
Usage: chat [<options>] <command> [<args>]...

Options:
  -c, --config=<path>  Specify configuration file
  -h, --help           Show this message and exit

Commands:
  chat    Start chatting with AI Assistant
  config  Print default config to stdout
  ingest  Ingest document(s) to RAG store
```

Running the app without any options is identical to running its command `chat`:

```
Start chatting with AI Assistant.
You can use the following commands:

- /help - print this help message
- /ingest <path> - ingest document(s)
- /exit - finish conversation

User> 
```

Try ingesting this `README.md` if you don't have any other suitable documents at hand!

```
User> /ingest README.md

Ingestion completed

User> How do I build the chat module?

AI> To build the chat module, follow these steps:
...
```
