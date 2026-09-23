# AeroSentinel — Security & Governance Architecture

---

## 1. Zero-Trust API & Key Isolation

1. **Server-Side API Key Confinement**:
   - Google Gemini, NASA FIRMS, and weather provider API credentials reside strictly in backend/AI service environment variables.
   - Frontend client bundles contain zero private tokens or credentials.
2. **Database Boundary Protection**:
   - The PostgreSQL + PostGIS database is bound strictly to the private internal container network.
   - All client queries must pass through Spring Boot controllers with parameter validation.
3. **CORS & Origin Policies**:
   - Spring Boot configures explicit Cross-Origin Resource Sharing (`CorsConfig.java`) allowing only trusted frontend origins (e.g. `http://localhost:3000`, `http://localhost:5173`).

---

## 2. Authentication & Authorization (RBAC)

Spring Security is configured with stateless JSON Web Token (JWT) verification:
- **CITIZEN**: Can view public map dashboards, forecast cards, and submit crowdsourced reports.
- **ANALYST**: Access to feature exploration, satellite comparison, correlation analysis, and model metrics.
- **AUTHORITY**: Access to real-time incident queues, alert acknowledgement, team assignment, and action logging.
- **ADMIN**: User account management, city configuration, and system observability.

---

## 3. Data Ingestion & Sanitization

- **Input Validation**: All REST inputs are sanitized using Spring Validation (`@Valid`, `@NotNull`, `@Size`).
- **File Upload Security**: Citizen image uploads are checked for valid extensions, maximum payload size (10MB), and content MIME headers to prevent arbitrary file execution.
