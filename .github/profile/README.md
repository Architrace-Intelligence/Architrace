# Architrace

Architrace is a Java-based distributed monitoring prototype with an agent and control-plane.

## Repository
- `architrace-agent` (Gradle multi-module):
  - `agent`: runtime agent CLI + OTLP receiver
  - `control-plane`: Spring Boot + gRPC control-plane
  - `api`: shared API/contract module

## Core Stack
- Java 25
- gRPC / Protobuf
- Spring Boot
- Gradle Kotlin DSL
- Sonar + Snyk + Jacoco + Spotless
