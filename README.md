# Auth Service

A Spring Boot microservice for user authentication and authorization using Keycloak with distributed tracing capabilities.

## Features

- **User Registration**: Create new user accounts with Keycloak integration
- **User Authentication**: JWT-based authentication via Keycloak
- **Role Management**: Support for ROLE_USER and ROLE_ADMIN roles
- **Distributed Tracing**: Micrometer Tracing with Zipkin integration
- **Keycloak Integration**: Full Keycloak admin client integration
- **API Documentation**: OpenAPI/Swagger integration

## Tech Stack

- **Framework**: Spring Boot 3.3.0
- **Security**: Spring Security with OAuth2 Resource Server
- **Identity Provider**: Keycloak 26.0.0
- **Tracing**: Micrometer Tracing + Zipkin
- **Communication**: OpenFeign for microservice calls
- **Documentation**: SpringDoc OpenAPI
- **Build Tool**: Maven

## Prerequisites

- Java 17+
- Keycloak Server
- Zipkin (for distributed tracing)

## Quick Start

1. **Start Keycloak Server**:
   ```bash
   ./start-keycloak.sh
   ```

2. **Setup Keycloak Configuration**:
   ```bash
   ./setup-keycloak.sh
   ```

3. **Get Client Secret** (if needed):
   ```bash
   ./get-client-secret.sh
   ```

4. **Start Zipkin** (for tracing):
   ```bash
   # Navigate to post-service directory for Zipkin scripts
   cd ../post-service
   ./start-zipkin.sh
   ```

5. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

6. **Access the API**:
   - API Base URL: `http://localhost:8081`
   - Swagger UI: `http://localhost:8081/swagger-ui/index.html`
   - Zipkin UI: `http://localhost:9411`

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Login user and get JWT token

### Test Endpoints
- `GET /api/test/health` - Health check
- `GET /api/test/distributed-test` - Test microservice communication via FeignClient
- `POST /api/test/chain-test` - Test request chaining across services

## Configuration

The application can be configured via `application.yml`:

```yaml
server:
  port: 8081

spring:
  application:
    name: auth-service

keycloak:
  server-url: http://localhost:8080
  realm: myrealm
  client-id: auth-service
  client-secret: YOUR_CLIENT_SECRET
  admin-username: admin
  admin-password: admin
```

## Keycloak Setup

The service includes automated Keycloak setup scripts:

### Setup Script (`setup-keycloak.sh`)
- Creates realm 'myrealm'
- Creates client 'auth-service'
- Sets up roles (ROLE_USER, ROLE_ADMIN)
- Configures client settings

### Start Script (`start-keycloak.sh`)
- Starts Keycloak in development mode
- Accessible at `http://localhost:8080`
- Admin console: `http://localhost:8080/admin`

### Get Client Secret (`get-client-secret.sh`)
- Retrieves the client secret for auth-service client
- Updates application configuration

## User Registration Request

```json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123",
  "firstName": "Test",
  "lastName": "User",
  "dateOfBirth": "1990-01-01"
}
```

## User Login Request

```json
{
  "username": "testuser",
  "password": "password123"
}
```

## Authentication Response

```json
{
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "id": "user-id",
  "username": "testuser",
  "email": "test@example.com",
  "roles": ["ROLE_USER"]
}
```

## Distributed Tracing

The service automatically:
- Adds trace and span IDs to all log messages
- Sends trace data to Zipkin for visualization
- Propagates trace context across microservice calls
- Provides test endpoints for distributed tracing verification

## Microservice Communication

The auth-service includes FeignClient integration to communicate with other services:
- Post-service health checks
- Cross-service request tracing
- Service chain testing

## Security

- JWT tokens are issued by Keycloak
- Tokens include user roles and claims
- Support for both ROLE_USER and ROLE_ADMIN
- Automatic token validation for protected endpoints

## Development

### Running Tests
```bash
mvn test
```

### Building
```bash
mvn clean package
```

### Keycloak Development Mode
```bash
# Start Keycloak for development
./start-keycloak.sh

# Access admin console
# URL: http://localhost:8080/admin
# Username: admin
# Password: admin
```

## Monitoring

- **Health Checks**: Available at `/actuator/health`
- **Metrics**: Available at `/actuator/metrics`
- **Tracing**: View traces at Zipkin UI (http://localhost:9411)

## Troubleshooting

### Common Issues

1. **Keycloak Connection Failed**
   - Ensure Keycloak server is running on port 8080
   - Check if realm 'myrealm' exists
   - Verify client-secret configuration

2. **Authentication Failed**
   - Check if user exists in Keycloak
   - Verify username/password
   - Ensure Direct Access Grants is enabled for client

3. **Client Secret Issues**
   - Run `./get-client-secret.sh` to retrieve latest secret
   - Update `application.yml` with correct client-secret

## Scripts

- `start-keycloak.sh` - Start Keycloak server
- `stop-keycloak.sh` - Stop Keycloak server
- `setup-keycloak.sh` - Setup Keycloak realm and client
- `get-client-secret.sh` - Retrieve client secret

## Related Services

This service is part of a microservices architecture:
- **Auth Service**: This service - handles authentication
- **Post Service**: Manages posts and comments

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request
