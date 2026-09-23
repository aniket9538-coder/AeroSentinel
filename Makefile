.PHONY: all setup dev backend frontend ai test build docker-up docker-down clean

# Default target
all: dev

# Setup all environments
setup:
	@echo "Setting up AeroSentinel environment..."
	@cd frontend && npm install
	@cd ai-service && pip install -r requirements.txt

# Start all development services locally
dev:
	@echo "To run all services, start backend, frontend, and ai-service in separate terminals:"
	@echo "  make backend"
	@echo "  make frontend"
	@echo "  make ai"

# Start Spring Boot backend
backend:
	@echo "Starting Spring Boot Backend..."
	@cd backend && ./mvnw spring-boot:run || cd backend && mvn spring-boot:run

# Start React Frontend
frontend:
	@echo "Starting React Frontend..."
	@cd frontend && npm run dev

# Start FastAPI AI Service
ai:
	@echo "Starting Python FastAPI AI Service..."
	@cd ai-service && uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

# Run all tests
test:
	@echo "Running backend tests..."
	@cd backend && mvn test || cd backend && ./mvnw test
	@echo "Running frontend tests / build..."
	@cd frontend && npm run build
	@echo "Running AI service tests..."
	@cd ai-service && pytest tests/ || python -m unittest discover tests

# Build all production artifacts
build:
	@echo "Building frontend..."
	@cd frontend && npm run build
	@echo "Building backend..."
	@cd backend && mvn clean package -DskipTests || cd backend && ./mvnw clean package -DskipTests

# Docker compose orchestration
docker-up:
	docker compose up --build -d

docker-down:
	docker compose down

clean:
	@echo "Cleaning build artifacts..."
	@cd frontend && rm -rf dist node_modules
	@cd backend && mvn clean || cd backend && ./mvnw clean
