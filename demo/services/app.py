import json
import os
import threading
import time

import psycopg2
import requests
from flask import Flask
from kafka import KafkaConsumer, KafkaProducer
from opentelemetry import trace
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.instrumentation.flask import FlaskInstrumentor
from opentelemetry.instrumentation.kafka import KafkaInstrumentor
from opentelemetry.instrumentation.psycopg2 import Psycopg2Instrumentor
from opentelemetry.instrumentation.requests import RequestsInstrumentor
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor

ROLE = os.environ["ROLE"]
PORT = int(os.getenv("PORT", "8080"))
KAFKA = os.getenv("KAFKA_BOOTSTRAP", "redpanda:9092")
TOPIC = os.getenv("KAFKA_TOPIC", "order-events")
DB_DSN = os.getenv("DB_DSN", "postgresql://shop:shop@demo-db:5432/shop")

provider = TracerProvider()
provider.add_span_processor(BatchSpanProcessor(OTLPSpanExporter()))
trace.set_tracer_provider(provider)
RequestsInstrumentor().instrument()
Psycopg2Instrumentor().instrument(skip_dep_check=True)
KafkaInstrumentor().instrument()

app = Flask(__name__)
FlaskInstrumentor().instrument_app(app)


def query(sql):
    connection = psycopg2.connect(DB_DSN)
    try:
        with connection.cursor() as cursor:
            cursor.execute(sql)
            return cursor.fetchone()
    finally:
        connection.close()


def call(url):
    try:
        return requests.get(url, timeout=5).text
    except requests.RequestException as error:
        return f"unavailable ({error.__class__.__name__})"


def producer():
    for _ in range(60):
        try:
            return KafkaProducer(bootstrap_servers=KAFKA, value_serializer=lambda value: json.dumps(value).encode())
        except Exception:
            time.sleep(2)
    raise RuntimeError("Kafka is not reachable")


@app.route("/health")
def health():
    return "ok"


if ROLE == "gateway":

    @app.route("/")
    def gateway():
        return f"gateway -> {call(os.environ['ORDER_URL'] + '/orders')}"


if ROLE == "order":
    events = producer()

    @app.route("/orders")
    def orders():
        query("select count(*) from pg_catalog.pg_tables")
        inventory = call(os.environ["INVENTORY_URL"] + "/reserve")
        payment = call(os.environ["PAYMENTS_URL"] + "/")
        events.send(TOPIC, {"order": int(time.time()), "payment": payment[:20]})
        return f"order -> {inventory}"

    @app.route("/status")
    def status():
        query("select now()")
        return "order status ok"


if ROLE == "inventory":

    @app.route("/reserve")
    def reserve():
        query("select count(*) from pg_catalog.pg_class")
        order_url = os.getenv("ORDER_URL")
        suffix = "" if order_url is None else f" -> {call(order_url + '/status')}"
        return f"inventory reserved{suffix}"


if ROLE == "notification":

    def consume():
        consumer = None
        while consumer is None:
            try:
                consumer = KafkaConsumer(
                    TOPIC,
                    bootstrap_servers=KAFKA,
                    group_id=os.getenv("KAFKA_GROUP", "notification"),
                    auto_offset_reset="latest",
                )
            except Exception:
                time.sleep(2)
        for _ in consumer:
            pass

    threading.Thread(target=consume, daemon=True).start()


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=PORT)
