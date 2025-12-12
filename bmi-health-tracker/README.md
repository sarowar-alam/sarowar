# BMI & Health Tracker

A full-stack web application for tracking Body Mass Index (BMI), Basal Metabolic Rate (BMR), and daily calorie requirements with trend visualization.

## 🚀 Quick Start

### Prerequisites
- Node.js 18+ (LTS)
- PostgreSQL 12+
- npm or yarn

### Local Development

1. **Clone the repository**
   ```bash
   git clone <your-repo-url>
   cd bmi-health-tracker
   ```

2. **Setup Backend**
   ```bash
   cd backend
   npm install
   cp .env.example .env
   # Edit .env with your database credentials
   ```

3. **Setup Database**
   ```bash
   # Create database and user
   sudo -u postgres createuser --pwprompt bmi_user
   sudo -u postgres createdb -O bmi_user bmidb
   
   # Run migrations
   psql -U bmi_user -d bmidb -h localhost -f migrations/001_create_measurements.sql
   ```

4. **Setup Frontend**
   ```bash
   cd ../frontend
   npm install
   ```

5. **Run the Application**
   ```bash
   # Terminal 1 - Backend
   cd backend
   npm run dev
   
   # Terminal 2 - Frontend
   cd frontend
   npm run dev
   ```

6. **Access the application**
   - Frontend: http://localhost:5173
   - Backend API: http://localhost:3000/api
   - Health Check: http://localhost:3000/health

## 📚 Documentation

- **[AGENT.md](AGENT.md)** - Complete project documentation
- **[CONNECTIVITY.md](CONNECTIVITY.md)** - 3-tier connectivity configuration
- **[BMI_Health_Tracker_Deployment_Readme.md](BMI_Health_Tracker_Deployment_Readme.md)** - AWS EC2 deployment guide

## 🏗️ Project Structure

```
bmi-health-tracker/
├── backend/           # Node.js + Express API
│   ├── src/
│   │   ├── server.js
│   │   ├── routes.js
│   │   ├── db.js
│   │   └── calculations.js
│   ├── migrations/
│   └── package.json
├── frontend/          # React + Vite
│   ├── src/
│   │   ├── components/
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── vite.config.js
│   └── package.json
└── README.md
```

## 🌟 Features

- ✅ **Modern Professional UI** - Card-based design with gradient backgrounds and smooth animations
- ✅ **Real-time Stats Dashboard** - Visual stat cards showing BMI, BMR, daily calories, and total records
- ✅ **BMI Calculation** - Instant BMI calculation with health categorization
- ✅ **BMR (Basal Metabolic Rate)** - Calculate your resting metabolic rate
- ✅ **Daily Calorie Needs** - Personalized based on activity level
- ✅ **30-Day BMI Trend** - Beautiful chart visualization of your progress
- ✅ **Historical Tracking** - View all measurements with color-coded badges
- ✅ **Fully Responsive** - Optimized for desktop, tablet, and mobile devices
- ✅ **Enhanced UX** - Loading states, success/error alerts with animations, empty state messages
- ✅ **Professional Form Design** - Multi-column responsive forms with focus states

## 🔧 Tech Stack

**Frontend:**
- React 18
- Vite 5
- Chart.js
- Axios

**Backend:**
- Node.js
- Express
- PostgreSQL
- PM2 (production)

## 📦 Deployment

See [BMI_Health_Tracker_Deployment_Readme.md](BMI_Health_Tracker_Deployment_Readme.md) for complete AWS EC2 Ubuntu deployment instructions.

## 🔒 Security

- Environment-based CORS configuration
- Parameterized SQL queries (SQL injection protection)
- Input validation
- Error handling without internal exposure

## 📄 License

MIT

## 👨‍💻 Author

Your Name

---

**Last Updated:** December 12, 2025
