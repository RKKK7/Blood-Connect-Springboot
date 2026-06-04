# 🩸 BloodConnect

> An AI-powered blood donation platform that connects blood donors with patients in need — in real time.

BloodConnect bridges the gap between people who urgently need blood and willing donors nearby. When a request is posted, the platform uses AI to assess its urgency, instantly notifies compatible donors, lets both parties coordinate through live chat, and tracks the donation from pledge to completion — rewarding donors with badges and certificates along the way.

This is a full **Java Spring Boot + PostgreSQL + React** application (originally a MERN-stack project, re-engineered onto a Spring Boot backend).

---

## 📖 Table of Contents

- [About the Project](#-about-the-project)
- [Features](#-features)
- [Tech Stack](#️-tech-stack)
- [Architecture & Ports](#️-architecture--ports)
- [Prerequisites](#-prerequisites)
- [Environment Variables](#️-environment-variables)
- [Getting Started](#-getting-started)
- [Creating Accounts & Sample Data](#-creating-accounts--sample-data)
- [API Overview](#-api-overview)
- [Project Structure](#-project-structure)
- [Troubleshooting](#-troubleshooting)
- [Security Notes](#-security-notes)
- [License](#-license)

---

## 🎯 About the Project

Blood shortages are a critical, recurring problem — patients in emergencies often need a specific blood type within hours, while willing donors nearby simply don't know a need exists. BloodConnect solves this by acting as a real-time matchmaking and coordination layer between **requesters** (patients/hospitals) and **donors**.

**How it works:**

1. A **requester** posts a blood request (patient, blood type, hospital, units needed).
2. An **AI model** classifies the request's urgency (critical / urgent / normal) and writes a short summary.
3. The platform instantly **notifies compatible donors** (matching blood-type compatibility rules) and shows the request on a live feed.
4. A **donor** pledges to donate; the two parties **chat in real time** to coordinate.
5. The requester **confirms** and later **marks the donation complete**.
6. The donor earns **badges**, a **printable certificate**, and becomes eligible to donate again after 56 days (the platform reminds them).

There are three roles: **donor**, **requester**, and **admin** (who gets a dashboard with analytics, bulk tools, and seeding).

---

## ✨ Features

### 🔐 Authentication & Accounts
- JWT-based register / login with BCrypt password hashing
- Role-based access: **donor**, **requester**, **admin**
- Email-based **password reset** (secure 30-minute token)
- Admin registration gated behind a secret code

### 🩸 Donor Management
- Donor profiles: blood type, location, city/state, availability toggle
- **Health score** and verification status
- **Badges** awarded by donation count — First Drop 🩸, Life Saver 💪, Blood Hero 🦸, Legend 🏆
- **Eligibility checks**: weight ≥ 50 kg, age 18–65, and the **56-day rule** between donations
- Availability scheduling (set days/hours you're available)
- Donor leaderboard

### 📋 Blood Requests
- Create requests with patient details, blood type, units, and hospital
- **AI urgency classification** (critical / urgent / normal) with a generated summary
- **Rate limiting**: max 3 open requests per user
- **Auto-expiry** of requests after 7 days
- Search & filter by status, blood type, urgency, and city

### 🚨 Emergency SOS
- One-tap SOS broadcast that alerts **all** compatible donors at once (in-app + email)
- Sent only once per request to prevent spam

### 🤝 Donation Lifecycle
- Pledge → Confirm → Complete workflow
- Post-donation **feedback** (1–5 rating) that adjusts the donor's health score
- Donation **history** for each donor
- Printable **HTML donation certificate**

### 💬 Real-Time Communication
- Live **chat** between donor and requester (Socket.IO)
- Live request feed updates
- In-app, email, and socket **notifications**

### 🧠 AI-Powered (Groq)
- Urgency classification of requests
- Donor–request **match scoring**
- Personalized post-donation **health tips**
- **Demand forecasting** for admins

### 📊 Dashboards & Analytics
- **Admin dashboard**: platform stats, blood-type breakdown, recent requests
- **Admin analytics**: daily request trends, fulfillment by blood type, response times, top cities
- **Blood shortage dashboard**: per-city, per-blood-type availability (🔴 red / 🟡 yellow / 🟢 green)
- **Public stats**: total donations, lives impacted, fulfillment rate
- Admin tools: bulk verify donors, bulk close requests, clean up stale requests, seed sample data

### ⏰ Automation
- Hourly cron jobs: auto-expire old requests, re-engage donors who became eligible again

---

## 🛠️ Tech Stack

| Layer        | Technology                                              |
|--------------|---------------------------------------------------------|
| Backend      | Java 17, Spring Boot 3.2, Spring Web, Spring Data JPA   |
| Security     | Spring Security, JWT (jjwt), BCrypt                      |
| Database     | PostgreSQL                                               |
| Real-time    | netty-socketio (Socket.IO protocol)                     |
| AI           | Groq API (`llama-3.1-8b-instant`)                       |
| Email        | Spring Mail (JavaMailSender / Gmail SMTP)               |
| Scheduling   | Spring `@Scheduled`                                      |
| Build        | Maven                                                    |
| Frontend     | React 18, Vite, React Router, Axios, Recharts, socket.io-client |

---

## 🏗️ Architecture & Ports

The backend runs **two servers**: the REST API and a separate Socket.IO server.

| Service                    | Port | Notes                                  |
|----------------------------|------|----------------------------------------|
| Spring Boot REST API       | 8080 | Vite proxies `/api` requests here      |
| Socket.IO (netty-socketio) | 5000 | real-time chat + live request feed     |
| React (Vite dev server)    | 5173 | the app you open in the browser        |


---

## 📋 Prerequisites

Make sure these are installed:

- **Java 17+** (JDK)
- **Maven 3.9+**
- **PostgreSQL 13+**
- **Node.js 18+** and npm
- *(optional)* a **Groq API key** — for AI features
- *(optional)* a **Gmail app password** — to send real emails

Verify with:

```bash
java -version
mvn -version
node -v
psql --version
```

---

## ⚙️ Environment Variables

The backend reads sensitive config from **environment variables**, with safe fallbacks in `application.properties`. **Never commit real credentials.**

### What each variable does

| Variable        | Required | Purpose                                                                       |
|-----------------|----------|-------------------------------------------------------------------------------|
| `DB_URL`        | ✅       | PostgreSQL JDBC URL                                                            |
| `DB_USER`       | ✅       | PostgreSQL username                                                           |
| `DB_PASSWORD`   | ✅       | PostgreSQL password                                                           |
| `JWT_SECRET`    | ✅       | Secret used to sign auth tokens — use **32+ random characters**               |
| `ADMIN_SECRET`  | ⬜       | Secret code required to register an admin (default `bloodconnect_admin_2024`) |
| `CLIENT_URL`    | ⬜       | Frontend origin, for CORS + email links (default `http://localhost:5173`)     |
| `PORT`          | ⬜       | REST API port (default `8080`)                                                |
| `SOCKET_PORT`   | ⬜       | Socket.IO port (default `5000`)                                               |
| `GROQ_API_KEY`  | ⬜       | Enables AI; if empty, the app uses safe fallback responses                    |
| `EMAIL_USER`    | ⬜       | Gmail address used to send mail; if empty, emails are **logged**, not sent    |
| `EMAIL_PASS`    | ⬜       | Gmail **app password** (not your normal Google password)                      |

> **About `EMAIL_USER` / `EMAIL_PASS`:** these are the login for the email account the app sends from (password-reset links and urgent donor alerts). For local development you can leave them blank — the app prints emails to the console instead of sending them, and everything still works.

### Option A — Set them in IntelliJ (recommended for local dev)

**Run → Edit Configurations… →** select `BloodConnectApplication` **→ Environment variables**, then paste (semicolon-separated):

| Environment Variable | Value |
|---------------------|-------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/bloodconnect` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | `your_password` |
| `JWT_SECRET` | `your_long_random_secret_at_least_32_chars` |
| `ADMIN_SECRET` | `bloodconnect_admin_2024` |
| `GROQ_API_KEY` | `<your_groq_api_key>` |
| `EMAIL_USER` | `<your_email>` |
| `EMAIL_PASS` | `<your_email_app_password>` |

### Option B — Set them in your shell

**Windows (PowerShell):**
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/bloodconnect"
$env:DB_USER="postgres"
$env:DB_PASSWORD="your_password"
$env:JWT_SECRET="your_long_random_secret_at_least_32_chars"
```

**macOS / Linux:**
```bash
export DB_URL=jdbc:postgresql://localhost:5432/bloodconnect
export DB_USER=postgres
export DB_PASSWORD=your_password
export JWT_SECRET=your_long_random_secret_at_least_32_chars
```

### Option C — Use a `.env` file

Copy the template and fill it in (the `.env` file is gitignored — never commit it):

```bash
cp backend/.env.example backend/.env
```

---

## Getting Started

Follow the steps below to run the BloodConnect application locally.

### 1. Open the Backend

Open the `backendSpring` project in **IntelliJ IDEA**.

### 2. Configure Environment Variables

Add the following environment variables to your Run Configuration:

| Environment Variable | Value |
|---------------------|-------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/bloodconnect` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | `your_password` |
| `JWT_SECRET` | `your_long_random_secret_at_least_32_chars` |
| `ADMIN_SECRET` | `bloodconnect_admin_2024` |
| `GROQ_API_KEY` | `<your_groq_api_key>` |
| `EMAIL_USER` | `<your_email>` |
| `EMAIL_PASS` | `<your_email_app_password>` |

### 3. Run the Backend

Run the Spring Boot application from IntelliJ IDEA.

The backend server will start on:

```text
http://localhost:8080
```

### 4. Open the Frontend

Open a new terminal and navigate to the frontend directory:

```bash
cd frontend
```

### 5. Install Dependencies

Install the required Node.js packages:

```bash
npm install
```

### 6. Start the Frontend

Run the development server:

```bash
npm run dev
```

The frontend application will be available at:

```text
http://localhost:5173
```

## Prerequisites

- Java 21+
- Maven
- PostgreSQL 18+
- Node.js 18+
- IntelliJ IDEA
- Git

## Database Setup

Create a PostgreSQL database named `bloodconnect`.

```sql
CREATE DATABASE bloodconnect;
```

Ensure PostgreSQL is running before starting the backend.



## ✨ Features

- **JWT authentication** — register / login, with email-based password reset
- **Donor profiles** — blood type, location, availability, badges, health score, 56-day eligibility rule
- **Blood requests** — AI urgency classification, max 3 open requests per user, 7-day auto-expiry
- **Emergency SOS** — one-time broadcast to all compatible donors
- **Donation lifecycle** — pledge → confirm → complete, with badge awards and post-donation feedback
- **AI features (Groq)** — donor-match scoring, health tips, demand forecasting
- **Real-time chat** — between donor and requester via Socket.IO
- **Notifications** — in-app, email, and live socket updates
- **Admin dashboard** — analytics, bulk operations, stale-request cleanup, data seeding
- **Blood shortage dashboard** — per-city, per-blood-type availability levels
- **Donation certificates** — printable HTML certificates

---

## 🛠️ Tech Stack

| Layer        | Technology                                      |
|--------------|-------------------------------------------------|
| Backend      | Java 17, Spring Boot 3.2, Spring Security, JPA  |
| Database     | PostgreSQL                                       |
| Real-time    | netty-socketio (Socket.IO protocol)             |
| Auth         | JWT (jjwt) + BCrypt                             |
| AI           | Groq API (llama-3.1-8b-instant)                 |
| Email        | Spring Mail (JavaMailSender)                     |
| Frontend     | React 18, Vite, React Router, Axios, Recharts   |

---

## 📋 Prerequisites

- **Java 17+** (JDK)
- **Maven 3.9+**
- **PostgreSQL 13+**
- **Node.js 18+** and npm
- *(optional)* a **Groq API key** for AI features
- *(optional)* a **Gmail app password** for sending real emails

---

## 🗄️ Architecture / Ports

| Service                    | Port | Notes                              |
|----------------------------|------|------------------------------------|
| Spring Boot REST API       | 8080 | Vite proxies `/api` here           |
| Socket.IO (netty-socketio) | 5000 | real-time chat + live feed         |
| React (Vite dev server)    | 5173 | the app you open in the browser    |

---

## ⚙️ Environment Variables Setup

The backend reads all sensitive values from **environment variables** (with safe
fallbacks in `application.properties`). **Never commit real credentials.**

### What each variable is for

| Variable        | Required? | Purpose                                                                 |
|-----------------|-----------|-------------------------------------------------------------------------|
| `DB_URL`        | Yes       | PostgreSQL JDBC URL                                                      |
| `DB_USER`       | Yes       | PostgreSQL username                                                      |
| `DB_PASSWORD`   | Yes       | PostgreSQL password                                                     |
| `JWT_SECRET`    | Yes       | Secret used to sign auth tokens (use 32+ random characters)             |
| `ADMIN_SECRET`  | No        | Secret code required to register an admin account                       |
| `CLIENT_URL`    | No        | Frontend origin (for CORS + email links)                                |
| `PORT`          | No        | REST API port (default 8080)                                            |
| `SOCKET_PORT`   | No        | Socket.IO port (default 5000)                                           |
| `GROQ_API_KEY`  | No        | Enables AI features; if empty, safe fallback responses are used         |
| `EMAIL_USER`    | No        | Gmail address used to send emails; if empty, emails are logged, not sent |
| `EMAIL_PASS`    | No        | Gmail **app password** (not your normal Google password)                |

> **Note:** `EMAIL_USER` / `EMAIL_PASS` let the app send password-reset links and
> urgent donor alerts. Leave them blank for local development — the app will print
> emails to the console instead of sending them, and everything still works.

### Option A — Set them in IntelliJ (recommended for local dev)

1. **Run → Edit Configurations…**
2. Select your **BloodConnectApplication** config
3. In the **Environment variables** field, add:
DB_URL=jdbc:postgresql://localhost:5432/bloodconnect;DB_USER=postgres;DB_PASSWORD=your_password;JWT_SECRET=your_long_random_secret_at_least_32_chars;ADMIN_SECRET=bloodconnect_admin_2024;GROQ_API_KEY=;EMAIL_USER=;EMAIL_PASS=

4. **Apply**, then run.

### Option B — Set them in your shell

**Windows (PowerShell):**
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/bloodconnect"
$env:DB_USER="postgres"
$env:DB_PASSWORD="your_password"
$env:JWT_SECRET="your_long_random_secret_at_least_32_chars"
```

**macOS / Linux:**
```bash
export DB_URL=jdbc:postgresql://localhost:5432/bloodconnect
export DB_USER=postgres
export DB_PASSWORD=your_password
export JWT_SECRET=your_long_random_secret_at_least_32_chars
```

### Option C — Use a `.env` file

A `.env.example` template is included. Copy it and fill in your values:

```bash
cp backend/.env.example backend/.env
```

> ⚠️ The `.env` file is gitignored and must **never** be committed.

---

## 🚀 Getting Started

### 1. Create the database

```bash
psql -U postgres -c "CREATE DATABASE bloodconnect;"
```

Tables are created automatically by Hibernate on first run.

### 2. Run the backend

```bash
cd backend
mvn spring-boot:run
```

On success you should see:
Tomcat started on port 8080 (http)
🔌 Socket.IO server started on port 5000
[CRON] Jobs started: request expiry + donor re-engagement

Verify: open `http://localhost:8080/api/health` → `{"status":"ok","version":"2.0"}`

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**

---

## 👤 Creating Accounts

1. Register an **admin** account — choose role `admin` and enter the admin secret (default `bloodconnect_admin_2024`).
2. Register a **requester** account (needed before seeding).
3. Log in as admin → use **Seed** to populate sample blood requests.
4. Optionally register **donor** accounts to explore matching, the leaderboard, and chat.

---



## 🔒 Security Notes

- Keep `JWT_SECRET` secret and stable — changing it logs out all existing users.
- Never commit `.env`, real passwords, or API keys.
- If a secret is ever exposed, **rotate it** (change the password / regenerate the key).





---

## 📖 Table of Contents

- [About the Project](#-about-the-project)
- [Features](#-features)
- [Tech Stack](#️-tech-stack)
- [Architecture & Ports](#️-architecture--ports)
- [Prerequisites](#-prerequisites)
- [Environment Variables](#️-environment-variables)
- [Getting Started](#-getting-started)
- [Creating Accounts & Sample Data](#-creating-accounts--sample-data)
- [API Overview](#-api-overview)
- [Project Structure](#-project-structure)
- [Troubleshooting](#-troubleshooting)
- [Security Notes](#-security-notes)
- [License](#-license)

---

## 🎯 About the Project

Blood shortages are a critical, recurring problem — patients in emergencies often need a specific blood type within hours, while willing donors nearby simply don't know a need exists. BloodConnect solves this by acting as a real-time matchmaking and coordination layer between **requesters** (patients/hospitals) and **donors**.

**How it works:**

1. A **requester** posts a blood request (patient, blood type, hospital, units needed).
2. An **AI model** classifies the request's urgency (critical / urgent / normal) and writes a short summary.
3. The platform instantly **notifies compatible donors** (matching blood-type compatibility rules) and shows the request on a live feed.
4. A **donor** pledges to donate; the two parties **chat in real time** to coordinate.
5. The requester **confirms** and later **marks the donation complete**.
6. The donor earns **badges**, a **printable certificate**, and becomes eligible to donate again after 56 days (the platform reminds them).

There are three roles: **donor**, **requester**, and **admin** (who gets a dashboard with analytics, bulk tools, and seeding).

---

## ✨ Features

### 🔐 Authentication & Accounts
- JWT-based register / login with BCrypt password hashing
- Role-based access: **donor**, **requester**, **admin**
- Email-based **password reset** (secure 30-minute token)
- Admin registration gated behind a secret code

### 🩸 Donor Management
- Donor profiles: blood type, location, city/state, availability toggle
- **Health score** and verification status
- **Badges** awarded by donation count — First Drop 🩸, Life Saver 💪, Blood Hero 🦸, Legend 🏆
- **Eligibility checks**: weight ≥ 50 kg, age 18–65, and the **56-day rule** between donations
- Availability scheduling (set days/hours you're available)
- Donor leaderboard

### 📋 Blood Requests
- Create requests with patient details, blood type, units, and hospital
- **AI urgency classification** (critical / urgent / normal) with a generated summary
- **Rate limiting**: max 3 open requests per user
- **Auto-expiry** of requests after 7 days
- Search & filter by status, blood type, urgency, and city

### 🚨 Emergency SOS
- One-tap SOS broadcast that alerts **all** compatible donors at once (in-app + email)
- Sent only once per request to prevent spam

### 🤝 Donation Lifecycle
- Pledge → Confirm → Complete workflow
- Post-donation **feedback** (1–5 rating) that adjusts the donor's health score
- Donation **history** for each donor
- Printable **HTML donation certificate**

### 💬 Real-Time Communication
- Live **chat** between donor and requester (Socket.IO)
- Live request feed updates
- In-app, email, and socket **notifications**

### 🧠 AI-Powered (Groq)
- Urgency classification of requests
- Donor–request **match scoring**
- Personalized post-donation **health tips**
- **Demand forecasting** for admins

### 📊 Dashboards & Analytics
- **Admin dashboard**: platform stats, blood-type breakdown, recent requests
- **Admin analytics**: daily request trends, fulfillment by blood type, response times, top cities
- **Blood shortage dashboard**: per-city, per-blood-type availability (🔴 red / 🟡 yellow / 🟢 green)
- **Public stats**: total donations, lives impacted, fulfillment rate
- Admin tools: bulk verify donors, bulk close requests, clean up stale requests, seed sample data

### ⏰ Automation
- Hourly cron jobs: auto-expire old requests, re-engage donors who became eligible again

---

## 🛠️ Tech Stack

| Layer        | Technology                                              |
|--------------|---------------------------------------------------------|
| Backend      | Java 17, Spring Boot 3.2, Spring Web, Spring Data JPA   |
| Security     | Spring Security, JWT (jjwt), BCrypt                      |
| Database     | PostgreSQL                                               |
| Real-time    | netty-socketio (Socket.IO protocol)                     |
| AI           | Groq API (`llama-3.1-8b-instant`)                       |
| Email        | Spring Mail (JavaMailSender / Gmail SMTP)               |
| Scheduling   | Spring `@Scheduled`                                      |
| Build        | Maven                                                    |
| Frontend     | React 18, Vite, React Router, Axios, Recharts, socket.io-client |

---

## 🏗️ Architecture & Ports

The backend runs **two servers**: the REST API and a separate Socket.IO server.

| Service                    | Port | Notes                                  |
|----------------------------|------|----------------------------------------|
| Spring Boot REST API       | 8080 | Vite proxies `/api` requests here      |
| Socket.IO (netty-socketio) | 5000 | real-time chat + live request feed     |
| React (Vite dev server)    | 5173 | the app you open in the browser        |


---

## 📋 Prerequisites

Make sure these are installed:

- **Java 17+** (JDK)
- **Maven 3.9+**
- **PostgreSQL 13+**
- **Node.js 18+** and npm
- *(optional)* a **Groq API key** — for AI features
- *(optional)* a **Gmail app password** — to send real emails

Verify with:

```bash
java -version
mvn -version
node -v
psql --version
```

---

## ⚙️ Environment Variables

The backend reads sensitive config from **environment variables**, with safe fallbacks in `application.properties`. **Never commit real credentials.**

### What each variable does

| Variable        | Required | Purpose                                                                       |
|-----------------|----------|-------------------------------------------------------------------------------|
| `DB_URL`        | ✅       | PostgreSQL JDBC URL                                                            |
| `DB_USER`       | ✅       | PostgreSQL username                                                           |
| `DB_PASSWORD`   | ✅       | PostgreSQL password                                                           |
| `JWT_SECRET`    | ✅       | Secret used to sign auth tokens — use **32+ random characters**               |
| `ADMIN_SECRET`  | ⬜       | Secret code required to register an admin (default `bloodconnect_admin_2024`) |
| `CLIENT_URL`    | ⬜       | Frontend origin, for CORS + email links (default `http://localhost:5173`)     |
| `PORT`          | ⬜       | REST API port (default `8080`)                                                |
| `SOCKET_PORT`   | ⬜       | Socket.IO port (default `5000`)                                               |
| `GROQ_API_KEY`  | ⬜       | Enables AI; if empty, the app uses safe fallback responses                    |
| `EMAIL_USER`    | ⬜       | Gmail address used to send mail; if empty, emails are **logged**, not sent    |
| `EMAIL_PASS`    | ⬜       | Gmail **app password** (not your normal Google password)                      |

> **About `EMAIL_USER` / `EMAIL_PASS`:** these are the login for the email account the app sends from (password-reset links and urgent donor alerts). For local development you can leave them blank — the app prints emails to the console instead of sending them, and everything still works.

### Option A — Set them in IntelliJ (recommended for local dev)

**Run → Edit Configurations… →** select `BloodConnectApplication` **→ Environment variables**, then paste (semicolon-separated):
| Environment Variable | Value |
|---------------------|-------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/bloodconnect` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | `your_password` |
| `JWT_SECRET` | `your_long_random_secret_at_least_32_chars` |
| `ADMIN_SECRET` | `bloodconnect_admin_2024` |
| `GROQ_API_KEY` | `<your_groq_api_key>` |
| `EMAIL_USER` | `<your_email>` |
| `EMAIL_PASS` | `<your_email_app_password>` |
### Option B — Set them in your shell

**Windows (PowerShell):**
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/bloodconnect"
$env:DB_USER="postgres"
$env:DB_PASSWORD="your_password"
$env:JWT_SECRET="your_long_random_secret_at_least_32_chars"
```

**macOS / Linux:**
```bash
export DB_URL=jdbc:postgresql://localhost:5432/bloodconnect
export DB_USER=postgres
export DB_PASSWORD=your_password
export JWT_SECRET=your_long_random_secret_at_least_32_chars
```

### Option C — Use a `.env` file

Copy the template and fill it in (the `.env` file is gitignored — never commit it):

```bash
cp backend/.env.example backend/.env
```

---

## 🚀 Getting Started

### Step 1 — Create the database

Tables are created automatically by Hibernate on first run; you only create the empty database:

```bash
psql -U postgres -c "CREATE DATABASE bloodconnect;"
```

### Step 2 — Run the backend

```bash
cd backend
mvn spring-boot:run
```

The first run downloads dependencies (needs internet). When it's up you'll see:
Tomcat started on port 8080 (http)
🔌 Socket.IO server started on port 5000
[CRON] Jobs started: request expiry + donor re-engagement

**Verify:** open `http://localhost:8080/api/health` → should return `{"status":"ok","version":"2.0"}`

To build a runnable jar instead:
```bash
mvn clean package
java -jar target/*.jar
```

### Step 3 — Run the frontend

In a **second terminal**:

```bash
cd frontend
npm install        # first time only
npm run dev
```

Open the URL it prints — usually **http://localhost:5173**

---

## 👤 Creating Accounts & Sample Data

1. **Register an admin** — on the Register page, choose role **admin** and enter the admin secret (default `bloodconnect_admin_2024`).
2. **Register a requester** — a normal account with role **requester** (needed before seeding).
3. **Seed sample data** — log in as admin, open the Admin dashboard, and run **Seed** to load sample blood requests.
4. *(optional)* **Register donor accounts** to explore matching, the leaderboard, and chat.

> With email disabled, the password-reset link is printed in the backend console instead of being emailed.

---

## 🔌 API Overview

Base URL: `http://localhost:8080/api`

| Method | Endpoint                          | Description                          | Auth      |
|--------|-----------------------------------|--------------------------------------|-----------|
| POST   | `/auth/register`                  | Register a new user                  | Public    |
| POST   | `/auth/login`                     | Log in                               | Public    |
| GET    | `/auth/me`                        | Current user + donor profile         | User      |
| POST   | `/auth/forgot-password`           | Request password reset               | Public    |
| POST   | `/auth/reset-password/{token}`    | Reset password                       | Public    |
| GET    | `/requests`                       | List blood requests (filters)        | Public    |
| POST   | `/requests`                       | Create a request (AI urgency)        | User      |
| GET    | `/requests/{id}`                  | Request details + donations          | Public    |
| POST   | `/requests/{id}/respond`          | Pledge to donate                     | User      |
| POST   | `/requests/{id}/sos`              | Emergency SOS broadcast              | User      |
| PUT    | `/requests/donations/{id}/confirm`| Confirm a pledge                     | User      |
| PUT    | `/requests/donations/{id}/complete`| Mark donation complete + badges     | User      |
| POST   | `/requests/donations/{id}/feedback`| Submit donor feedback               | User      |
| GET    | `/donors/nearby`                  | Nearby available donors              | Public    |
| GET    | `/donors/leaderboard`             | Top donors                           | Public    |
| GET/PUT| `/donors/profile`                 | Get / update donor profile           | User      |
| GET    | `/match/{requestId}`              | AI-scored donor matches              | Public    |
| GET    | `/chat/{donationId}`              | Chat messages                        | User      |
| POST   | `/chat/{donationId}`              | Send a chat message                  | User      |
| GET    | `/notifications`                  | User notifications                   | User      |
| GET    | `/shortage`                       | Blood shortage dashboard             | Public    |
| GET    | `/analytics/public`               | Public platform stats                | Public    |
| GET    | `/analytics/admin`                | Admin analytics                      | Admin     |
| GET    | `/analytics/forecast`             | AI demand forecast                   | Admin     |
| GET    | `/certificate/{donationId}`       | HTML donation certificate            | Token     |
| POST   | `/admin/seed`                     | Seed sample requests                 | Admin     |

---

---

## 🧯 Troubleshooting

| Problem                                              | Fix                                                                                   |
|------------------------------------------------------|---------------------------------------------------------------------------------------|
| `Could not resolve placeholder 'app.jwt.secret'`     | Ensure `application.properties` is in `src/main/resources/`; rebuild the project       |
| `Port 8080 was already in use`                       | Stop the old run, or kill the process: `netstat -ano \| findstr :8080` then `taskkill /PID <pid> /F` (run terminal as Administrator) |
| `Port 5000 in use` (common on macOS — AirPlay)       | Set `SOCKET_PORT` to e.g. `5050` and update `/socket.io` proxy + socket URL accordingly |
| `password authentication failed for user "postgres"` | `DB_PASSWORD` doesn't match your Postgres password                                     |
| `Connection refused` on port 5432                    | PostgreSQL service isn't running                                                       |
| Maven can't resolve `netty-socketio:2.0.9`           | Bump the version in `pom.xml` (e.g. `2.0.12`) and reload Maven                          |
| CORS / socket errors in browser                      | Open the app via `http://localhost:5173` (not `127.0.0.1`) and keep both servers running |

---

## 🔒 Security Notes

- Keep `JWT_SECRET` secret and **stable** — changing it logs out all existing users.
- Never commit `.env`, real passwords, or API keys. Confirm `.gitignore` covers `target/`, `node_modules/`, `.env`, and `.idea/`.
- If a secret is ever exposed, **rotate it** (change the DB password, regenerate the Groq/Gmail key) — removing it from a file doesn't remove it from git history.

---

## 📝 License

Built for academic / portfolio purposes. Feel free to use and adapt.

---

<p align="center">Made with ❤️ to help save lives — one donation at a time. 🩸</p>
