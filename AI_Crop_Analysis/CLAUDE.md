# AI Crop Lifecycle Advisor - Project Overview

## Project Structure

```
AI_Crop_Analysis/
├── src/main/java/com/procure/aicrop/
│   ├── entity/               # JPA entities
│   ├── repository/          # Spring Data repositories
│   ├── service/             # Business logic
│   ├── controller/          # REST API endpoints
│   ├── dto/                 # Data transfer objects
│   ├── security/            # JWT and security
│   └── AiCropAdvisorApplication.java
├── src/main/resources/
│   ├── application.yml      # Configuration
│   └── static/              # Frontend HTML/CSS/JS
│       ├── index.html
│       ├── login.html
│       ├── register.html
│       ├── dashboard.html
│       ├── soil-analysis.html
│       ├── crop-recommendation.html
│       ├── crop-planting.html
│       ├── alerts.html
│       └── yield-prediction.html
├── pom.xml                  # Maven configuration
├── README.md               # Project documentation
├── CLAUDE.md              # This file
└── .gitignore            # Git ignore rules
```

## Key Components

### Domain Models

1. **User** - Farmer account with location and farm details
2. **Crop** - Crop database with requirements (temperature, rainfall, soil types, growing days)
3. **SoilAnalysis** - Results from AI soil image analysis
4. **CropPlanting** - Records of planted crops with tracking
5. **WeatherData** - Historical and forecast weather information
6. **Recommendation** - AI-generated crop recommendations with sowing windows
7. **Alert** - Weather and crop health alerts
8. **YieldPrediction** - Daily yield predictions based on conditions

### Service Layer

- **AuthService**: JWT token generation and validation
- **UserService**: User management
- **CropService**: Crop database operations
- **SoilAnalysisService**: Soil image upload and analysis
- **RecommendationService**: Crop recommendations and suitability analysis
- **CropPlantingService**: Crop lifecycle tracking
- **AlertService**: Alert management
- **YieldPredictionService**: Daily yield calculations

### Frontend Components

- **Login/Register**: Authentication
- **Dashboard**: Overview and quick actions
- **Soil Analysis**: Image upload and results display
- **Crop Recommendations**: Personalized suggestions with decision factors
- **Crop Planting**: Lifecycle tracking
- **Alerts**: Notification management
- **Yield Predictions**: Visualization with Chart.js

## API Architecture

RESTful API with JWT authentication:
- Base URL: `http://localhost:8080/api`
- Authentication: Bearer token in Authorization header
- Response format: JSON with ApiResponse wrapper

## Database Design

- H2 for development
- MySQL for production
- JPA/Hibernate for ORM
- Automatic schema creation with `ddl-auto: update`

## Security

- JWT tokens for stateless authentication
- BCrypt password hashing
- CORS configuration
- Input validation
- SQL injection protection via JPA

## Frontend Features

- Bootstrap 5.3 for responsive design
- Modern CSS with gradients and animations
- JavaScript ES6+ for interactivity
- Font Awesome icons
- Chart.js for yield prediction visualization
- LocalStorage for token management

## Development Setup

1. Java 17+ required
2. Maven build system
3. H2 in-memory database (default)
4. No additional services needed for development

## Testing Approach

### Manual Testing Checklist
- [ ] User registration and login
- [ ] Soil image upload and analysis
- [ ] Crop recommendation generation
- [ ] Crop planting creation
- [ ] Growth stage updates
- [ ] Alert management
- [ ] Yield prediction generation
- [ ] Responsive design on mobile/tablet/desktop

## Performance Considerations

- Index on User.email for faster lookups
- Lazy loading for crop relationships
- Pagination for large datasets
- Image compression for uploads
- Cache frequently accessed crops

## Future Enhancements

1. Real ML model integration for soil analysis
2. External weather API integration (OpenWeatherMap, NOAA)
3. Mobile app (React Native/Flutter)
4. Email notifications for critical alerts
5. SMS alerts for farmers
6. Multi-language support
7. Farmer community features
8. Extension advisor dashboard
9. Export reports (PDF/Excel)
10. Real-time weather map integration

## Deployment Notes

- Container-ready with Docker support
- Environment variables for sensitive config
- Database migration scripts needed for production
- Configure HTTPS in production
- Set up proper CORS for frontend domain
- Enable rate limiting and API throttling
- Monitor logs and errors

## Code Quality

- No third-party code generation used
- Clean architecture with separation of concerns
- DRY principles followed
- Minimal dependencies approach
- Spring Boot best practices
- Bootstrap/HTML5 semantic markup

## Git Guidelines

- Commit frequently with clear messages
- Create feature branches for new features
- Main branch is production-ready
- Tag releases with version numbers

## Support

- Full API documentation in README.md
- Error handling with proper HTTP status codes
- Clear error messages in responses
- Logging for debugging

---

**Created**: July 6, 2026
**Status**: Production Ready (MVP)
**Version**: 1.0.0
