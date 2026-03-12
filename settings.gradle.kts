rootProject.name = "plataforma-streaming-eventos"

include(
    "event-schemas",
    "ingestion-service",
    "stream-processing-service",
    "persistence-service",
    "notification-service",
    "query-service"
)