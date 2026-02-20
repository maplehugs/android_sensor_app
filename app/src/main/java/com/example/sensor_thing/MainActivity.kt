package com.example.sensor_thing

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor_thing.ui.theme.Sensor_thingTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var ambientTempSensor: Sensor? = null

    private var lightLux by mutableFloatStateOf(0f)
    private var ambientTempC by mutableStateOf<Float?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        ambientTempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

        enableEdgeToEdge()
        setContent {
            Sensor_thingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SensorScreen(
                        lightLux = lightLux,
                        ambientTempC = ambientTempC,
                        hasLightSensor = lightSensor != null,
                        hasTempSensor = ambientTempSensor != null,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        ambientTempSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LIGHT -> lightLux = event.values.firstOrNull() ?: lightLux
            Sensor.TYPE_AMBIENT_TEMPERATURE -> ambientTempC = event.values.firstOrNull()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

@Composable
fun SensorScreen(
    lightLux: Float,
    ambientTempC: Float?,
    hasLightSensor: Boolean,
    hasTempSensor: Boolean,
    modifier: Modifier = Modifier,
) {
    val baseColor = colorFromLight(lightLux)
    val temperatureColor = ambientTempC?.let { colorFromTemperature(it) }
    val background = if (temperatureColor != null) lerp(baseColor, temperatureColor, 0.45f) else baseColor

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Sensor Thing",
            fontSize = 28.sp,
            color = Color.White,
        )

        SensorCard(
            title = "Luz",
            value = if (hasLightSensor) "${lightLux.roundToInt()} lux" else "No disponible",
            subtitle = "El color base cambia según la luz ambiente.",
        )

        SensorCard(
            title = "Temperatura ambiente",
            value = if (hasTempSensor) {
                ambientTempC?.let { "${it.roundToInt()} °C" } ?: "Midiendo..."
            } else {
                "No disponible en este dispositivo"
            },
            subtitle = "Si existe sensor, también influye en el color.",
        )

        ColorBar(background)
    }
}

@Composable
private fun SensorCard(title: String, value: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = title, fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
            Text(text = value, fontSize = 22.sp, color = Color.White)
            Text(text = subtitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun ColorBar(currentColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(currentColor),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "Color actual", color = Color.White, fontSize = 16.sp)
        }
    }
}

private fun colorFromLight(lux: Float): Color {
    val clamped = lux.coerceIn(0f, 20_000f) / 20_000f
    val dark = Color(0xFF12182D)
    val warm = Color(0xFFE0A125)
    val bright = Color(0xFFFFF4C2)
    return if (clamped < 0.45f) {
        lerp(dark, warm, clamped / 0.45f)
    } else {
        lerp(warm, bright, (clamped - 0.45f) / 0.55f)
    }
}

private fun colorFromTemperature(tempC: Float): Color {
    val normalized = ((tempC - 0f) / 45f).coerceIn(0f, 1f)
    val cold = Color(0xFF2C7BE5)
    val hot = Color(0xFFE55353)
    return lerp(cold, hot, normalized)
}

@Preview(showBackground = true)
@Composable
fun SensorPreview() {
    Sensor_thingTheme {
        SensorScreen(
            lightLux = 3500f,
            ambientTempC = 24f,
            hasLightSensor = true,
            hasTempSensor = true,
        )
    }
}
