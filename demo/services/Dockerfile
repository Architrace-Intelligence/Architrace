FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app.py .

ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317 \
    OTEL_SEMCONV_STABILITY_OPT_IN=http \
    PORT=8080

EXPOSE 8080
CMD ["python", "app.py"]
