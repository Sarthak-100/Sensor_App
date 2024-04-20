package com.example.sensor_app

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.util.Log
import androidx.compose.ui.Modifier
import android.widget.Toast
import java.io.File
import java.io.FileWriter
import com.opencsv.CSVWriter
import kotlinx.coroutines.*
//import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
//import android.graphics.Color
import com.example.sensor_app.ui.theme.Sensor_AppTheme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.*
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.data.LineDataSet
//import com.example.sensor_app.ui.theme.Sensor_AppTheme

@Entity(tableName = "accelerometer_data")
data class AccelerometerData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
)

@Dao
interface AccelerometerDao {
    @Insert
    suspend fun insert(data: AccelerometerData)

    @Query("SELECT * FROM accelerometer_data ORDER BY timestamp DESC")
    fun getAllData(): List<AccelerometerData>

    @Query("DELETE FROM accelerometer_data")
    suspend fun deleteAllData()
}

@Database(entities = [AccelerometerData::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accelerometerDao(): AccelerometerDao

    suspend fun clearAllData() {
        accelerometerDao().deleteAllData()
    }
}

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var accelerometerValues by mutableStateOf(floatArrayOf(0f, 0f, 0f))
    private lateinit var db: AppDatabase
    private var buttonClicked by mutableStateOf(false)
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private fun exportDatabaseToCsv(db: AppDatabase) {
        coroutineScope.launch(Dispatchers.IO) {
            val file = File(getExternalFilesDir(null), "database_export.csv")
            val writer = CSVWriter(FileWriter(file))

            // Write CSV header
            writer.writeNext(arrayOf("ID", "X", "Y", "Z", "Timestamp"))

            // Query the database to get all data
            val data = db.accelerometerDao().getAllData()

            // Write each row to the CSV file
            data.forEach { entry ->
                val row = arrayOf(
                    entry.id.toString(),
                    entry.x.toString(),
                    entry.y.toString(),
                    entry.z.toString(),
                    entry.timestamp.toString()
                )
                writer.writeNext(row)
            }

            // Close the CSV writer
            writer.close()

            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Database exported to $file", Toast.LENGTH_SHORT).show()
            }
            Log.d("Sensor App", "Database exported to $file")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "sensor_database"
        ).build()

        // Log the database entries
        coroutineScope.launch {
            val entries = withContext(Dispatchers.IO) {
                db.accelerometerDao().getAllData()
            }
            for (entry in entries) {
                Log.d("Sensor App", "ID: ${entry.id}, X: ${entry.x}, Y: ${entry.y}, Z: ${entry.z}, Timestamp: ${entry.timestamp}")
            }
        }
        // Clear the database
        coroutineScope.launch {
            db.clearAllData()
        }
        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "main") {
                composable("main") {
                    Sensor_AppTheme {
                        // A surface container using the 'background' color from the theme
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            SensorDataDisplay(
                                accelerometerValues,
                                onExportClick = { exportDatabaseToCsv(db) },
                                onButtonClick = { buttonClicked })
//                            navController.navigate("graph")
                        }
                    }
                }
                composable("graph") {
                    GraphScreen(db)
                    if (buttonClicked) {
                        GraphScreen(db)
                    } else {
                        // Placeholder composable, can be empty or display a message
                        Text("Click the button to go to the graph screen")
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { accel ->
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        coroutineScope.cancel()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Can be used to react to changes in sensor accuracy.
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            accelerometerValues = event.values.clone()
            val data = AccelerometerData(
                x = accelerometerValues[0],
                y = accelerometerValues[1],
                z = accelerometerValues[2],
                timestamp = System.currentTimeMillis()
            )
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    db.accelerometerDao().insert(data)
                }
            }
        }
    }
}

@Composable
fun SensorDataDisplay(values: FloatArray, onExportClick: () -> Unit,onButtonClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "X: ${values[0]}")
        Text(text = "Y: ${values[1]}")
        Text(text = "Z: ${values[2]}")
        Button(onClick = onExportClick) {
            Text("Export Data")
        }
        Button(onClick = onButtonClick) {
            Text("Go to Graph")
        }
    }
}

@Composable
fun GraphScreen(db: AppDatabase) {
    val xEntries = remember { mutableStateOf(mutableListOf<Entry>()) }
    val yEntries = remember { mutableStateOf(mutableListOf<Entry>()) }
    val zEntries = remember { mutableStateOf(mutableListOf<Entry>()) }

    val coroutineScope = rememberCoroutineScope()

    // Fetch data from the database when the composable is first launched
    LaunchedEffect(db) {
        coroutineScope.launch {
            val data = withContext(Dispatchers.IO) {
                db.accelerometerDao().getAllData()
            }
            xEntries.value.clear()
            yEntries.value.clear()
            zEntries.value.clear()
            data.forEachIndexed { index, entry ->
                xEntries.value.add(Entry(index.toFloat(), entry.x))
                yEntries.value.add(Entry(index.toFloat(), entry.y))
                zEntries.value.add(Entry(index.toFloat(), entry.z))
            }
        }
    }

    Sensor_AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            Chart(LineData(LineDataSet(xEntries.value, "X")), "X vs Time")
            Chart(LineData(LineDataSet(yEntries.value, "Y")), "Y vs Time")
            Chart(LineData(LineDataSet(zEntries.value, "Z")), "Z vs Time")
        }
    }

}


@Composable
fun Chart(data: LineData, title: String) {
    AndroidView(factory = { context ->
        LineChart(context).apply {
            this.data = data
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisRight.isEnabled = false
            legend.isEnabled = true
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            invalidate()
        }
    })
}


