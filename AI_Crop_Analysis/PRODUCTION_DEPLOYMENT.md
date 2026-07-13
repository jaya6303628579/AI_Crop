# 🚀 Production Deployment Guide

## ⚠️ CRITICAL: JWT Secret for Production

### **Your Production JWT Secret (Keep This SAFE!)**
```
OTmDhLAnchHLcdODBdERRWtWZL5HOnFnqhlS84/ENqo=
```

🔐 **This is for PRODUCTION ONLY**
- ✅ Different from development secret
- ✅ Generate this ONLY once
- ✅ Store in secure environment variables
- ✅ NEVER commit to git
- ✅ NEVER share in public channels

---

## 📋 Deployment Checklist

### **Before Deploying to Production**

- [ ] Generate a NEW JWT secret (different from development)
- [ ] Never hardcode the secret in code
- [ ] Never commit secrets to git
- [ ] Use environment variables only
- [ ] Use HTTPS (secure connection)
- [ ] Use strong database password
- [ ] Use production-grade database (not SQLite/H2)
- [ ] Set up proper logging and monitoring
- [ ] Test authentication flows
- [ ] Backup database regularly

---

## 🔧 Setting Up in Production

### **For AWS EC2 / Linux Server**

**1. Create a secrets file (NOT in git):**
```bash
sudo nano /etc/ai-crop-advisor/.env.production
```

**2. Add your production secrets:**
```
JWT_SECRET=OTmDhLAnchHLcdODBdERRWtWZL5HOnFnqhlS84/ENqo=
DATABASE_URL=jdbc:postgresql://prod-db-server:5432/aicrop
DATABASE_USERNAME=prod_user
DATABASE_PASSWORD=<strong-password>
GROQ_API_KEY=<production-api-key>
WEATHER_API_KEY=<production-api-key>
```

**3. Restrict file permissions:**
```bash
sudo chmod 600 /etc/ai-crop-advisor/.env.production
sudo chown ai-crop-advisor:ai-crop-advisor /etc/ai-crop-advisor/.env.production
```

**4. Load environment and start app:**
```bash
source /etc/ai-crop-advisor/.env.production
java -jar ai-crop-advisor.jar
```

---

### **For AWS Secrets Manager (Recommended)**

**1. Create secret in AWS Console:**
```
Secrets Manager → Create Secret
Name: ai-crop-advisor/production/jwt-secret
Value: OTmDhLAnchHLcdODBdERRWtWZL5HOnFnqhlS84/ENqo=
```

**2. In your application code:**
```bash
# Fetch secret at runtime
export JWT_SECRET=$(aws secretsmanager get-secret-value \
  --secret-id ai-crop-advisor/production/jwt-secret \
  --region us-east-1 \
  --query SecretString --output text)
```

**3. Start the application:**
```bash
java -jar ai-crop-advisor.jar
```

---

### **For Docker (Production)**

**1. Create a secure .env file (NOT in git):**
```bash
# .env.production (NOT COMMITTED TO GIT)
JWT_SECRET=OTmDhLAnchHLcdODBdERRWtWZL5HOnFnqhlS84/ENqo=
DB_URL=jdbc:postgresql://postgres-container:5432/aicrop
DB_USERNAME=prod_user
DB_PASSWORD=<strong-password>
```

**2. Docker Compose:**
```yaml
version: '3.8'

services:
  app:
    image: ai-crop-advisor:latest
    ports:
      - "8080:8080"
    environment:
      JWT_SECRET: ${JWT_SECRET}
      DATABASE_URL: ${DB_URL}
      DATABASE_USERNAME: ${DB_USERNAME}
      DATABASE_PASSWORD: ${DB_PASSWORD}
    depends_on:
      - postgres

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_USER: prod_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_DB: aicrop
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

**3. Run with production secrets:**
```bash
docker-compose --env-file .env.production up -d
```

---

### **For Kubernetes (Enterprise)**

**1. Create a Kubernetes secret:**
```bash
kubectl create secret generic ai-crop-secrets \
  --from-literal=jwt_secret=OTmDhLAnchHLcdODBdERRWtWZL5HOnFnqhlS84/ENqo= \
  --from-literal=db_password=<strong-password> \
  -n production
```

**2. Use in deployment:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-crop-advisor
spec:
  template:
    spec:
      containers:
      - name: app
        image: ai-crop-advisor:latest
        env:
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: ai-crop-secrets
              key: jwt_secret
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: ai-crop-secrets
              key: db_password
```

**3. Deploy:**
```bash
kubectl apply -f deployment.yaml -n production
```

---

## 🔑 Secrets Management Best Practices

### **DO (✅)**
- ✅ Use environment variables
- ✅ Store in AWS Secrets Manager
- ✅ Use HashiCorp Vault
- ✅ Restrict file permissions (chmod 600)
- ✅ Rotate secrets every 90 days
- ✅ Use different secrets per environment
- ✅ Log access to secrets
- ✅ Audit who accessed secrets

### **DON'T (❌)**
- ❌ Hardcode secrets in code
- ❌ Commit secrets to git
- ❌ Share secrets in email/Slack
- ❌ Use same secret for multiple environments
- ❌ Store in plain text files
- ❌ Put secrets in Docker images
- ❌ Log secret values
- ❌ Use weak/short secrets

---

## 📊 Environment Comparison

| Setting | Development | Production |
|---------|-------------|-----------|
| **JWT Secret** | `evVXzvnrjr8M8Ed7jGsGhKNkzmZAvtk+ULobneY6jvI=` | `OTmDhLAnchHLcdODBdERRWtWZL5HOnFnqhlS84/ENqo=` |
| **Database** | Local PostgreSQL | RDS/Cloud DB |
| **HTTPS** | ❌ (http://localhost) | ✅ (https://domain.com) |
| **Logging** | DEBUG | INFO/ERROR only |
| **CORS** | localhost:3000 | yourdomain.com |
| **Token Expiry** | 7 days | 7 days (configurable) |
| **Storage** | .env file | Environment variables/Secrets Manager |

---

## 🧪 Testing Production Setup

### **1. Verify JWT Secret is Loaded**
```bash
# In app logs, should show:
# [INFO] JwtAuthenticationFilter - JWT authentication successful
```

### **2. Test Token Generation**
```bash
curl -X POST http://localhost:8080/api/auth/login-form \
  -d "email=user@example.com&password=Password123" \
  -v
# Should return token in response
```

### **3. Test Protected Endpoint**
```bash
# Get token from step 2
TOKEN="<your-jwt-token>"

curl -X GET http://localhost:8080/api/plantings \
  -H "Authorization: Bearer $TOKEN"
# Should return user's plantings
```

### **4. Test Expired Token**
```bash
# Use old/invalid token
curl -X GET http://localhost:8080/api/plantings \
  -H "Authorization: Bearer invalid-token"
# Should return 401 Unauthorized
```

---

## 🔄 Secret Rotation

Every 90 days, generate a new secret:

```bash
# Generate new secret
NEW_SECRET=$(openssl rand -base64 32)
echo "New Secret: $NEW_SECRET"

# Update in your secrets manager
# Deploy new version with new secret
# Old tokens will expire (7 days) automatically
# Users re-login to get new token
```

---

## 📞 Security Audit Checklist

Before going live:

- [ ] JWT secret is NOT in git
- [ ] JWT secret is NOT in code
- [ ] JWT secret is NOT in logs
- [ ] JWT secret is stored in environment variables
- [ ] JWT secret is different from development
- [ ] Database password is strong (16+ characters)
- [ ] HTTPS is enabled
- [ ] Firewall configured
- [ ] Database backups configured
- [ ] Monitoring and alerts configured
- [ ] Logging configured (not logging secrets)
- [ ] Rate limiting configured
- [ ] CORS properly configured
- [ ] Security headers configured (HSTS, CSP, etc.)

---

## 🆘 Emergency: Secret Compromised

If your production JWT secret is exposed:

1. **Immediately:**
   - Generate new secret
   - Update in secrets manager
   - Restart application

2. **Within 24 hours:**
   - Review access logs
   - Check for unauthorized access
   - Rotate database password
   - Check for data breaches

3. **Communication:**
   - Notify security team
   - Document incident
   - Update incident log

---

## 📝 Summary

| Environment | Secret | Storage | Rotation |
|-------------|--------|---------|----------|
| **Development** | `evVXzvn...` | .env file | As needed |
| **Staging** | (Different) | Env variables | Monthly |
| **Production** | `OTmDhLA...` | Secrets Manager | Every 90 days |

---

**Generated:** July 10, 2026
**Status:** Ready for Production
**Security Level:** Enterprise-Grade ✅
