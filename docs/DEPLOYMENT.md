# AeroSentinel — Deployment & Operations Guide

---

## 1. Local Development Stack (Docker Compose)

The fastest way to spin up the entire multi-tier stack locally is via `docker-compose.yml`:

```bash
# 1. Ensure Docker Desktop / daemon is active
docker compose up --build -d

# 2. View running services and logs
docker compose ps
docker compose logs -f backend
```

Ports:
- **Frontend**: `http://localhost:3000` (Nginx serving production bundle)
- **Spring Boot Backend**: `http://localhost:8080` (API & Actuator health at `/actuator/health`)
- **FastAPI AI Service**: `http://localhost:8000` (OpenAPI Swagger docs at `/docs`)
- **PostgreSQL / PostGIS**: `localhost:5432`

---

## 2. Production Cloud Deployment Blueprint

```text
       Internet
          │
          ▼
   [Cloud CDN / WAF]
          │
    HTTPS:443
          ▼
   [Reverse Proxy / Nginx]
          │
    ┌─────┴─────────────────────────┐
    ▼                               ▼
[Static Web Hosting]       [Internal Load Balancer]
React + Vite Bundle                 │
                                    ▼
                         [Spring Boot Containers]
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
        [FastAPI AI Containers]            [PostgreSQL + PostGIS]
                    │                            (Cloud SQL)
                    ▼
           [Google Gemini API]
```

### Healthcheck Endpoints
- Backend: `GET /actuator/health`
- AI Service: `GET /health`
