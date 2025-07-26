# Crawler Manager

A web-based management interface for monitoring and controlling web crawler instances. Built with Quarkus backend and Vue.js 3 + PrimeVue frontend.

## Architecture

- **Backend**: Quarkus 3.19.4 with WebSocket and REST support
- **Frontend**: Vue.js 3 with TypeScript, PrimeVue UI components
- **Build System**: Maven with integrated frontend build via Vite
- **Real-time Updates**: WebSocket connections for live status updates

## Development Setup

### Prerequisites

- Java 21+
- Maven 3.8+
- Node.js 18+ and npm (for frontend development)

### Quick Start

Use the development script to start both backend and frontend:

```bash
./scripts/dev-start.sh
```

This will start:
- Quarkus backend at http://localhost:8082
- Vue.js frontend at http://localhost:5173
- Quarkus Dev UI at http://localhost:8082/q/dev/

### Manual Development Mode

**Backend only:**
```bash
mvn quarkus:dev
```

**Frontend only:**
```bash
cd frontend
npm install
npm run dev
```

## Production Build

Use the production build script:

```bash
./scripts/build-prod.sh
```

Or manually:

```bash
mvn clean package
```

This will:
1. Install Node.js and npm dependencies
2. Build the Vue.js frontend
3. Copy frontend assets to Quarkus resources
4. Package the complete application

Run the production build:
```bash
java -jar target/quarkus-app/quarkus-run.jar
```

## Testing

Run all tests:
```bash
./scripts/test.sh
```

Or run tests separately:
```bash
# Backend tests
mvn test

# Frontend tests
cd frontend && npm run test
```

## Project Structure

```
crawler-manager/
├── src/main/java/          # Quarkus backend code
├── src/main/resources/     # Backend resources & built frontend assets
├── frontend/               # Vue.js frontend source
│   ├── src/               # Vue components and TypeScript code
│   ├── package.json       # Frontend dependencies
│   └── vite.config.ts     # Frontend build configuration
├── scripts/               # Build and development scripts
├── pom.xml               # Maven configuration with frontend integration
└── README.md
```

## Configuration

The application is configured via `src/main/resources/application.properties`:

- **Server**: Runs on port 8082
- **CORS**: Configured for frontend development
- **Database**: PostgreSQL connection
- **Redis**: For caching and real-time updates
- **Crawler Instances**: Configured crawler endpoints

## Features (Planned)

- Real-time crawler health monitoring
- Metrics visualization with charts
- Crawl job triggering and monitoring
- Job history and status tracking
- Notification system for events
- Responsive design for mobile devices

## Related Guides

- [Quarkus REST](https://quarkus.io/guides/rest)
- [Quarkus WebSockets](https://quarkus.io/guides/websockets)
- [Vue.js 3](https://vuejs.org/guide/)
- [PrimeVue](https://primevue.org/)