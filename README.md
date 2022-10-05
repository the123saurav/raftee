# Raftee

## What is Raftee?

Raftee is a Raft implementation in Java.

## Why another Raft implementation?
Raftee is implemented on top of latest Java 19 features like:
- Virtual threads
- Structured concurrency

This allows for a system where the JVM can optimize on IO based workflows.

## Dependencies

# Building project
This project uses maven for builds.
Requirements:
- maven CLI(tested with 3.8.6)
- Java 19

# Running locally
This project uses maven shade plugin to create an uber jar.
- mvn clean verify
- java --enable-preview --add-modules jdk.incubator.concurrent -jar <PROJECT_PATH>/raftee/raftee-demo-app/target/raftee-demo-app-1.0-SNAPSHOT.jar



