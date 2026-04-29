# SentinelAI-UI

A simple React frontend for SentinelAI log analysis.

## Prerequisites
- Node.js (v16 or newer recommended)
- npm (comes with Node.js) or yarn

## Getting Started

1. **Install dependencies:**
   ```bash
   npm install
   # or
   yarn install
   ```

2. **Start the development server:**
   ```bash
   npm start
   # or
   yarn start
   ```
   This will open the app at http://localhost:3000 by default.

3. **API Integration:**
   - The UI calls the SentinelAI-Core API at `POST /api/rca/analyze` for both pasted text and uploaded files (files are read as text in the browser).
   - For local development, the UI proxies API requests to the Java backend via `package.json`:
     ```json
     "proxy": "http://localhost:8080"
     ```
     (Replace 8080 with your backend port.)

## Features
- Paste log text or upload a log file
- Submit for RCA analysis
- View results from backend (history or AI/ML)

## Learning React
- This project uses functional components and hooks (useState).
- You can extend it as you learn more about React!

---

For any issues, check your backend API is running and accessible from the UI.
