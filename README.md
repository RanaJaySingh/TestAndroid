# PiPlanner - Every Rupee Has a Plan

An Indian savings-goal planner Android app with AI assistant powered by Grok.

## Overview

PiPlanner helps users manage their savings goals by:
- Picking one dedicated savings account
- Splitting every new credit across goals by percentage
- Locking saved amounts to prevent overspending
- Using AI (Grok) for goal parsing and assistance

## Features

### J1: First-time Setup
- Welcome screen with "How it works" explanation
- Account selection with dedicated savings toggle
- Balance consent (auto vs manual)
- UPI PIN demo pad for balance fetch
- Goal creation via AI chat or manual form
- Inflation adjustment (default 7%)
- Opening balance split with lock

### Main App
- **Goals Tab**: Balance card, quick actions (Sync, New goal, Transfer, History), goal cards with progress
- **History Tab**: Chronological list of all transactions (credits, transfers, withdrawals)
- **Ask Tab**: AI-powered assistant for goal management queries

### Additional Features
- Transfer between goals
- Record withdrawals
- Settings with account management and demo reset
- Indian INR formatting (₹1,00,000)

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Navigation**: Navigation Compose
- **Persistence**: Room Database + DataStore Preferences
- **Architecture**: Clean Architecture (UI / Domain / Data layers)

## Demo Profile

- **User**: Rahul
- **Savings**: HDFC ••4821 - ₹1,00,000 (dedicated savings)
- **Spending**: SBI ••7730 - ₹72,000

## Project Structure

```
app/src/main/java/com/piplanner/android/
├── data/
│   ├── local/
│   │   ├── dao/           # Room DAOs
│   │   ├── database/      # Room Database
│   │   └── entity/        # Room Entities
│   ├── repository/        # Data repositories
│   ├── GrokStubService.kt # AI stub implementation
│   └── PreferencesDataStore.kt
├── domain/
│   └── model/             # Domain models
├── ui/
│   ├── components/        # Reusable UI components
│   ├── navigation/        # Navigation setup
│   ├── screens/           # Screen composables
│   │   ├── ask/
│   │   ├── balance/
│   │   ├── consent/
│   │   ├── goals/
│   │   ├── history/
│   │   ├── settings/
│   │   ├── setup/
│   │   ├── transfer/
│   │   └── withdrawal/
│   └── theme/             # Colors, Typography, Theme
├── util/                  # Utility classes
├── MainActivity.kt
└── PiPlannerApplication.kt
```

## How to Run

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd PiPlanner
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory and select it

3. **Sync Gradle**
   - Android Studio should automatically sync Gradle
   - If not, click "Sync Project with Gradle Files" in the toolbar

4. **Run the app**
   - Select a device/emulator from the device dropdown
   - Click the Run button (green play icon)
   - Or use `./gradlew installDebug` from terminal

### Build from Command Line

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

## Design System

### Colors
- **Primary Navy**: #002E6E
- **Background**: #F4F6FA
- **Surface**: #FFFFFF
- **Status Green**: #22C55E (on track)
- **Status Amber**: #F59E0B (behind/warning)
- **Status Red**: #EF4444 (error)

### Components
- White rounded cards with subtle borders
- iOS-style toggles
- Large ₹ amounts with Indian number formatting
- Step indicators (Step 1 of 3)
- "Grok's proposal" highlighted cards

## API Stubs

The Grok AI integration is stubbed with deterministic responses:
- Goal parsing from natural language
- Transfer suggestions
- Progress status queries

The stub can be replaced with real API integration by modifying `GrokStubService.kt`.

## License

This is a demo application for educational purposes.
