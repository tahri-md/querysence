# QuerySence

A full-stack SQL query analysis platform with team collaboration features. QuerySence helps developers and DBAs analyze, optimize, and secure SQL queries with AI-powered insights and comprehensive performance metrics.

## Features

### Core Analysis
- **SQL Query Analysis** - Detailed complexity scoring and performance assessment
- **Security Scanning** - Identify SQL injection vulnerabilities and security risks
- **Query Optimization** - AI-powered suggestions for query optimization
- **Index Recommendations** - Smart index suggestions based on query patterns
- **Query Parsing** - Support for multiple SQL dialects (PostgreSQL, MySQL, SQLServer, etc.)

### Database Management
- **Schema Management** - Create and manage database schemas with DDL support
- **Table Definitions** - Full table structure editing with column and index management
- **Multi-Dialect Support** - Work with different SQL databases seamlessly
- **Query History** - Complete history of analyzed queries with timestamps and metrics

### Team Collaboration
- **Project Sharing** - Share projects with team members
- **Role-Based Access** - Owner, Editor, and Viewer roles for fine-grained permissions
- **Invitations** - Send and accept project invitations with expiring invite links
- **Team Project Access** - Manage who can view, edit, or own your projects

### Analytics & Insights
- **Dashboard Overview** - Real-time analytics on query trends and complexity
- **Performance Metrics** - Execution time analysis and trend visualization
- **Top Issues Tracking** - Identify the most common query problems
- **Query Trend Analysis** - Monitor query patterns over time

### User Experience
- **PWA Support** - Progressive Web App for offline usage
- **Responsive Design** - Works seamlessly on desktop and mobile
- **Dark Mode** - Comfortable theme for extended usage
- **Real-Time Notifications** - Toast notifications for all operations

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.4.1
- **Language**: Java 21
- **Database**: PostgreSQL with Flyway migrations
- **ORM**: Hibernate/JPA
- **Security**: Spring Security with JWT authentication
- **API**: REST with Swagger/OpenAPI documentation
- **Build**: Maven

### Frontend
- **Framework**: Next.js 14 with App Router
- **Language**: TypeScript
- **Styling**: TailwindCSS with custom components
- **UI Components**: Shadcn/ui
- **State Management**: React Hooks & Context API
- **Charts**: Recharts for analytics visualization
- **Authentication**: JWT with local storage

## Prerequisites

- **Java 21** or higher
- **Node.js 18** or higher
- **PostgreSQL 12** or higher
- **npm** or **yarn**
- **Maven 3.8+** (included via mvnw)

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/tahri-md/querysence.git
cd querysence
```

### 2. Backend Setup

```bash
cd querysence-back

# Build the backend
./mvnw clean package -DskipTests

# Or compile only
./mvnw clean compile
```

**Database Configuration** - Update `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/querysence
spring.datasource.username=postgres
spring.datasource.password=your_password
```

### 3. Frontend Setup

```bash
cd querysence-front

# Install dependencies
npm install

# Or with yarn
yarn install
```

## Running the Application

### Backend

```bash
cd querysence-back

# Run the Spring Boot application
./mvnw spring-boot:run
```

The backend will start on `http://localhost:8081`

### Frontend

```bash
cd querysence-front

# Development mode
npm run dev

# Or with yarn
yarn dev
```

The frontend will be available at `http://localhost:3000`

### Using Docker Compose (Optional)

```bash
# Start all services (PostgreSQL, Backend, Frontend)
docker-compose up

# Or in detached mode
docker-compose up -d
```

## API Documentation

### Authentication Endpoints
- `POST /auth/register` - Register a new user
- `POST /auth/login` - Login and get JWT tokens
- `POST /auth/refresh` - Refresh access token
- `POST /auth/logout` - Logout and blacklist token
- `GET /auth/me` - Get current user info
- `GET /auth/me/invites` - Get pending project invitations

### Project Endpoints
- `GET /projects` - List all user projects (owned and shared)
- `POST /projects` - Create a new project
- `GET /projects/{id}` - Get project details
- `DELETE /projects/{id}` - Delete a project
- `GET /projects/{id}/members` - List project members
- `POST /projects/{id}/members/invite` - Send project invitation
- `POST /projects/{id}/members/accept-invite` - Accept an invitation
- `DELETE /projects/{id}/members/{memberId}` - Remove a member

### Schema & Query Endpoints
- `POST /projects/{projectId}/schemas` - Create schema
- `POST /schemas/{id}/tables` - Add table to schema
- `POST /queries/analyze` - Analyze SQL query
- `POST /queries/parse` - Parse SQL query
- `GET /queries/{id}` - Get query analysis results
- `GET /history` - Get query history with pagination
- `GET /analytics/overview` - Get dashboard analytics

## Project Structure

```
querysence/
├── querysence-back/              # Spring Boot Backend
│   ├── src/main/java/
│   │   └── com/example/querysence/
│   │       ├── controller/       # REST endpoints
│   │       ├── service/          # Business logic
│   │       ├── repository/       # Data access
│   │       ├── model/            # Entities & DTOs
│   │       └── parser/           # SQL parsing logic
│   ├── src/main/resources/
│   │   ├── db/migration/         # Flyway migrations
│   │   ├── application.properties
│   │   └── application.yml
│   └── pom.xml
│
├── querysence-front/             # Next.js Frontend
│   ├── app/
│   │   ├── (auth)/              # Authentication routes
│   │   ├── (dashboard)/         # Dashboard routes
│   │   └── page.tsx             # Home page
│   ├── components/
│   │   ├── ui/                  # Reusable UI components
│   │   └── *.tsx                # Feature components
│   ├── lib/
│   │   ├── api.ts               # API client
│   │   └── auth-context.tsx     # Auth provider
│   ├── public/                  # Static assets
│   └── package.json
│
└── README.md                     # This file
```

## Key Features Explained

### Query Analysis Workflow
1. User enters a SQL query
2. System parses and analyzes the query
3. Generate complexity score, security findings, and optimization suggestions
4. Store analysis in history for future reference
5. Display results with actionable insights

### Project Collaboration
1. Create a project and become the owner
2. Share project with others via invitation
3. Team members accept invitation to gain access
4. Control member permissions (Owner, Editor, Viewer)
5. Manage schemas and queries collaboratively

### Team Roles
- **Owner**: Full control, can manage members and delete project
- **Editor**: Can create/edit schemas and queries, view analytics
- **Viewer**: Read-only access to project data and analytics

## Security Features

- **JWT Authentication** - Secure token-based authentication
- **Role-Based Access Control** - Fine-grained permission management
- **SQL Injection Detection** - Identify potential security vulnerabilities
- **Secure Password Storage** - Bcrypt hashing for user passwords
- **Token Blacklisting** - Invalidate tokens on logout
- **CORS Protection** - Configured cross-origin resource sharing

## Database Migrations

Migrations are automatically applied on startup using Flyway. Existing migrations:
- V1__initial_schema.sql - Core tables for users, projects, schemas
- V2__add_collaboration.sql - Team collaboration tables (members, invites)

## Development

### Building the Backend
```bash
cd querysence-back
./mvnw clean package
```

### Running Tests
```bash
cd querysence-back
./mvnw test
```

### Frontend Development
```bash
cd querysence-front
npm run dev      # Start development server
npm run build    # Production build
npm start        # Start production server
```

## Troubleshooting

### Backend won't start
- Ensure PostgreSQL is running
- Check database credentials in `application.properties`
- Check port 8081 is not in use

### Frontend won't compile
- Delete `node_modules` and `.next` directories
- Run `npm install` again
- Check Node.js version with `node --version`

### Can't connect to backend
- Ensure backend is running on port 8081
- Check API URL in `lib/api.ts`
- Verify CORS configuration in Spring Boot

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues, questions, or suggestions, please open an issue on GitHub.

---

**Happy analyzing!** 🚀
