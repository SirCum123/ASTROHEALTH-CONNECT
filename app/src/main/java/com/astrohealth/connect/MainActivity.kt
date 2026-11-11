package com.astrohealth.connect

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var healthClient: HealthConnectClient? = null

    private val requiredPermissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    private val permissionLauncher =
        registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { granted ->
            if (granted.containsAll(requiredPermissions)) {
                readHealthData()
            } else {
                sendMessage(
                    JSONObject()
                        .put("ok", false)
                        .put("message", "No se concedieron todos los permisos. Puedes habilitarlos en Health Connect.")
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(HealthBridge(), "AstroHealthAndroid")
        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")

        val status = HealthConnectClient.getSdkStatus(this)
        if (status == HealthConnectClient.SDK_AVAILABLE) {
            healthClient = HealthConnectClient.getOrCreate(this)
        } else {
            webView.post {
                sendMessage(
                    JSONObject()
                        .put("ok", false)
                        .put("message", "Health Connect no está disponible. Actualiza o instala Health Connect según la versión de Android.")
                )
            }
        }
    }

    inner class HealthBridge {
        @JavascriptInterface
        fun requestHealthData() {
            runOnUiThread {
                lifecycleScope.launch {
                    val client = healthClient
                    if (client == null) {
                        sendMessage(
                            JSONObject().put("ok", false)
                                .put("message", "No se pudo iniciar Health Connect en este teléfono.")
                        )
                        return@launch
                    }

                    try {
                        val granted = client.permissionController.getGrantedPermissions()
                        if (granted.containsAll(requiredPermissions)) {
                            readHealthData()
                        } else {
                            permissionLauncher.launch(requiredPermissions)
                        }
                    } catch (e: Exception) {
                        sendMessage(
                            JSONObject().put("ok", false)
                                .put("message", "Error comprobando permisos: ${e.localizedMessage}")
                        )
                    }
                }
            }
        }
    }

    private fun readHealthData() {
        lifecycleScope.launch {
            try {
                val client = healthClient ?: return@launch
                val end = Instant.now()
                val start = end.minus(24, ChronoUnit.HOURS)
                val range = TimeRangeFilter.between(start, end)

                // Frecuencia cardíaca: tomar la muestra más reciente de las últimas 24 h.
                val heartRecords = client.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = range,
                        pageSize = 1000
                    )
                ).records

                val latestHeart = heartRecords
                    .flatMap { it.samples }
                    .maxByOrNull { it.time }

                // SpO2: Health Connect solo tendrá datos si una app/reloj compatible los escribió.
                val oxygenRecords = client.readRecords(
                    ReadRecordsRequest(
                        recordType = OxygenSaturationRecord::class,
                        timeRangeFilter = range,
                        pageSize = 1000
                    )
                ).records

                val latestOxygen = oxygenRecords.maxByOrNull { it.time }

                // Pasos: usar aggregate para evitar duplicar datos de varias fuentes.
                val stepsResult = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = range
                    )
                )
                val totalSteps = stepsResult[StepsRecord.COUNT_TOTAL]

                val bpRecords = client.readRecords(
                    ReadRecordsRequest(
                        recordType = BloodPressureRecord::class,
                        timeRangeFilter = range,
                        pageSize = 1000
                    )
                ).records
                val latestBp = bpRecords.maxByOrNull { it.time }

                val tempRecords = client.readRecords(
                    ReadRecordsRequest(
                        recordType = BodyTemperatureRecord::class,
                        timeRangeFilter = range,
                        pageSize = 1000
                    )
                ).records
                val latestTemp = tempRecords.maxByOrNull { it.time }

                val sleepRecords = client.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = range,
                        pageSize = 1000
                    )
                ).records
                val latestSleep = sleepRecords.maxByOrNull { it.endTime }

                val result = JSONObject()
                    .put("ok", true)
                    .put("source", "HEALTH_CONNECT")
                    .put("window", "Últimas 24 horas")
                    .put("retrievedAt", Instant.now().toString())
                    .put("heartRate", latestHeart?.beatsPerMinute)
                    .put("heartRateTime", latestHeart?.time?.toString())
                    .put("spo2", latestOxygen?.percentage?.value)
                    .put("spo2Time", latestOxygen?.time?.toString())
                    .put("steps24h", totalSteps)
                    .put("systolic", latestBp?.systolic?.inMillimetersOfMercury)
                    .put("diastolic", latestBp?.diastolic?.inMillimetersOfMercury)
                    .put("bloodPressureTime", latestBp?.time?.toString())
                    .put("bodyTempC", latestTemp?.temperature?.inCelsius)
                    .put("bodyTempTime", latestTemp?.time?.toString())
                    .put("sleepStart", latestSleep?.startTime?.toString())
                    .put("sleepEnd", latestSleep?.endTime?.toString())
                    .put("note", "Registros sincronizados con Health Connect; no necesariamente son mediciones en vivo. Los campos vacíos no están disponibles.")

                sendMessage(result)
            } catch (e: SecurityException) {
                sendMessage(
                    JSONObject().put("ok", false)
                        .put("message", "Permiso denegado o retirado. Vuelve a autorizar el acceso en Health Connect.")
                )
            } catch (e: Exception) {
                sendMessage(
                    JSONObject().put("ok", false)
                        .put("message", "No se pudieron leer los datos: ${e.localizedMessage}")
                )
            }
        }
    }

    private fun sendMessage(data: JSONObject) {
        if (!::webView.isInitialized) return
        val jsonString = JSONObject.quote(data.toString())
        webView.post {
            webView.evaluateJavascript(
                "window.onHealthConnectResult && window.onHealthConnectResult($jsonString);",
                null
            )
        }
    }
}
