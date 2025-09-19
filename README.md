# Spring Boot + PostgreSQL 17.6 (Logical Replication)

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17.6-blue)
![Maven](https://img.shields.io/badge/Maven-Build-orange)
![Ubuntu](https://img.shields.io/badge/Ubuntu-25.04-E95420)

This project demonstrates how to set up a Spring Boot 3.2.0 application with **PostgreSQL 17.6** on **Ubuntu 25.04**, configured for **logical replication** using the `wal2json` plugin.

---

## 📌 Prerequisites

- **Ubuntu 25.04** (fully updated)
- **Java 17** (required for Spring Boot 3.2.0)
- **Maven** (for building the project)
- **PostgreSQL 17.6**
- **IDE**: IntelliJ IDEA, VS Code, or your preferred editor

---

## 🚀 Step 1: Install PostgreSQL 17.6

PostgreSQL 17.6 is not included in Ubuntu 25.04’s default repositories, so we’ll add the official PostgreSQL Apt repository.

### Update the system:
```bash
sudo apt update
sudo apt upgrade -y
```

### Install dependencies:
```bash
sudo apt install curl ca-certificates -y
```

### Add PostgreSQL repository:
```bash
sudo install -d /usr/share/postgresql-common/pgdg
sudo curl -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc --fail https://www.postgresql.org/media/keys/ACCC4CF8.asc
. /etc/os-release
sudo sh -c "echo 'deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt $VERSION_CODENAME-pgdg main' > /etc/apt/sources.list.d/pgdg.list"
```

### Install PostgreSQL 17.6:
```bash
sudo apt update
sudo apt install postgresql-17 postgresql-client-17 -y
```

### Verify installation:
```bash
psql --version
```

Expected output:
```
psql (PostgreSQL) 17.6 (Ubuntu 17.6-0ubuntu0.25.04.1)
```

### Start and enable PostgreSQL:
```bash
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### Set a password for the `postgres` user:
```bash
sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'your_password';"
```

### Create a database:
```bash
sudo -u postgres createdb orderdb
```

---

## 🔄 Step 2: Configure PostgreSQL for Logical Replication

We’ll configure PostgreSQL to use the **wal2json** plugin for logical replication.

### Edit PostgreSQL configuration:
```bash
sudo nano /etc/postgresql/17/main/postgresql.conf
```

Add or update:
```
wal_level = logical
max_wal_senders = 10
max_replication_slots = 10
shared_preload_libraries = 'wal2json'
```

### Restart PostgreSQL:
```bash
sudo systemctl restart postgresql
```

### Create tables and publication:
```bash
sudo -u postgres psql -d orderdb
```

Run:
```sql
CREATE TABLE events (
    id SERIAL PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    order_id UUID PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE PUBLICATION events_pub FOR TABLE events;
SELECT * FROM pg_create_logical_replication_slot('events_slot', 'wal2json');
```

---

## ⚙️ Step 3: Set Up the Spring Boot Project

### Install Java 17:
```bash
sudo apt install openjdk-17-jdk -y
java --version
```

### Install Maven:
```bash
sudo apt install maven -y
mvn --version
```

### Configure the application

Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/orderdb
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

---

## ▶️ Run and Test the Application

### Build the project:
```bash
mvn clean install
```

### Run the application:
```bash
mvn spring-boot:run
```

### Test the API (using curl or Postman):
```bash
curl -X POST http://localhost:8080/orders   -H "Content-Type: application/json"   -d '{"status":"CREATED","totalAmount":49.99}'
```

### Verify events:
```bash
sudo -u postgres psql -d orderdb -c "SELECT * FROM events;"
```

Check the Spring Boot console logs to confirm **events are streamed** from `EventConsumer`.

---

## ✅ Summary

You now have:
- **PostgreSQL 17.6** installed and configured for **logical replication**
- A **Spring Boot 3.2.0** project connected to `orderdb`
- An **event-driven setup** that streams changes from PostgreSQL

---
