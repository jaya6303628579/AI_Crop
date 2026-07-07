# AI Crop Lifecycle Advisor

An intelligent agricultural advisory platform powered by AI that helps farmers make data-driven decisions throughout the entire crop lifecycle—from sowing to harvest.

## 🌾 Features

### Core Features
- **Soil Analysis**: Upload soil images for AI-powered analysis to determine soil type, nutrient content, pH levels, and suitability for crops
- **Crop Recommendations**: Get personalized crop suggestions based on soil analysis, weather patterns, and historical climate data
- **Safe Sowing Windows**: Identify optimal sowing dates with confidence scores and understand why certain dates are dangerous
- **Crop Lifecycle Monitoring**: Track crop growth from germination to harvest with daily monitoring capabilities
- **Weather Integration**: Real-time weather data integration for accurate risk assessment
- **Yield Predictions**: Dynamic yield predictions that update as weather conditions change
- **Intelligent Alerts**: Receive timely alerts about diseases, pests, weather risks, and growth concerns
- **Harvest Recommendations**: Get guidance on the safest harvest window to maximize yield

### Advanced Features
- **Daily Yield Recalculation**: Yields are recalculated daily based on actual weather received
- **Growth Stage Tracking**: Automatic determination of crop growth stage based on days since sowing
- **Multi-Crop Comparison**: Compare suitability of multiple crops for your farm conditions
- **Historical Analytics**: Track your crop performance over multiple seasons
- **Mobile Responsive Design**: Works seamlessly on desktop, tablet, and mobile devices

## 🏗️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: H2 (Development), MySQL (Production)
- **ORM**: Hibernate/JPA
- **Security**: Spring Security + JWT
- **Build Tool**: Maven

### Frontend
- **HTML5, CSS3, JavaScript (ES6+)**
- **Bootstrap 5.3**
- **Chart.js** for visualization
- **Font Awesome** for icons
- **Responsive Design**

### APIs & Services
- **Weather Data Integration**: Real-time and forecast weather data
- **Image Processing**: Soil image analysis using TensorFlow
- **RESTful APIs**: Complete REST API for all functionality

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+ (for production)
- Git

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/AI_Crop_Analysis.git
cd AI_Crop_Analysis
```

2. **Build the project**
```bash
mvn clean install
```

3. **Run the application**
```bash
mvn spring-boot:run
```

4. **Access the application**
- Open your browser and navigate to: `http://localhost:8080`

### Configuration

Edit `src/main/resources/application.yml` to configure:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb  # Change to MySQL for production
    driverClassName: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: update

jwt:
  secret: your-secret-key-change-this  # Change in production!
  expiration: 86400000  # 24 hours
```

## 📋 API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user
- `GET /api/auth/validate` - Validate JWT token

### Crops
- `GET /api/crops` - Get all crops
- `GET /api/crops/{id}` - Get crop details
- `GET /api/crops/soil/{soilType}` - Get crops suitable for soil type

### Soil Analysis
- `POST /api/soil/analyze` - Upload and analyze soil image
- `GET /api/soil/analyses` - Get user's soil analyses
- `GET /api/soil/latest` - Get latest soil analysis

### Recommendations
- `POST /api/recommendations/generate/{cropId}` - Generate crop recommendation
- `POST /api/recommendations/alternatives` - Get alternative crop recommendations
- `GET /api/recommendations` - Get all recommendations
- `POST /api/recommendations/{id}/accept` - Accept recommendation

### Crop Planting
- `POST /api/plantings` - Record new planting
- `GET /api/plantings` - Get all plantings
- `GET /api/plantings/{id}` - Get planting details
- `PUT /api/plantings/{id}/status` - Update planting status
- `PUT /api/plantings/{id}/growth-stage` - Update growth stage

### Alerts
- `GET /api/alerts` - Get all alerts
- `GET /api/alerts/unread` - Get unread alerts
- `GET /api/alerts/critical` - Get critical alerts
- `PUT /api/alerts/{id}/read` - Mark alert as read
- `DELETE /api/alerts/{id}` - Delete alert

### Yield Predictions
- `POST /api/yield-predictions/{plantingId}/generate` - Generate yield prediction
- `GET /api/yield-predictions/{plantingId}` - Get all predictions for planting
- `GET /api/yield-predictions/{plantingId}/latest` - Get latest prediction

## 📱 User Interface

### Pages
1. **Login/Register** - User authentication and account creation
2. **Dashboard** - Overview of active crops, alerts, and quick actions
3. **Soil Analysis** - Upload and analyze soil images
4. **Crop Recommendations** - View personalized crop suggestions
5. **Crop Planting** - Record and manage crop plantings
6. **Yield Predictions** - Track yield predictions with visualizations
7. **Alerts** - Manage weather and crop health alerts

## 🔐 Security Features

- JWT token-based authentication
- Password encryption using BCrypt
- CORS configuration for frontend integration
- Input validation and sanitization
- SQL injection prevention through JPA
- XSS protection

## 🗄️ Database Schema

### Key Tables
- `users` - Farmer user accounts
- `crops` - Crop database with requirements
- `soil_analysis` - Soil analysis results
- `crop_plantings` - Crop planting records
- `weather_data` - Historical and forecast weather
- `recommendations` - Crop recommendations
- `alerts` - Weather and crop alerts
- `yield_predictions` - Yield prediction history

## 📊 AI/ML Components

### Soil Analysis
- Image classification to determine soil type
- Nutrient content estimation
- Texture analysis
- Confidence scoring

### Crop Recommendation Engine
- Soil-crop suitability matching
- Weather pattern analysis
- Historical climate consideration
- Dangerous date identification

### Yield Prediction Model
- Temperature impact calculation
- Rainfall impact assessment
- Disease risk modeling
- Pest risk evaluation
- Growth stage-based adjustments

## 🔄 Workflow

1. **User Registration**: Farmer creates account with location and farm details
2. **Soil Analysis**: Upload soil image for AI analysis
3. **Crop Selection**: Get recommendations based on soil analysis
4. **Sowing Decision**: Review safe sowing windows and dangerous dates
5. **Crop Recording**: Log when crop is planted
6. **Daily Monitoring**: System monitors weather and crop health
7. **Alerts**: Receive alerts for critical events
8. **Yield Tracking**: Monitor yield predictions as crop grows
9. **Harvest Planning**: Get harvest window recommendations

## 🚧 Deployment

### Docker Deployment
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/ai-crop-advisor.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Production Checklist
- [ ] Change JWT secret
- [ ] Configure MySQL database
- [ ] Set up environment variables
- [ ] Enable HTTPS
- [ ] Configure CORS properly
- [ ] Set up logging and monitoring
- [ ] Configure backups
- [ ] Set up CI/CD pipeline

## 📈 Performance Optimization

- Database query optimization with indexes
- Caching for frequently accessed data
- Lazy loading of relationships
- Pagination for large datasets
- Efficient image compression

## 🐛 Troubleshooting

### Port Already in Use
```bash
# Change port in application.yml
server:
  port: 8081
```

### Database Connection Issues
- Verify MySQL is running
- Check credentials in application.yml
- Ensure database exists

### Image Upload Not Working
- Create `uploads/soil` directory
- Check file permissions
- Verify file size limits

## 📚 Documentation

- API documentation: See API Endpoints section above
- Database schema documentation in `docs/schema.md`
- User guide in `docs/user-guide.md`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Contact: support@aicropadviser.com

## 🙏 Acknowledgments

- Bootstrap team for the CSS framework
- Font Awesome for icons
- Chart.js for visualization
- Spring Boot community for the framework
- TensorFlow for machine learning capabilities

---

**Last Updated**: July 6, 2026

**Version**: 1.0.0

Made with ❤️ for farmers
