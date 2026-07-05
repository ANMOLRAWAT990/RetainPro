# RetainPro

<div align="center">
  <img width="800" alt="RetainPro Banner"/>
</div>

## Overview
**RetainPro** is a comprehensive Android application built using Kotlin and Jetpack Compose. It serves as a specialized tool for structural engineers, architects, and construction professionals to design, analyze, visualize, and estimate costs for retaining walls. The app provides a seamless and interactive interface with various dedicated modules, streamlining the process of structural calculation and project estimation.

## Features
- **Design Module**: Input structural parameters, soil properties, and load values to configure the retaining wall design.
- **Analysis Engine**: Advanced structural calculations to ensure stability against overturning, sliding, and bearing capacity failure (powered by a dedicated `StructuralCalculator`).
- **Drawing Screen**: Automatically generates a visual representation of the retaining wall based on the input parameters.
- **Estimate Tool**: Built-in functionality for cost estimation based on required materials and custom labor/material rates.
- **Saved Logs**: Save your designs and estimations for future reference, allowing you to load or delete past projects.
- **Walkthrough**: Integrated guide to help new users navigate the app effectively.

## Tech Stack
- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Navigation**: Compose Navigation
- **Local Database/Storage**: Room (for saved logs and rate configurations)
- **Asynchronous Programming**: Kotlin Coroutines & Flow

## Prerequisites
- [Android Studio](https://developer.android.com/studio) (latest version recommended)
- Android SDK (Min SDK: 26, Target SDK: 36)
- Java 11

## Getting Started

1. **Clone the Repository**
   ```bash
   git clone https://github.com/ANMOLRAWAT990/RetainPro.git
   ```
2. **Open the Project**
   Launch Android Studio, select **Open**, and navigate to the cloned `RetainPro` directory.
3. **Sync Gradle**
   Allow Android Studio to sync dependencies and fix any incompatibilities.
4. **Environment Variables (Optional)**
   If the project requires API integrations, create a `.env` file in the root directory (see `.env.example`).
5. **Run the App**
   Select an emulator or a physical device and click the **Run** button in Android Studio.

## Architecture
RetainPro follows a robust architecture adhering to modern Android development practices:
- **`ui/`**: Contains all Jetpack Compose screens (`DesignScreen`, `AnalysisScreen`, etc.) and theming.
- **`viewmodel/`**: Houses the `WallViewModel` which manages the UI state, user inputs, and coordinates between data and calculation modules.
- **`engineering/`**: Contains the core logic (`StructuralCalculator`) for structural engineering calculations.
- **`model/`**: Data classes representing wall parameters, analysis results, and logs.
- **`data/`**: Repositories and local database implementations for saving and retrieving logs.
- **`export/`**: Utilities for exporting estimates or drawing data.

## Contributing
Contributions are welcome! If you'd like to improve RetainPro, feel free to fork the repository, create a feature branch, and submit a pull request.

## License
This project is licensed under the MIT License.
