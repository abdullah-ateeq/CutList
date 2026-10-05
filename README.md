# CutList 🪵📏

**CutList** is a modern, feature-rich Android application built for woodworkers, carpenters, fabricators, and DIY enthusiasts. It simplifies planning, organizing, and calculating cut lists for wood, sheet goods, panels, and other raw materials with precision and ease.

---

## 🌟 Key Features

- **Precise Calculations**: Uses exact `BigDecimal` arithmetic internally to prevent floating-point rounding errors during area and length computations.
- **Flexible Unit Systems**: Supports both Metric (millimeters, centimeters, meters) and Imperial (fractional inches, decimal inches) with intuitive parsing and formatting.
- **Smart Grouping**: Automatically groups cut pieces by material type and thickness, helping you optimize material usage and stock planning.
- **Consolidation & Sorting**:
  - Automatically or manually combine identical cut pieces while preserving distinct notes and names.
  - Sort list items by Name, Length, Width, Quantity, or original entry order.
- **Detailed Piece Attributes**: Track dimensions, thickness, material, quantity, notes, grain direction, and rotation constraints.
- **Project History & Export**: Save projects, review past cut lists in History, and export/import projects easily using robust JSON codecs.
- **Modern UI / Material 3**: Built natively with Jetpack Compose and Material 3, featuring smooth navigation, dark/light theme support, and a clean, accessible interface.

---

## 📱 Screenshots & UI

<p align="center">
  <br>
  <em>Built with Jetpack Compose & Material 3</em>
</p>

---

## 🛠️ Tech Stack & Architecture

- **Kotlin**: 100% Kotlin codebase leveraging coroutines and modern language features.
- **Jetpack Compose**: Declarative UI toolkit adhering to Material 3 design guidelines.
- **Architecture**: MVVM pattern with `ViewModel`, `StateFlow`, and Repository abstraction (`ProjectRepository`, `SettingsRepository`).
- **Data & Serialization**: `kotlinx.serialization` for reliable project import/export and Jetpack DataStore for user preferences.
- **Testing**: Comprehensive unit test suite covering core math (`CutListCalculator`), validators (`PieceValidator`), formatters (`MeasurementFormatter`), and repositories.

---

## 📂 Project Structure

```tree
com.example.cutlist/
├── data/          # Repositories, AppSettings, ProjectCodec, and local data handling
├── domain/        # CutListCalculator, PieceValidator, Unit conversion & formatting models
├── ui/            # Jetpack Compose UI screens (CutList, History, Settings, About, Splash)
│   ├── components/# Reusable UI components & dialogs
│   ├── cutlist/   # Main cut list editor, piece sheet, and summary views
│   ├── history/   # Saved project history screen
│   ├── settings/  # App preferences & unit configuration
│   └── theme/     # Material 3 colors, typography, and theme setup
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Koala / Jellyfish / Ladybug or newer recommended)
- **JDK 11** or higher
- **Android SDK** (Compile SDK 37, Min SDK 24+)

### Installation & Building
1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/CutList.git
   ```
2. Open the project folder in Android Studio.
3. Sync project with Gradle files.
4. Run the app on an Android emulator or connected physical device.

---

## 🧪 Running Tests

To run the unit test suite:
```bash
./gradlew test
```

To run instrumented UI tests:
```bash
./gradlew connectedAndroidTest
```

---

## 📄 License

This project is available under the [MIT License](LICENSE).
