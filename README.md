# Mastake – Personal Notes & IoT Sync App

**Mastake** is a smart personal notes and to-do application for Android, built using Java and modern Android development technologies. It combines local-first note management, cloud synchronization, AI-powered note assistance, and Bluetooth Low Energy (BLE) communication with an ESP32 microcontroller.

The application is designed to help users manage their personal notes efficiently, access them across devices, and synchronize notes with an external IoT display.

Developed as a final university project for the **Mobile Application and Development Lab**.

---

## 👨‍💻 Author Information

| Information | Details |
|---|---|
| **Project Name** | Mastake |
| **Developer** | Md Siamul Islam Soaib |
| **Roll Number** | 04 |
| **Batch** | D85 |
| **Platform** | Android |
| **Project Type** | University Lab Project |

---

## ✨ Features

### 1. Authentication & User Management
- Secure Google Sign-In.
- Email and password authentication.
- Firebase Authentication integration.
- Android Credential Manager support.

### 2. Personal Notes Management
- Create, edit, and delete personal notes.
- Local-first data storage using Room Database.
- Cloud synchronization with Firebase Firestore.
- Access locally stored notes without an internet connection.

### 3. AI-Powered Note Assistant
- Integrated Google Gemini AI (`gemini-1.5-flash`).
- Automatically organizes unstructured notes.
- Refines and improves raw note content.
- Formats notes for better readability.

### 4. IoT Synchronization (ESP32 + BLE)
- Connects to an ESP32 microcontroller using Bluetooth Low Energy.
- Synchronizes personal notes with an external TFT display.
- Transfers data in background chunks.
- Receives synchronization acknowledgements from the ESP32.

### 5. Home Screen Widget
- Quick access to note creation.
- Create notes directly from the Android home screen.

---

## 🎓 University Lab Brief Features

The following features were implemented to fulfill additional university lab requirements.

### 6. Image to Text
- Capture images using the device camera.
- Select images from the device gallery.
- Uses `FileProvider` and `ActivityResultContracts`.
- Safely previews downscaled images to prevent OutOfMemory (OOM) errors.
- Prepares image content for OCR processing.

### 7. Shake to Sync
- Detects phone shaking using `SensorManager` and the accelerometer.
- Automatically triggers note synchronization with the ESP32.
- Provides haptic feedback using `VibratorManager` / `Vibrator`.
- Allows users to trigger synchronization through a physical gesture.

### 8. Share & Open

A dedicated actions screen provides the following functionalities:

- **Share My Notes:** Exports and shares personal notes using `ACTION_SEND` with `text/plain`.
- **Open GitHub Repository:** Opens the project repository using `ACTION_VIEW`.
- **Call Support:** Opens the phone dialer using `ACTION_DIAL`.
- **Email Developer:** Opens an email application using `ACTION_SENDTO` with a `mailto:` URI.

---

## 🛠️ Technology Stack

| Category | Technologies |
|---|---|
| Programming Language | Java |
| UI Development | XML |
| UI Components | Material 3 |
| View Management | ViewBinding |
| Local Database | Room Database |
| Authentication | Firebase Authentication |
| Cloud Database | Firebase Firestore |
| AI Integration | Google Gemini AI |
| Bluetooth Communication | Android BLE |
| IoT Hardware | ESP32 |
| Display Module | ILI9341 TFT |
| Device Sensors | Android SensorManager |
| Build System | Gradle |

---

## 🚀 Getting Started

Follow these instructions to set up and run the project locally.

### Prerequisites

Make sure you have the following installed:

- Android Studio (Koala or newer recommended).
- Android SDK (Compile SDK 36).
- Android device or emulator running Android API 24 or higher.
- Java Development Kit (JDK).
- ESP32 hardware (optional, required for IoT synchronization testing).

### 1. Clone the Repository

```bash
https://github.com/mdsiamulislam/Mastake

cd mastake
```

### 2. Configure Local Properties

Create or update the `local.properties` file in the project root directory.

Add your Android SDK location and Gemini API key:

```properties
sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk

GEMINI_API_KEY=your_gemini_api_key_here
```

**Important:** Keep API keys private. Never commit `local.properties` or sensitive credentials to GitHub.

### 3. Configure Firebase

- Create a Firebase project in the [Firebase Console](https://console.firebase.google.com/).
- Register your Android application using its package name.
- Download the `google-services.json` configuration file.
- Place it inside the app module directory.
- Enable Firebase Authentication and configure the required sign-in methods.
- Set up Firebase Firestore.

### 4. Sync Gradle Dependencies

Open the project in Android Studio and select:

`File → Sync Project with Gradle Files`

Wait for Gradle synchronization to complete.

### 5. Build and Run

- Connect an Android device with USB Debugging enabled or launch an emulator.
- Select the application configuration.
- Click the **Run (▶)** button.

Alternatively, build the debug APK using the terminal:

```bash
./gradlew assembleDebug
```

---

## 🔌 IoT Hardware Setup

Mastake supports external note synchronization through an ESP32 microcontroller.

**Required Hardware:**
- ESP32 development board.
- ILI9341 TFT display.
- Compatible wiring and power supply.

**Communication Protocol:**
- Bluetooth Low Energy (BLE).
- Device Name: `Mastaka_IoT`
- BLE Service UUID: `4fafc201-1fb5-459e-8fcc-c5c9c331914b`
- BLE Characteristic UUID: `beb5483e-36e1-4688-b7f5-ea07361b26a8`

The Android application sends note data to the ESP32, which stores the received content and displays it on the connected TFT screen.

---

## 📂 Project Overview

The application combines multiple Android technologies into a single platform:

- **Android Application:** User interface, authentication, note management, and synchronization.
- **Room Database:** Local data persistence.
- **Firebase:** Authentication and cloud data storage.
- **Gemini AI:** Intelligent note organization and formatting.
- **ESP32:** External IoT note display.
- **Bluetooth LE:** Wireless communication between the Android application and ESP32.

---

## 🎯 Project Objectives

- Develop a modern Android-based personal note management system.
- Implement offline-first local data storage.
- Integrate cloud-based authentication and synchronization.
- Explore practical AI integration in mobile applications.
- Establish wireless communication between Android and IoT hardware.
- Demonstrate Android sensors, widgets, and implicit intents through practical implementations.

---

## 📄 Academic Information

**Course:** Mobile Application and Development Lab  
**Project:** Mastake – Personal Notes & IoT Sync App  
**Developer:** Md Siamul Islam Soaib  
**Roll:** 04  
**Batch:** L d85

---

<p align="center">
  <b>Built with ❤️ using Android, Firebase, Gemini AI & ESP32</b>
</p>
