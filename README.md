```markdown
# MultiModuleProject

A modern Android multi-module project showcasing best practices for scalable and maintainable Android application architecture using Kotlin, with Server-Driven UI (SDUI) implementation.

## Overview

MultiModuleProject is a production-ready Android application built with a modular architecture. It demonstrates how to organize large Android projects into independent, reusable modules while maintaining clean separation of concerns and facilitating team collaboration. The project implements Server-Driven UI (SDUI) patterns to enable dynamic UI configuration and component rendering from remote configurations.

**Created:** February 2, 2025  
**Language:** Kotlin  
**Platform:** Android  

## Project Structure

The project is organized into the following module layers:

### Core Modules
These are foundational modules providing shared functionality and services:

- **`:core:navigation`** - Navigation and routing between screens
- **`:core:network`** - Network requests and API interactions
- **`:core:authentication`** - User authentication and session management
- **`:core:common`** - Shared utilities and base classes
  - **`:core:common:uicomponents`** - Reusable UI components
- **`:core:data`** - Data layer, repository patterns, and SDUI data mapping
- **`:core:forms`** - Form handling utilities and validation with server-driven configuration
- **`:core:babycare`** - Baby care domain core logic
- **`:core:renderer`** - SDUI component rendering engine and utilities

### Feature Modules
Feature-specific implementations that utilize core modules:

- **`:feature:home`** - Home screen and main navigation with developer menu for SDUI config sync
- **`:feature:login`** - User login and authentication UI
- **`:feature:splashscreen`** - Splash screen on app startup
- **`:feature:coffee`** - Coffee-related features with SDUI components
- **`:feature:babycare`** - Baby care feature UI with SDUI-driven screens
- **`:feature:forms`** - Form-based features with server-driven form configurations
- **`:feature:priceinput`** - Price input functionality

### App Module
- **`:app`** - Main application module that integrates all features

### Build Logic
- **`:build-logic`** - Gradle convention plugins and shared build configuration
  - **`:build-logic:convention`** - Convention plugins (e.g., `mmp.android.application`, `mmp.android.compose`)

### Testing
- **`:baselineprofile`** - Baseline profiles for performance benchmarking and optimization

## Key Technologies & Dependencies

### Build System & Configuration
- **Kotlin DSL** - Type-safe Gradle build scripts
- **Gradle Version Catalog** - Centralized dependency management via `gradle/libs.versions.toml`
- **Build Logic Plugins** - Convention-based plugin system for consistent module configuration

### Android & UI
- **Android SDK 31-37** - Compatible with modern Android versions
- **Jetpack Compose** - Modern declarative UI framework
- **Baseline Profiles** - Performance optimization and faster app startup

### Server-Driven UI (SDUI)
- **Dynamic Component Rendering** - Flexible UI configuration from remote sources
- **SDUI ViewModel** - `GenericSduiViewModel` for managing screen state and dynamic component loading
- **SDUI Screen Framework** - `GenericSduiScreen` composable for rendering server-driven layouts
- **ScreenRepository** - Manages fetching and caching of screen configurations
- **RenderUI Engine** - Dynamic component renderer supporting multiple UI element types
- **Pull-to-Refresh** - Support for refreshing SDUI configurations with user interaction
- **Developer Menu** - Sync SDUI configurations in debug builds for testing and development

### Dependency Injection & Serialization
- **Dagger Hilt** - Dependency injection framework (v2.60.1)
- **Kotlin Serialization** - Type-safe serialization support
- **Kotlin Parcelize** - Serializable object support

### Backend & Authentication
- **Firebase** - Backend services including:
  - Firestore (database for SDUI configurations and app data)
  - Firebase Authentication
  - Google Play Services

### App Functions
- **AndroidX App Functions** - Modern app function capabilities with KSP compilation

### Testing
- **MockK** - Mocking framework for unit tests
- **Kotlinx Coroutines Test** - Coroutine testing utilities
- **Turbine** - Flow testing utilities
- **Android JUnit & UI Test Automator** - UI testing frameworks

## Server-Driven UI (SDUI) Architecture

### Overview
The MultiModuleProject implements a comprehensive SDUI system that enables the app to dynamically render UI components based on server-provided configurations. This approach allows for:

- **Remote UI Updates** - Change app UI without requiring app updates
- **A/B Testing** - Serve different UI configurations to different user segments
- **Feature Flags** - Enable/disable features dynamically
- **Rapid Iteration** - Quick UI changes and experiments

### SDUI Components

The project supports multiple SDUI component types including:

- **Tiles** - Interactive tile components with navigation destinations
- **Charts** - Data visualization components (growth charts, feeding frequency, feeding gaps)
- **History Components** - Chronological data display for feeding, temperature, measurements
- **Text & Title** - Basic text rendering components
- **Custom Domain Components** - Specialized components like temperature charts and baby care insights

### SDUI Data Flow

```
ScreenRepository (fetches configs) 
    ↓
GenericSduiViewModel (manages state & caching)
    ↓
ScreenDataMapper (maps DTOs to domain models)
    ↓
GenericSduiScreen (renders components)
    ↓
RenderUI Engine (renders individual components)
```

### Usage Example

```kotlin
GenericSduiScreen(
    screenId = "temperature_screen",
    title = "Temperature",
    onNavigateBack = { navController.popBackStack() },
    onAddNew = { navigateToForm("temperatureLog", null) },
    lazyFeatureContent = { item ->
        when (item) {
            is NetworkScreenData.TemperatureHistoryDataNetwork -> {
                item { TemperatureHistoryComponent(...) }
                true
            }
            is NetworkScreenData.TemperatureChartDataNetwork -> {
                item { TemperatureChartComponent(...) }
                true
            }
            else -> false
        }
    }
)
```

### Developer Features

In debug builds, a developer menu in the Settings screen enables:
- **Sync SDUI Configurations** - Manually synchronize form and screen configurations from the server
- **Test Dynamic Rendering** - Preview SDUI changes without app rebuild
- **Configuration Management** - Manage local SDUI cache and state

## Getting Started

### Prerequisites
- Android Studio (latest version recommended)
- JDK 17 or higher
- Gradle (included via Gradle Wrapper)

### Building the Project

```bash
# Clone the repository
git clone https://github.com/bobob306/MultiModuleProject.git
cd MultiModuleProject

# Build the project
./gradlew build

# Build and run on a connected device or emulator
./gradlew installDebug
```

### Running Tests

```bash
# Run all unit tests
./gradlew test

# Run baseline profile generation
./gradlew :baselineprofile:connectedCheck
```

## Architecture Highlights

### Modular Design
- **Feature isolation**: Each feature module is independent and can be developed/tested in isolation
- **Core layer dependency**: Feature modules depend on core modules, but not on each other
- **Build logic separation**: Convention plugins ensure consistent configuration across modules

### Dependency Injection
- Hilt is configured at the app level and propagates through the module hierarchy
- Custom KSP configurations for app functions aggregation

### Navigation
- Centralized navigation logic in `:core:navigation` module
- Type-safe navigation between feature modules
- Deep linking support for SDUI-driven screens

### SDUI & Dynamic Rendering
- Server-driven UI components decouple UI structure from code
- Flexible component mapping allows feature-specific rendering
- Efficient caching and state management via SDUI ViewModels

### Performance Optimization
- Baseline profiles for faster app startup and improved performance
- ProGuard configuration for release builds with minification and resource shrinking
- Gradle configuration caching for faster builds
- Pull-to-refresh pattern for efficient SDUI configuration updates

## Version Configuration

Version information can be configured via:
- **Gradle Properties**: `-PversionCode=78 -PversionName="0.0.78"`
- **Default Values**: Falls back to versionCode 77 and versionName "0.0.77"

Example:
```bash
./gradlew build -PversionCode=78 -PversionName="0.0.78"
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Follow the modular architecture patterns established in the project
4. Write tests for new functionality
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## Project Statistics

- **Size**: ~41 MB
- **Modules**: 16+
- **Open Issues**: 0
- **Last Updated**: September 8, 2026

## License

This project does not currently have a license specified. See the repository for more details.

## Repository

- **Owner**: [@bobob306](https://github.com/bobob306)
- **URL**: [github.com/bobob306/MultiModuleProject](https://github.com/bobob306/MultiModuleProject)
- **Visibility**: Public

---

For issues, questions, or contributions, please visit the [repository's Issues page](https://github.com/bobob306/MultiModuleProject/issues).
```

This is the complete README with all sections included:
- **Overview** introducing the project and SDUI approach
- **Project Structure** detailing all 16+ modules organized by layer
- **Key Technologies** covering build system, Android/UI, SDUI, DI, backend, and testing
- **Server-Driven UI Architecture** section with comprehensive SDUI documentation including overview, components, data flow, usage example, and developer features
- **Getting Started** with prerequisites and build instructions
- **Architecture Highlights** explaining modular design, DI, navigation, SDUI, and performance
- **Version Configuration** and **Contributing** guidelines
- **Project Statistics** and **License** information
