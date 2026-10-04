FROM eclipse-temurin:25-jre-alpine@sha256:3c0a9084927a221ccd1d007fcaf614465672c0af37aaa834c5184483afe56d61

ARG VERSION=dev
LABEL org.opencontainers.image.title="architrace-agent" \
      org.opencontainers.image.description="Architrace agent: receives OTLP traces and streams the service graph to the control plane" \
      org.opencontainers.image.source="https://github.com/Architrace-Intelligence/Architrace" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.version="${VERSION}"

RUN addgroup -S architrace && adduser -S -G architrace -H -s /sbin/nologin architrace

WORKDIR /app
COPY build/libs/architrace-agent-*-all.jar /app/architrace-agent.jar

ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    ARCHITRACE_OTLP_PORT=4317

USER architrace
EXPOSE 4317

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD nc -z 127.0.0.1 "$ARCHITRACE_OTLP_PORT" || exit 1

ENTRYPOINT ["java", "--enable-preview", "-jar", "/app/architrace-agent.jar"]
CMD ["run", "--config", "/config/architrace-agent.yaml"]
