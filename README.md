# 🌤️ Interactive Live Weather App

![Java](https://img.shields.io/badge/Java-Swing%20%26%20AWT-red)
![API](https://img.shields.io/badge/API-OpenWeatherMap-orange)
![UI](https://img.shields.io/badge/UI-Dynamic%20Animations-blue)

## 📌 Project Overview
A modern, visually engaging desktop application built with Java that fetches and displays real-time weather data. Moving beyond standard static GUIs, this project features a custom-built rendering engine that generates 60 FPS dynamic weather animations (rain, moving clouds, rotating sun) directly reflecting the live weather conditions of the searched city.

## 🚀 Core Features
* **Real-Time Data Fetching:** Integrates with the OpenWeatherMap REST API to pull live metrics including Temperature, Humidity, Wind Speed, Pressure, and 'Feels Like' data.
* **Dynamic Background Engine:** A custom AWT/Swing animation loop that visually alters the application's background based on the current weather state:
  * ☀️ **Clear:** Rotating sun with glowing rays.
  * 🌧️ **Rain:** High-speed falling particle effects.
  * ☁️ **Clouds:** Drifting cloud silhouettes.
* **Modern UI/UX:** Features interactive "glassmorphism-style" metric cards that lift and glow on mouse hover, alongside a custom-painted animated search button and text field.
* **JSON Parsing:** Efficiently parses complex nested JSON API responses using the `org.json` library.

## 🛠️ Tech Stack
* **Language:** Java
* **GUI Framework:** Java Swing & AWT (`javax.swing.*`, `java.awt.*`)
* **API:** OpenWeatherMap API
* **Data Processing:** `org.json` (for JSON parsing)

## 📂 Project Structure
```text
📦 Live-Weather-App
 ┣ 📜 InteractiveWeatherApp.java    # Main application class, UI components, and API logic
 ┗ 📜 README.md                     # Project documentation
