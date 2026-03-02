FROM python:3.11-slim

WORKDIR /app

COPY . .

RUN pip install flask requests \
    opentelemetry-api \
    opentelemetry-sdk \
    opentelemetry-exporter-otlp \
    opentelemetry-instrumentation-flask \
    opentelemetry-instrumentation-requests

CMD ["python", "app.py"]