FROM eclipse-temurin:25-jre-alpine@sha256:3c0a9084927a221ccd1d007fcaf614465672c0af37aaa834c5184483afe56d61

ARG VERSION=dev
LABEL org.opencontainers.image.title="architrace-control-plane" \
      org.opencontainers.image.description="Architrace control plane: ingests agent streams, stores the topology, serves the Query API and the UI" \
      org.opencontainers.image.source="https://github.com/Architrace-Intelligence/Architrace" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.version="${VERSION}"

RUN addgroup -S architrace && adduser -S -G architrace -H -s /sbin/nologin architrace

WORKDIR /app
COPY build/libs/architrace-control-plane-*.jar /app/architrace-control-plane.jar

ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

USER architrace
EXPOSE 8085 9090

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8085/actuator/health || exit 1

ENTRYPOINT ["java", "--enable-preview", "-jar", "/app/architrace-control-plane.jar"]
