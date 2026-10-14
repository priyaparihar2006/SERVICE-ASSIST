# 🛠️ ServiceAssist (Servora)

<div align="center">

[![Live Web App](https://img.shields.io/badge/🚀_Live_Demo-assist--services.vercel.app-00DC82?style=for-the-badge&logo=vercel&logoColor=white)](https://assist-services.vercel.app/)
[![React 19](https://img.shields.io/badge/React-19.0-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.8-3178C6?style=for-the-badge&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Vite](https://img.shields.io/badge/Vite-6.2-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev/)
[![TailwindCSS](https://img.shields.io/badge/Tailwind_CSS-v4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)

**The all-in-one on-demand home services booking platform and partner ecosystem.**

[🌐 Explore Live Website](https://assist-services.vercel.app/) • [📱 Mobile App Setup](#-android-mobile-app) • [💻 Web Frontend](#-web-frontend) • [⚙️ Backend API](#-backend-api)

---

</div>

## 🌐 Live Web Deployment

The web application is live and deployed on Vercel:

> **🔗 Production URL:** **[https://assist-services.vercel.app/](https://assist-services.vercel.app/)**

---

## ✨ Features

### 🌟 Web Platform ([https://assist-services.vercel.app/](https://assist-services.vercel.app/))
- **Hero & 3D Interactive Experience**: Immersive video background, interactive technician showcase, and smooth motion animations.
- **🤖 AI Diagnostic Assistant**: Integrated Gemini-powered diagnostic tool helping customers troubleshoot issues and recommend exact services.
- **📦 Service Catalog & Categories**: Comprehensive catalog for AC Servicing, Electrical, Deep Cleaning, Plumbing, Appliance Repair, and Painting.
- **📅 Instant Booking & Scheduling**: Modal-based booking flow with real-time slot selection, address selection, and price breakdown.
- **🛒 Cart Drawer & Floating Bar**: Persistent cart drawer and responsive bottom bar for effortless multi-service bookings.
- **🤝 Pro Partner Ecosystem**: Partner onboarding workflow, performance metrics, and duty management.
- **🔐 Secure Authentication**: Role-gated dashboards for Customers, Service Professionals, and Administrators.

### 📱 Android Mobile Application
- **Jetpack Compose UI**: Modern, fluid, reactive UI built with Kotlin and Material 3.
- **🌈 Dynamic Rainbow Themes**: User-selectable appearance palette (Red, Orange, Yellow, Green, Blue, Indigo, Violet, Rose) with full Dark & Light mode contrast.
- **⚡ Offline-First Architecture**: Powered by Room Database and Supabase real-time synchronization.
- **💬 Real-Time In-App Chat**: 1-to-1 customer-partner chat with instant status updates and OTP verification on job start.

---

## 🏗️ Project Architecture

```
ServiceAssist/
├── frontend/             # React 19 + TypeScript + Tailwind CSS web application
│   ├── src/
│   │   ├── components/   # UI components (Hero, BookingModal, AiDiagnostic, CartDrawer)
│   │   ├── pages/        # Route pages (Home, Services, CustomerDashboard, etc.)
│   │   └── services/     # API and WebSocket client services
│   └── vercel.json       # Vercel SPA routing and asset rewrite configuration
├── backend/              # Node.js + Express + Socket.IO + PostgreSQL backend
│   └── src/              # Routes, controllers, database models, and auth middleware
├── app/                  # Android mobile app (Kotlin + Jetpack Compose)
│   └── src/main/java/com/example/
│       ├── data/         # Room DB, Supabase client, models, and repositories
│       └── ui/           # Compose screens, viewmodels, and ServoraTheme system
├── api/                  # Vercel serverless function entry points
└── vercel.json           # Root Vercel full-stack deployment configuration
```

---

## 🚀 Getting Started

### 🌐 Web Frontend

#### Prerequisites
- Node.js `22.x`
- npm `10+`

#### Installation & Development
```bash
# Navigate to frontend
cd frontend

# Install dependencies
npm install

# Start Vite dev server
npm run dev
```
Open [http://localhost:5173](http://localhost:5173) in your browser.

#### Production Build
```bash
npm run build
```

---

### 📱 Android Mobile App

#### Prerequisites
- [Android Studio Ladybug or newer](https://developer.android.com/studio)
- JDK 17+
- Android SDK 34+

#### Running the Mobile App
1. Open Android Studio.
2. Select **Open** and choose the `ServiceAssist` root directory.
3. Allow Gradle to sync dependencies.
4. Create a `.env` file in the project root with your configuration:
   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   ```
5. Select an Android emulator or connect a physical device with USB debugging enabled.
6. Click **Run (`Shift + F10`)**.

---

### ⚙️ Backend API

#### Local Development
```bash
cd backend
npm install
cp .env.example .env
# Configure your DATABASE_URL and JWT_SECRET in .env
npm run dev
```

---

## 🚢 Deployment on Vercel

The web application is configured for deployment using `vercel.json`:

```json
{
  "buildCommand": "cd backend && npm install && npm run build && cd ../frontend && npm install && npm run build",
  "outputDirectory": "frontend/dist"
}
```

- **Live URL**: [https://assist-services.vercel.app/](https://assist-services.vercel.app/)

---

## 📄 License

This project is licensed under the MIT License.
