# AI Crop Advisor - Render Platform Deployment Guide

## Overview
This guide covers deploying your Spring Boot application to Render.com with PostgreSQL database.

---

## Prerequisites

✅ **Before starting, you need:**
1. Render.com account (free: https://render.com)
2. Git repository (GitHub/GitLab) with your code pushed
3. All environment variables documented
4. Docker knowledge (basic)

---

## Step 1: Prepare Your Application

### 1.1 Create `Dockerfile`

Create a file named `Dockerfile` in your project root:

```dockerfile
# Build stage
FROM maven:3.9.0-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/AI_Crop_Analysis/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Location:** `C:\Procure\git\AI_CROP_New\AI_Crop\Dockerfile`

### 1.2 Create `.dockerignore`

```
target/
.git/
.gitignore
.env
.env.*
README.md
node_modules/
.classpath
.project
.settings/
*.log
uploads/
logs/
```

**Location:** `C:\Procure\git\AI_CROP_New\AI_Crop\.dockerignore`

### 1.3 Create `render.yaml` (Infrastructure as Code)

```yaml
services:
  - type: web
    name: ai-crop-advisor
    env: docker
    dockerfilePath: ./Dockerfile
    plan: free
    region: oregon
    repo: https://github.com/YOUR_USERNAME/AI_Crop.git
    branch: main
    healthCheckPath: /api/health
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: production
      - key: PORT
        value: 8080

databases:
  - name: ai-crop-db
    databaseName: aicrop_db
    user: aicrop_user
    plan: free
    region: oregon
```

**Location:** `C:\Procure\git\AI_CROP_New\AI_Crop\render.yaml`

---

## Step 2: Set Up PostgreSQL Database on Render

### 2.1 Create PostgreSQL Database

1. Go to **Render Dashboard** → **New +** → **PostgreSQL**
2. Configure:
   ```
   Name: ai-crop-db
   Database: ai_crop_db
   User: aicrop_user
   Region: Oregon (same as app)
   Plan: Free tier
   ```
3. Click **Create Database**
4. **Copy the connection string** (you'll need this)

### 2.2 Database Connection String Format
```
postgresql://username:password@hostname:5432/database_name
```

Example:
```
postgresql://aicrop_user:your_password@dpg-xyz123.render.com:5432/ai_crop_db
```

---

## Step 3: Configure Environment Variables

### 3.1 Create `.env.production` file

```properties
# ============================================
# DATABASE CONFIGURATION
# ============================================
DATABASE_URL=postgresql://aicrop_user:YOUR_PASSWORD@dpg-xyz123.render.com:5432/ai_crop_db

# ============================================
# SPRING CONFIGURATION
# ============================================
SPRING_DATASOURCE_URL=jdbc:postgresql://dpg-xyz123.render.com:5432/ai_crop_db?sslmode=require
SPRING_DATASOURCE_USERNAME=aicrop_user
SPRING_DATASOURCE_PASSWORD=YOUR_PASSWORD
SPRING_JPA_HIBERNATE_DDL_AUTO=validate

# ============================================
# JWT CONFIGURATION
# ============================================
JWT_SECRET=5vOdspNJ4JUU+kvw3wwHltrScFcTQTtyTbUIrGFsiBM+0vBu/C+2xNwS3K7d/22OsV0WxwwWSU2gC+pMzcaAQA==

# ============================================
# CLOUDINARY CONFIGURATION
# ============================================
CLOUDINARY_CLOUD_NAME=zl36rfxr
CLOUDINARY_API_KEY=583992561841781
CLOUDINARY_API_SECRET=1QzrNkCkZBjn0DPtdc1bkxPVYFc

# ============================================
# GROQ AI CONFIGURATION
# ============================================
GROQ_API_KEY=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

# ============================================
# WEATHER API CONFIGURATION
# ============================================
WEATHER_API_KEY=d8186ed62280132f79314304f33e8227

# ============================================
# APP CONFIGURATION
# ============================================
PORT=8080
JAVA_OPTS=-Xmx512m -Xms256m
```

### 3.2 DO NOT commit `.env.production`

Add to `.gitignore`:
```
.env.production
.env.local
.env
```

---

## Step 4: Push Code to GitHub

### 4.1 Push your repository

```bash
cd C:\Procure\git\AI_CROP_New\AI_Crop

# Stage all changes (except .env files)
git add .
git commit -m "Prepare for Render deployment - add Dockerfile and configuration"

# Push to GitHub
git push origin main
```

---

## Step 5: Deploy on Render Dashboard

### 5.1 Create Web Service

1. Go to **Render Dashboard** → **New +** → **Web Service**
2. Select your GitHub repository
3. Configure:
   ```
   Name: ai-crop-advisor
   Runtime: Docker
   Build Command: (leave empty - uses Dockerfile)
   Start Command: (leave empty - uses Dockerfile)
   Plan: Free (or Starter for production)
   Region: Oregon
   ```

### 5.2 Add Environment Variables

Click **Environment** and add all variables from `.env.production`:

```
DATABASE_URL = postgresql://...
SPRING_DATASOURCE_URL = jdbc:postgresql://...
SPRING_DATASOURCE_USERNAME = aicrop_user
SPRING_DATASOURCE_PASSWORD = [hidden]
JWT_SECRET = [hidden]
CLOUDINARY_CLOUD_NAME = zl36rfxr
CLOUDINARY_API_KEY = [hidden]
CLOUDINARY_API_SECRET = [hidden]
GROQ_API_KEY = [hidden]
WEATHER_API_KEY = [hidden]
PORT = 8080
```

### 5.3 Deploy

Click **Create Web Service** and wait for deployment to complete (~5-10 minutes)

---

## Step 6: Configure application.properties for Production

Update `src/main/resources/application.properties`:

```properties
# ============================================
# PRODUCTION DATABASE CONFIGURATION
# ============================================
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# SSL Mode for Production
spring.datasource.hikari.connection-init-sql=SET ssl=true

# ============================================
# JPA/HIBERNATE CONFIGURATION
# ============================================
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

# ============================================
# SERVER CONFIGURATION
# ============================================
server.port=${PORT:8080}
server.servlet.context-path=/api
server.error.include-message=always

# ============================================
# JWT CONFIGURATION
# ============================================
jwt.secret=${JWT_SECRET}
jwt.expiration=604800000

# ============================================
# CLOUDINARY CONFIGURATION
# ============================================
cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME}
cloudinary.api-key=${CLOUDINARY_API_KEY}
cloudinary.api-secret=${CLOUDINARY_API_SECRET}
cloudinary.folder.soil-analysis=Soil_Analysis

# ============================================
# GROQ AI CONFIGURATION
# ============================================
ai.groq.enabled=true
ai.groq.api-key=${GROQ_API_KEY}

# ============================================
# WEATHER API CONFIGURATION
# ============================================
weather.api.enabled=true
weather.api.key=${WEATHER_API_KEY}

# ============================================
# LOGGING
# ============================================
logging.level.root=WARN
logging.level.com.procure.aicrop=INFO
```

---

## Step 7: Verify Deployment

### 7.1 Check Application Status

1. Go to **Render Dashboard** → Your service
2. Check:
   - ✅ Status: "Live"
   - ✅ Build logs show no errors
   - ✅ Logs show app started on port 8080

### 7.2 Test API Endpoints

```bash
# Get your Render URL (e.g., https://ai-crop-advisor.onrender.com)

# Health check
curl https://ai-crop-advisor.onrender.com/api/health

# Test registration
curl -X POST https://ai-crop-advisor.onrender.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test@123456",
    "fullName": "Test User",
    "phoneNumber": "9999999999"
  }'

# Test login
curl -X POST https://ai-crop-advisor.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test@123456"
  }'
```

### 7.3 Test in Browser

Open your browser:
```
https://ai-crop-advisor.onrender.com/api/login.html
```

---

## Environment Variables Summary

| Variable | Value | Where to Get |
|----------|-------|--------------|
| `DATABASE_URL` | PostgreSQL connection string | Render DB page |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | Render DB page |
| `JWT_SECRET` | 512-bit base64 string | Keep as is |
| `CLOUDINARY_CLOUD_NAME` | zl36rfxr | Your Cloudinary |
| `CLOUDINARY_API_KEY` | 583992561841781 | Your Cloudinary |
| `CLOUDINARY_API_SECRET` | 1QzrNkCkZBjn0DPtdc1bkxPVYFc | Your Cloudinary |
| `GROQ_API_KEY` | gsk_... | Your Groq account |
| `WEATHER_API_KEY` | d8186ed62... | OpenWeatherMap |
| `PORT` | 8080 | Keep as is |

---

## Troubleshooting

### Issue: "Build failed"
```
Solution: Check Dockerfile path, ensure Java 17 is used
Run locally: docker build -t ai-crop .
```

### Issue: "Database connection error"
```
Solution: Verify DATABASE_URL format
Check PostgreSQL is running on Render
Add ?sslmode=require to JDBC URL
```

### Issue: "Application crashes after deploy"
```
Solution: Check logs in Render dashboard
Verify all environment variables are set
Check JAR file size isn't too large for free tier
```

### Issue: "401 Unauthorized on login"
```
Solution: Verify JWT_SECRET is correct
Check database migration ran (ddl-auto=validate)
```

---

## Production Checklist

- [ ] Dockerfile created and tested locally
- [ ] `.env.production` configured with all secrets
- [ ] Code pushed to GitHub
- [ ] PostgreSQL database created on Render
- [ ] Web service created on Render
- [ ] All environment variables set in Render dashboard
- [ ] Health check endpoint working
- [ ] User registration and login working
- [ ] Soil image upload working (Cloudinary)
- [ ] Database backups enabled
- [ ] Custom domain configured (optional)
- [ ] HTTPS enabled (automatic on Render)

---

## Scaling for Production

### Free Tier Limits:
- 1 vCPU, 0.5GB RAM
- ~100 concurrent requests
- Database goes to sleep after 15 min inactivity

### Upgrade to Starter/Pro:
1. Render Dashboard → Service → Plan
2. Select **Starter** ($7/month) for production
3. Auto-restart on crash
4. Keep database alive

---

## Monitoring & Logs

### View Logs:
1. Render Dashboard → Your service → Logs
2. Filter by date/level
3. Monitor for errors

### Common Issues:
- Out of memory: Increase RAM
- Slow response: Check database load
- 500 errors: Check application logs

---

## Custom Domain (Optional)

1. Render Dashboard → Service → Settings
2. Add custom domain
3. Update DNS records
4. HTTPS auto-configured

---

## Backup & Recovery

### PostgreSQL Backups:
1. Render Dashboard → Database → Backups
2. Enable automatic backups
3. Test restore procedure

---

## Cost Summary

| Service | Free Tier | Starter |
|---------|-----------|---------|
| Web App | $0 | $7/mo |
| PostgreSQL | $0 | $15/mo |
| Storage | 0.5GB | 10GB |
| **Total** | **$0** | **$22/mo** |

---

## Support & Resources

- Render Docs: https://render.com/docs
- Spring Boot on Render: https://render.com/docs/deploy-spring
- PostgreSQL on Render: https://render.com/docs/databases
- GitHub Integration: https://render.com/docs/github

---

## Next Steps

1. ✅ Create Dockerfile and `.dockerignore`
2. ✅ Create `render.yaml` for IaC
3. ✅ Push code to GitHub
4. ✅ Create PostgreSQL on Render
5. ✅ Deploy web service
6. ✅ Set environment variables
7. ✅ Test all endpoints
8. ✅ Monitor logs and performance

**Estimated Time: 30-45 minutes**

---

**Questions?** Check Render documentation or contact support.

Good luck with your deployment! 🚀
