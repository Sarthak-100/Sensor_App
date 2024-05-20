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
import androidx.compose.foundation.layout.*
import android.widget.Toast
import java.io.File
import java.io.FileWriter
import com.opencsv.CSVWriter
import kotlinx.coroutines.*
import android.view.ViewGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
//import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
//import android.graphics.Color
import com.example.sensor_app.ui.theme.Sensor_AppTheme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.*
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
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
//        // Clear the database
//        coroutineScope.launch {
//            db.clearAllData()
//        }
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
                                navController = navController
                            )
                        }
                    }
                }
                composable("graph") {
                    GraphScreen(db)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { accel ->
            sensorManager.registerListener(this, accel, 100000)
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
fun SensorDataDisplay(
    values: FloatArray,
    onExportClick: () -> Unit,
    navController: NavController
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier
                .width(300.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Accelerometer Data",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = Modifier
                        .   align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "X: ${String.format("%.2f", values[0])}",
                    )
                    Text(
                        text = "Y: ${String.format("%.2f", values[1])}",
                    )
                    Text(
                        text = "Z: ${String.format("%.2f", values[2])}",
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onExportClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Data")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { navController.navigate("graph") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to Graph")
                }
            }
        }
    }
}


@Composable
fun GraphScreen(db: AppDatabase) {
    val coroutineScope = rememberCoroutineScope()
    var dataX by remember { mutableStateOf<LineData?>(null) }
    var dataY by remember { mutableStateOf<LineData?>(null) }
    var dataZ by remember { mutableStateOf<LineData?>(null) }

    LaunchedEffect(key1 = true) {
        coroutineScope.launch {
            val data = withContext(Dispatchers.IO) {
                db.accelerometerDao().getAllData()
            }

            val entriesX = mutableListOf<Entry>()
            val entriesY = mutableListOf<Entry>()
            val entriesZ = mutableListOf<Entry>()

            data.forEachIndexed { index, accelerometerData ->
                entriesX.add(Entry(accelerometerData.timestamp.toFloat(), accelerometerData.x))
                entriesY.add(Entry(accelerometerData.timestamp.toFloat(), accelerometerData.y))
                entriesZ.add(Entry(accelerometerData.timestamp.toFloat(), accelerometerData.z))
            }

            val dataSetX = LineDataSet(entriesX, "X Axis").apply { axisDependency = YAxis.AxisDependency.LEFT }
            val dataSetY = LineDataSet(entriesY, "Y Axis").apply { axisDependency = YAxis.AxisDependency.LEFT }
            val dataSetZ = LineDataSet(entriesZ, "Z Axis").apply { axisDependency = YAxis.AxisDependency.LEFT }

            dataX = LineData(dataSetX)
            dataY = LineData(dataSetY)
            dataZ = LineData(dataSetZ)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        GraphView(dataX, "Graph X vs Time", Modifier.weight(1f))
        Spacer(modifier = Modifier.height(16.dp))  // Space between charts
        GraphView(dataY, "Graph Y vs Time", Modifier.weight(1f))
        Spacer(modifier = Modifier.height(16.dp))  // Space between charts
        GraphView(dataZ, "Graph Z vs Time", Modifier.weight(1f))
    }
}

@Composable
fun GraphView(lineData: LineData?, title: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(200.dp)) {
        lineData?.let { data ->
            AndroidView(factory = { context ->
                LineChart(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.data = data
                    description.text = title
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    axisLeft.setDrawGridLines(false)
                    xAxis.setDrawGridLines(false)
                    xAxis.setDrawAxisLine(true)
                    legend.form = Legend.LegendForm.LINE
                    setTouchEnabled(true)
                    setPinchZoom(true)
                    invalidate()
                }
            })
        }
    }
}

