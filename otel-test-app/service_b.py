from flask import Flask
import requests
import os

from opentelemetry import trace
from opentelemetry.sdk.resources import Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.instrumentation.flask import FlaskInstrumentor
from opentelemetry.instrumentation.requests import RequestsInstrumentor

service_name = "service-b"
span_metadata = {
    "environment": os.getenv("ENVIRONMENT", "test"),
    "domainId": os.getenv("DOMAIN_ID", "architrace"),
    "serviceName": service_name,
    "cluster": os.getenv("CLUSTER", "otel-test-cluster"),
    "namespace": os.getenv("NAMESPACE", "otel-test-app"),
}

resource = Resource(attributes={
    "service.name": service_name,
    **span_metadata,
})

trace.set_tracer_provider(TracerProvider(resource=resource))
tracer = trace.get_tracer(__name__)

otlp_exporter = OTLPSpanExporter(
    endpoint="http://otel-collector:4317",
    insecure=True,
)

span_processor = BatchSpanProcessor(otlp_exporter)
trace.get_tracer_provider().add_span_processor(span_processor)

app = Flask(__name__)
FlaskInstrumentor().instrument_app(app)
RequestsInstrumentor().instrument()

@app.route("/")
def call_c():
    current_span = trace.get_current_span()
    for key, value in span_metadata.items():
        current_span.set_attribute(key, value)

    response = requests.get("http://service-c:8082/")
    return f"B -> {response.text}"

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8081)
