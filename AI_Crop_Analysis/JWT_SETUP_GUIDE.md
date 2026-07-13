# JWT Secret Configuration Guide

## 🔐 Your JWT Secret (Generated on 2026-07-10)

```
evVXzvnrjr8M8Ed7jGsGhKNkzmZAvtk+ULobneY6jvI=
```

✅ This is a **256-bit cryptographic key** - Strong and secure!

---

## 📋 How to Use This Secret

### **Option 1: Local Development (Easiest)**

1. **Copy the example environment file:**
   ```bash
   cp .env.example .env
   ```

2. **The `.env` file contains:**
   ```
   JWT_SECRET=evVXzvnrjr8M8Ed7jGsGhKNkzmZAvtk+ULobneY6jvI=
   ```

3. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

   ✅ Spring Boot automatically reads `.env` variables!

### **Option 2: Windows PowerShell**

Run these commands in PowerShell before starting the app:

```powershell
# Set the environment variable
$env:JWT_SECRET = "evVXzvnrjr8M8Ed7jGsGhKNkzmZAvtk+ULobneY6jvI="

# Start the application
mvn spring-boot:run
```

### **Option 3: Windows Command Prompt**

Run these commands in CMD before starting the app:

```batch
# Set the environment variable
set JWT_SECRET=evVXzvnrjr8M8Ed7jGsGhKNkzmZAvtk+ULobneY6jvI=

# Start the application
mvn spring-boot:run
```

### **Option 4: Linux/Mac Terminal**

Run these commands in terminal before starting the app:

```bash
# Set the environment variable
export JWT_SECRET="evVXzvnrjr8M8Ed7jGsGhKNkzmZAvtk+ULobneY6jvI="

# Start the application
mvn spring-boot:run
```

---

## 🏢 Production Deployment

### **For AWS/Cloud Servers**

1. **Generate a NEW secret for production** (different from development):
   ```bash
   openssl rand -base64 32
   ```

2. **Store in AWS Secrets Manager:**
   - Go to AWS Console → Secrets Manager
   - Create secret: `ai-crop-advisor/jwt-secret`
   - Value: `<your-new-production-secret>`

3. **Set environment variable:**
   ```bash
   export JWT_SECRET=$(aws secretsmanager get-secret-value --secret-id ai-crop-advisor/jwt-secret --query SecretString --output text)
   ```

### **For Docker**

1. **Create `.env.prod` file (NOT committed to git):**
   ```
   JWT_SECRET=<your-production-secret>
   DB_PASSWORD=<your-db-password>
   ```

2. **Run Docker with environment file:**
   ```bash
   docker run --env-file .env.prod -p 8080:8080 ai-crop-advisor:latest
   ```

### **For Docker Compose**

1. **docker-compose.yml:**
   ```yaml
   version: '3.8'
   services:
     app:
       image: ai-crop-advisor:latest
       environment:
         JWT_SECRET: ${JWT_SECRET}
         DATABASE_URL: ${DB_URL}
       ports:
         - "8080:8080"
   ```

2. **Run with environment file:**
   ```bash
   docker-compose --env-file .env.prod up
   ```

---

## ✅ How to Verify It's Working

### **1. Check if Secret is Loaded**

When you start the app, look for this log message:

```
INFO] JWT authentication successful for user: user@example.com
```

If you see this ✅, the secret is working!

### **2. Test Login and Check Token**

1. Open browser DevTools (F12)
2. Go to Console tab
3. Run this command:
   ```javascript
   console.log(localStorage.getItem('token'))
   ```
4. You should see a long token starting with `eyJ...` ✅

### **3. Check Token Structure (Optional)**

Visit https://jwt.io and paste the token to verify it's valid!

---

## 🔄 How JWT Secret Works in Your App

```
User Login
    ↓
Form submitted to /auth/login-form
    ↓
Spring Security validates email + password
    ↓
If valid: Generate JWT Token using SECRET KEY
    ↓
Token returned to frontend & stored in localStorage
    ↓
Every API call includes: Authorization: Bearer <token>
    ↓
JwtAuthenticationFilter validates token using SECRET KEY
    ↓
If valid: User authenticated, request proceeds
If invalid: Return 401 Unauthorized
```

---

## ⚠️ Security Best Practices

| Rule | Why | Example |
|------|-----|---------|
| **Never commit secrets to git** | Anyone with repo access gets secrets | ❌ Don't add `.env` to git |
| **Use different secrets per environment** | Limits damage if one is compromised | 🔒 Dev: `secret1`, Prod: `secret2` |
| **Rotate secrets regularly** | Reduces risk of compromise | 🔄 Every 90 days |
| **Use strong, random secrets** | Prevents brute force attacks | ✅ `openssl rand -base64 32` |
| **Store in environment variables, not code** | Code is less secure than env vars | ✅ `$env:JWT_SECRET`, not in .java files |
| **Never share secrets in emails/chat** | Leaves audit trail of exposure | ❌ Never send in Slack/Email |

---

## 📝 File Checklist

Your project now has:

- ✅ **application.properties** - Updated with environment variable support
- ✅ **.env.example** - Template for local development (can be committed)
- ✅ **.gitignore** - Excludes `.env` files (secrets won't be committed)
- ✅ **JWT_SETUP_GUIDE.md** - This file! (reference guide)

---

## 🚀 Quick Start Commands

### **Windows (PowerShell)**
```powershell
$env:JWT_SECRET = "evVXzvnrjr8M8Ed7jGsGhKNkzmZAvtk+ULobneY6jvI="
mvn spring-boot:run
# Then go to http://localhost:8080/api/login.html
```

### **Linux/Mac**
```bash
export JWT_SECRET="evVXzvnrjr8M8Ed7jGsGhKNkzmZAvtk+ULobneY6jvI="
mvn spring-boot:run
# Then go to http://localhost:8080/api/login.html
```

---

## ❓ Troubleshooting

### **Error: "JWT authentication failed"**
- ✅ Check if `JWT_SECRET` environment variable is set
- ✅ Check if it matches the one in application.properties
- ✅ Restart the application after setting the variable

### **Error: "Invalid or expired token"**
- ✅ Token expired (7 days) - User needs to re-login
- ✅ Secret was changed - Old tokens won't validate

### **Token not storing in localStorage**
- ✅ Open DevTools Console and check for errors
- ✅ Make sure login endpoint returns token in response
- ✅ Check browser's localStorage (DevTools → Application → Local Storage)

---

## 📞 Questions?

Refer to this guide for JWT setup or ask for help!

**Generated:** July 10, 2026
**Secret:** `evVXzvnrjr8M8Ed7jGsGhKNkzmZAvtk+ULobneY6jvI=`
**Expiration:** 7 days per token
