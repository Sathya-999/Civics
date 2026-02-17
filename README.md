# Civic Connect

A Java web application for civic engagement — complaint filing, service discovery, departmental info, and an AI chatbot — built with Jakarta Servlets and deployed on Apache Tomcat.

## Features

- **User Authentication** – Register / Login (including Google OAuth)
- **Complaint Management** – Raise, track, and escalate civic complaints
- **Service Portal** – Browse government services by department
- **Dashboard** – Admin and user dashboards with analytics
- **AI Chatbot** – Civic-assistance chatbot
- **Email Notifications** – Automated email via `EmailUtil`

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17+, Jakarta Servlet API |
| Frontend | JSP, HTML, CSS, JavaScript |
| Database | MySQL (JDBC) |
| Server | Apache Tomcat 10 |
| Auth | Google OAuth 2.0 |

## Project Structure

```
src/              → Java source files (controllers, DAOs, models, filters, utilities)
WebContent/       → Web assets (JSP, HTML, CSS, JS, images)
  WEB-INF/        → Deployment descriptor (web.xml) & libraries
deploy.ps1        → PowerShell build & deploy script
reset_db.sql      → Database schema / reset script
```

## Getting Started

1. **Clone** the repo  
2. **Import** into your IDE as a Dynamic Web Project  
3. **Configure** database connection in `src/com/civics/util/DBUtil.java`  
4. **Run** `reset_db.sql` against your MySQL instance  
5. **Deploy** to Tomcat — or run `deploy.ps1` in PowerShell

## License

This project is for educational purposes.
