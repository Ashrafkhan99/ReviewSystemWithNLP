# Review System with NLP Integration: Complete Project Documentation

## Table of Contents

- [Project Overview](#project-overview)
- [Architecture and Technology Stack](#architecture-and-technology-stack) 
- [System Components](#system-components)
- [Database Design](#database-design)
- [Caching Strategy](#caching-strategy)
- [NLP Integration](#nlp-integration)
- [API Documentation](#api-documentation)
- [Installation and Setup](#installation-and-setup)
- [Configuration](#configuration)
- [Usage Examples](#usage-examples)
- [Performance and Scalability](#performance-and-scalability)
- [Troubleshooting](#troubleshooting)

## Project Overview

This project implements a comprehensive review system integrated with Natural Language Processing capabilities for automated sentiment analysis. The platform allows users to submit reviews for various entities, automatically analyzes the emotional tone of the content, and maintains a real-time leaderboard ranking system based on sentiment scores.

### Key Features

- **Automated Sentiment Analysis**: Real-time NLP processing of review text using state-of-the-art machine learning models
- **Live Leaderboard System**: Redis-powered ranking system with sub-millisecond response times
- **RESTful API Design**: Comprehensive API endpoints following industry best practices
- **Microservices Architecture**: Distributed system design for scalability and maintainability
- **Real-time Updates**: Cache-first strategy for immediate leaderboard updates
- **Containerized Deployment**: Docker-based deployment for consistent environments

### Business Value

The system transforms unstructured text reviews into actionable business intelligence by automatically categorizing sentiment as positive, negative, or neutral. This enables businesses to understan, and make data-driven decisions without manual review analysis.

## Architecture and Technology Stack

### Microservices Architecture

The system employs a microservices architecture pattern with two primary services:

**Spring Boot API Service**
- Handles business logic and data persistence
- Manages entity information and review storage
- Coordinates leaderboard updates
- Provides RESTful endpoints for client interactions

**Python NLP Service**
- Specializes in sentiment analysis processing
- Loads and executes machine learning models
- Processes text data using Hugging Face Transformers
- Returns structured sentiment results

### Technology Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Primary API** | Spring Boot 3.2 | Enterprise-grade REST API framework |
| **NLP Processing** | Python Flask | Lightweight service for ML model execution |
| **Database** | PostgreSQL 15 | ACID-compliant relational data storage |
| **Caching** | Redis 7 | High-performance in-memory data structure store |
| **Containerization** | Docker & Docker Compose | Consistent deployment environments |
| **ML Framework** | Hugging Face Transformers | State-of-the-art NLP model processing |

### Design Patterns

**Cache-Aside Pattern**: Primary caching strategy where the application manages cache population and invalidation[1].

**Circuit Breaker Pattern**: Prevents cascading failures between services with fallback mechanisms[1].

**Repository Pattern**: Data access abstraction layer using Spring Data JPA[1].

## System Components

### Spring Boot API Service

The main API service implements a three-layer architecture:

**Controller Layer**: Handles HTTP requests and responses
- Entity management endpoints
- Review submission endpoints  
- Leaderboard query endpoints
- Health monitoring endpoints

**Service Layer**: Contains business logic
- Review processing and validation
- NLP service integration
- Leaderboard management
- Score calculation algorithms

**Repository Layer**: Data access management
- JPA entity operations
- Custom query implementations
- Transaction management

### Python NLP Service

The NLP service focuses on sentiment analysis:

**Model Management**: Background thread initialization to prevent cold start timeouts
**Request Processing**: Flask-based HTTP endpoint handling
**Sentiment Analysis**: Transformer model execution with confidence scoring
**Health Monitoring**: Service readiness and model status endpoints

### PostgreSQL Database

Relational database design with normalized structure:

**Entities Table**: Stores information about reviewed entities
**Reviews Table**: Contains review text and computed sentiment data
**Relationships**: Foreign key constraints maintain data integrity

### Redis Cache

High-performance caching layer:

**SortedSets**: Automatic ranking maintenance with O(log N) operations
**Real-time Updates**: Immediate leaderboard refreshes
**Persistence**: Data durability across service restarts

## Database Design

### Entity Relationship Design

The database follows third normal form principles with clear separation of concerns:

```sql
-- Primary entity storage
CREATE TABLE entities (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    address VARCHAR(255),
    category_type VARCHAR(100),
    total_score DOUBLE PRECISION DEFAULT 0.0,
    review_count INTEGER DEFAULT 0,
    average_score DOUBLE PRECISION DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Review content and sentiment data
CREATE TABLE reviews (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    entity_id BIGINT NOT NULL REFERENCES entities(id) ON DELETE CASCADE,
    review_text TEXT NOT NULL,
    reviewer_name VARCHAR(100),
    sentiment_label VARCHAR(50),
    sentiment_score DOUBLE PRECISION,
    sentiment_confidence DOUBLE PRECISION,
    is_positive BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Indexing Strategy

Performance-optimized indexes for common query patterns[2]:

```sql
-- Leaderboard queries
CREATE INDEX idx_entity_average_score ON entities(average_score DESC);

-- Review lookup patterns  
CREATE INDEX idx_review_entity_created ON reviews(entity_id, created_at DESC);
CREATE INDEX idx_review_sentiment ON reviews(entity_id, sentiment_label);
```

## Caching Strategy

### Redis SortedSet Implementation

The leaderboard uses Redis SortedSets for automatic ranking maintenance:

**Data Structure**: `ZADD restaurant:leaderboard {score} {entity_id}`
**Operations**: Add, update, and range queries in O(log N) time complexity
**Ranking**: `ZREVRANGE` operations for top-N queries
**Updates**: Cache-first strategy with immediate Redis updates

### Cache Invalidation Strategy

**Cache-Aside Pattern**: Application manages cache population and updates[1]
**Write-Behind Strategy**: Update cache first, then persist to database asynchronously
**Consistency**: Eventual consistency model acceptable for leaderboard use case

## NLP Integration

### Sentiment Analysis Pipeline

The NLP service implements a sophisticated sentiment analysis workflow:

**Model Loading**: Background thread initialization prevents service blocking
**Text Processing**: Input validation and preprocessing
**Model Execution**: Hugging Face Transformer model inference
**Result Processing**: Confidence scoring and label normalization

### Model Architecture

**Primary Model**: `cardiffnlp/twitter-roberta-base-sentiment-latest`
- Pre-trained on social media text for robust sentiment detection
- Multi-class output: POSITIVE, NEGATIVE, NEUTRAL
- Confidence scores for prediction reliability

**Fallback Model**: `distilbert-base-uncased-finetuned-sst-2-english`
- Lighter model for faster processing when primary model fails
- Binary classification with high accuracy

### Integration Pattern

**Asynchronous Processing**: NLP service loads models in background threads
**Health Monitoring**: Separate endpoints for service health and model readiness
**Error Handling**: Graceful degradation with fallback responses
**Timeout Management**: Configurable timeouts with retry logic

## API Documentation

### Entity Management Endpoints

#### Create Entity
```bash
curl -X POST http://localhost:8080/api/entities \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Downtown Cafe",
    "description": "Cozy coffee shop with excellent pastries",
    "address": "123 Main Street",
    "categoryType": "Food & Beverage"
  }'
```

#### Get All Entities
```bash
curl http://localhost:8080/api/entities
```

#### Get Entity by ID
```bash
curl http://localhost:8080/api/entities/1
```

#### Update Entity
```bash
curl -X PUT http://localhost:8080/api/entities/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Downtown Cafe & Bistro",
    "description": "Updated description",
    "address": "123 Main Street",
    "categoryType": "Food & Beverage"
  }'
```

#### Delete Entity
```bash
curl -X DELETE http://localhost:8080/api/entities/1
```

### Review Management Endpoints

#### Submit Review
```bash
curl -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -d '{
    "entityId": 1,
    "reviewText": "Excellent service and amazing coffee! Highly recommend.",
    "reviewerName": "John Smith"
  }'
```

#### Get Reviews for Entity
```bash
curl http://localhost:8080/api/reviews/entity/1
```

#### Get Review by ID
```bash
curl http://localhost:8080/api/reviews/5
```

#### Get Reviews by Sentiment
```bash
curl http://localhost:8080/api/reviews/entity/1/sentiment/POSITIVE
```

#### Get Recent Reviews
```bash
curl http://localhost:8080/api/reviews/entity/1/recent?hours=24
```

#### Get Review Statistics
```bash
curl http://localhost:8080/api/reviews/entity/1/statistics
```

#### Delete Review
```bash
curl -X DELETE http://localhost:8080/api/reviews/5
```

### Leaderboard Endpoints

#### Get Top Entities
```bash
curl http://localhost:8080/api/leaderboard?limit=10
```

#### Get Entity Rank
```bash
curl http://localhost:8080/api/leaderboard/entity/1/rank
```

### Search and Filter Endpoints

#### Search Entities
```bash
curl "http://localhost:8080/api/entities?search=coffee&page=0&size=10"
```

#### Filter by Category
```bash
curl http://localhost:8080/api/entities/category/Food%20%26%20Beverage
```

#### Paginated Reviews
```bash
curl "http://localhost:8080/api/reviews/entity/1?page=0&size=5&sortBy=createdAt&sortDir=desc"
```

### Health and Monitoring Endpoints

#### System Health Check
```bash
curl http://localhost:8080/api/health
```

#### NLP Service Health
```bash
curl http://localhost:5000/health
```

#### NLP Service Readiness
```bash
curl http://localhost:5000/ready
```

### Batch Operations

#### Batch Sentiment Analysis
```bash
curl -X POST http://localhost:5000/batch-analyze \
  -H "Content-Type: application/json" \
  -d '{
    "texts": [
      "Great experience overall!",
      "Terrible service, would not recommend.",
      "Average quality, nothing special."
    ]
  }'
```

## Installation and Setup

### Prerequisites

- Docker Desktop 4.0 or higher
- Docker Compose 2.0 or higher
- 8GB RAM minimum (for ML model loading)
- 10GB available disk space

### Quick Start

1. **Clone and Setup**
```bash
git clone 
cd review-system-nlp
```

2. **Environment Configuration**
```bash
# Copy environment template
cp .env.example .env

# Edit configuration as needed
nano .env
```

3. **Start Services**
```bash
# Start infrastructure services first
docker-compose up postgres redis -d

# Wait for services to initialize
sleep 30

# Start all services
docker-compose up --build
```

4. **Verify Installation**
```bash
# Check service health
curl http://localhost:8080/api/health
curl http://localhost:5000/health

# Check logs
docker-compose logs -f
```

### Manual Setup (Development)

#### Java Spring Boot Service

1. **Prerequisites**
```bash
# Install Java 17
sudo apt update
sudo apt install openjdk-17-jdk

# Verify installation
java --version
```

2. **Database Setup**
```bash
# Start PostgreSQL
docker run -d --name postgres \
  -e POSTGRES_DB=review_system \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin123 \
  -p 5432:5432 postgres:15-alpine

# Start Redis
docker run -d --name redis \
  -p 6379:6379 redis:7-alpine
```

3. **Application Startup**
```bash
cd spring-boot-api
./mvnw clean install
./mvnw spring-boot:run
```

#### Python NLP Service

1. **Environment Setup**
```bash
cd python-nlp-service
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

pip install -r requirements.txt
```

2. **Service Startup**
```bash
python app.py
```

## Configuration

### Environment Variables

**Spring Boot Service**
```yaml
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/review_system
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=admin123

# Redis Configuration
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# NLP Service Configuration  
NLP_SERVICE_URL=http://localhost:5000
```

**Python NLP Service**
```python
# Server Configuration
PORT=5000
FLASK_ENV=production

# Model Configuration
PRIMARY_MODEL=cardiffnlp/twitter-roberta-base-sentiment-latest
FALLBACK_MODEL=distilbert-base-uncased-finetuned-sst-2-english
```

### Application Properties

**Spring Boot Configuration** (`application.yml`)
```yaml
server:
  port: 8080

spring:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  data:
    redis:
      timeout: 2000ms
      
app:
  leaderboard:
    cache-key: "entity:leaderboard"
    top-limit: 50
```

## Usage Examples

### Complete Workflow Example

1. **Create Entity**
```bash
ENTITY_RESPONSE=$(curl -s -X POST http://localhost:8080/api/entities \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Tech Startup Inc",
    "description": "Innovative technology company",
    "address": "Silicon Valley",
    "categoryType": "Technology"
  }')

ENTITY_ID=$(echo $ENTITY_RESPONSE | jq -r '.id')
echo "Created entity with ID: $ENTITY_ID"
```

2. **Submit Multiple Reviews**
```bash
# Positive review
curl -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -d "{
    \"entityId\": $ENTITY_ID,
    \"reviewText\": \"Outstanding innovation and excellent team culture!\",
    \"reviewerName\": \"Alice Johnson\"
  }"

# Negative review  
curl -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -d "{
    \"entityId\": $ENTITY_ID,
    \"reviewText\": \"Poor management and unrealistic deadlines.\",
    \"reviewerName\": \"Bob Wilson\"
  }"
```

3. **Check Results**
```bash
# View entity ranking
curl http://localhost:8080/api/leaderboard/entity/$ENTITY_ID/rank

# Get review statistics
curl http://localhost:8080/api/reviews/entity/$ENTITY_ID/statistics

# View leaderboard
curl http://localhost:8080/api/leaderboard?limit=5
```

### Advanced Usage Patterns

**Batch Review Processing**
```bash
# Process multiple reviews simultaneously
for text in "Great service!" "Poor quality" "Average experience"; do
  curl -X POST http://localhost:8080/api/reviews \
    -H "Content-Type: application/json" \
    -d "{
      \"entityId\": $ENTITY_ID,
      \"reviewText\": \"$text\",
      \"reviewerName\": \"Batch User\"
    }" &
done
wait
```

**Real-time Monitoring**
```bash
# Monitor leaderboard changes
watch -n 2 'curl -s http://localhost:8080/api/leaderboard?limit=5 | jq'

# Track review statistics
watch -n 5 'curl -s http://localhost:8080/api/reviews/entity/1/statistics | jq'
```

## Performance and Scalability

### Performance Characteristics

**API Response Times**[3]:
- Entity queries:  getTopEntities(int limit) {
    return leaderboardService.getTopEntities(limit);
}
```

## Troubleshooting

### Common Issues and Solutions

**Issue**: NLP service timeout during startup
**Solution**: Increase Docker container memory allocation and health check timeout
```yaml
python-nlp:
  deploy:
    resources:
      limits:
        memory: 4G
  healthcheck:
    start_period: 180s
```

**Issue**: Database connection failures
**Solution**: Verify PostgreSQL service status and connection parameters
```bash
# Check database connectivity
docker exec -it postgres_db psql -U admin -d review_system -c "SELECT 1;"

# Review connection logs
docker-compose logs postgres
```

**Issue**: Redis cache misses
**Solution**: Monitor Redis memory usage and configure appropriate eviction policies
```bash
# Check Redis memory usage
docker exec -it redis_cache redis-cli INFO memory

# Monitor cache hit ratio
docker exec -it redis_cache redis-cli INFO stats | grep keyspace
```

**Issue**: Slow sentiment analysis processing
**Solution**: Implement model caching and batch processing
```python
# Cache frequently analyzed phrases
@lru_cache(maxsize=1000)
def get_cached_sentiment(text_hash):
    return redis_client.get(f"sentiment:{text_hash}")
```

### Monitoring and Debugging

**Application Logs**:
```bash
# View service logs
docker-compose logs -f spring-boot-api
docker-compose logs -f python-nlp

# Filter by log level
docker-compose logs spring-boot-api | grep ERROR
```

**Performance Monitoring**:
```bash
# Container resource usage
docker stats

# Database performance
docker exec -it postgres_db psql -U admin -d review_system \
  -c "SELECT * FROM pg_stat_activity WHERE state = 'active';"
```

**Health Monitoring**:
```bash
# Automated health checks
while true; do
  curl -f http://localhost:8080/api/health || echo "API unhealthy"
  curl -f http://localhost:5000/health || echo "NLP unhealthy"  
  sleep 30
done
```

### Performance Optimization

**Database Query Optimization**[2]:
```sql
-- Analyze query performance
EXPLAIN ANALYZE SELECT * FROM entities ORDER BY average_score DESC LIMIT 10;

-- Add missing indexes
CREATE INDEX CONCURRENTLY idx_entity_category ON entities(category_type);
```

**Redis Optimization**:
```bash
# Configure Redis for optimal performance
docker exec -it redis_cache redis-cli CONFIG SET maxmemory 1gb
docker exec -it redis_cache redis-cli CONFIG SET maxmemory-policy allkeys-lru
```
