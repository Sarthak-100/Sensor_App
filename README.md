# Sensor App
<p align="center">
  <img src="https://github.com/Sarthak-100/Sensor_App/assets/82911845/5383e0b9-549c-4d67-ab2d-e3b07c1052a3" width="280" height="500">
  <img src="https://github.com/Sarthak-100/Sensor_App/assets/82911845/4d4d49c1-9620-4e12-af02-7527e9d9bdfb" width="280" height="500">
  <img src="https://github.com/Sarthak-100/Sensor_App/assets/82911845/a389042c-7bf2-4977-83d3-75273d904727" width="750" height="300">
</p>

This is an Android application built with Jetpack Compose that demonstrates how to use the device's accelerometer sensor to collect and display sensor data, store the data in a local Room database, and visualize the data using the MPAndroidChart library.

## Features

- Collects accelerometer data (x, y, z values) at a specified rate
- Displays the current accelerometer values on the main screen
- Stores the accelerometer data in a local Room database
- Exports the database data to a CSV file
- Displays line charts for the x, y, and z accelerometer values over time

## Prerequisites

- Android Studio
- Android device or emulator

## Getting Started

1. Clone the repository: `git clone https://github.com/Sarthak-100/Sensor_App.git`
2. Open the project in Android Studio.

3. Build and run the app on your Android device or emulator.

## Usage

1. When the app starts, it will display the current accelerometer values on the main screen.

2. To export the database data to a CSV file, click the "Export Data" button. The exported file will be saved in the app's external files directory with the name "database_export.csv".

3. To view the line charts of the accelerometer data over time, click the "Go to Graph" button. This will navigate to a new screen displaying three charts: one for the x-axis, one for the y-axis, and one for the z-axis.

4. You can zoom in and out of the charts by pinching the screen.

## Code Structure

- `MainActivity.kt`: The main activity of the app, responsible for setting up the Jetpack Compose UI, handling sensor events, and managing the Room database.
- `AccelerometerData.kt`: The data class representing an accelerometer data entry, annotated with `@Entity` for Room database integration.
- `AccelerometerDao.kt`: The Data Access Object interface for interacting with the Room database.
- `AppDatabase.kt`: The Room database class, providing access to the `AccelerometerDao`.
- `SensorDataDisplay.kt`: A Composable function that displays the current accelerometer values and provides buttons for exporting data and navigating to the graph screen.
- `GraphScreen.kt`: A Composable function that displays line charts for the x, y, and z accelerometer values over time, using the MPAndroidChart library.

## Dependencies

This project uses the following dependencies:

- Jetpack Compose
- Room
- MPAndroidChart
- OpenCSV

## Contributing

Contributions are welcome! If you find any issues or have suggestions for improvements, please open an issue or submit a pull request.

## License

This project is licensed under the [MIT License](LICENSE).
