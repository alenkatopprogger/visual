package com.example.appandroid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.telephony.*
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import org.json.JSONObject
import org.zeromq.ZMQ
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class LocationActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var tvResult: TextView
    private lateinit var tvCellInfo: TextView
    private lateinit var tvFile: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var tvConnectionStatus: TextView
    private lateinit var etServerIp: EditText
    private lateinit var etServerPort: EditText
    private lateinit var btnConnect: Button
    private lateinit var btnDisconnect: Button

    private var zmqContext: ZMQ.Context? = null
    private var zmqSocket: ZMQ.Socket? = null
    private var isConnected = false
    private var sendingThread: Thread? = null
    private var autoSendEnabled = false

    private val PERMISSION_REQUEST_CODE = 100
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.READ_PHONE_STATE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        tvResult = findViewById(R.id.tvResult)
        tvCellInfo = findViewById(R.id.tvCellInfo)
        tvFile = findViewById(R.id.tvFile)
        scrollView = findViewById(R.id.scrollView)
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus)
        etServerIp = findViewById(R.id.etServerIp)
        etServerPort = findViewById(R.id.etServerPort)
        btnConnect = findViewById(R.id.btnConnect)
        btnDisconnect = findViewById(R.id.btnDisconnect)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // Загрузка сохраненных настроек
        loadServerSettings()

        findViewById<Button>(R.id.btnGetLocation).setOnClickListener {
            checkAndRequestPermissions()
        }

        btnConnect.setOnClickListener {
            connectToServer()
        }

        btnDisconnect.setOnClickListener {
            disconnectFromServer()
        }
    }

    private fun loadServerSettings() {
        val prefs = getSharedPreferences("server_settings", Context.MODE_PRIVATE)
        etServerIp.setText(prefs.getString("server_ip", "172.20.10.2"))
        etServerPort.setText(prefs.getString("server_port", "7777"))
    }

    private fun saveServerSettings() {
        val prefs = getSharedPreferences("server_settings", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("server_ip", etServerIp.text.toString())
            putString("server_port", etServerPort.text.toString())
            apply()
        }
    }

    private fun connectToServer() {
        val ip = etServerIp.text.toString().trim()
        val port = etServerPort.text.toString().trim()

        if (ip.isEmpty() || port.isEmpty()) {
            showStatus("Введите IP и порт сервера", false)
            return
        }

        saveServerSettings()
        showStatus("Подключение к $ip:$port...", true)

        thread {
            try {
                // Исправленный способ создания контекста и сокета
                zmqContext = ZMQ.context(1)
                zmqSocket = zmqContext?.socket(ZMQ.PUSH)  // Исправлено: используем socket() вместо createSocket()

                val address = "tcp://$ip:$port"
                zmqSocket?.connect(address)

                isConnected = true

                runOnUiThread {
                    updateConnectionUI(true)
                    showStatus("Подключено к $address", true)
                    Snackbar.make(findViewById(android.R.id.content),
                        "Подключено к серверу", Snackbar.LENGTH_SHORT).show()

                    // Автоматически начинаем отправку данных
                    startAutoSending()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    showStatus("Ошибка подключения: ${e.message}", false)
                    updateConnectionUI(false)
                }
            }
        }
    }

    private fun disconnectFromServer() {
        stopAutoSending()

        thread {
            try {
                zmqSocket?.close()
                zmqContext?.term()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isConnected = false
                zmqSocket = null
                zmqContext = null

                runOnUiThread {
                    updateConnectionUI(false)
                    showStatus("Отключено от сервера", false)
                    Snackbar.make(findViewById(android.R.id.content),
                        "Отключено от сервера", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateConnectionUI(connected: Boolean) {
        btnConnect.isEnabled = !connected
        btnDisconnect.isEnabled = connected
        etServerIp.isEnabled = !connected
        etServerPort.isEnabled = !connected
    }

    private fun showStatus(message: String, isConnected: Boolean) {
        tvConnectionStatus.text = "Статус: $message"
        tvConnectionStatus.setTextColor(
            if (isConnected)
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            else
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
        )
    }

    private fun startAutoSending() {
        autoSendEnabled = true
        sendingThread = thread {
            while (autoSendEnabled && isConnected) {
                val location = getLocation()
                val cellInfo = getCellInfo()

                if (location != null || cellInfo != null) {
                    val data = prepareDataForSending(location, cellInfo)
                    sendDataToServer(data)
                }

                Thread.sleep(5000) // Отправка каждые 5 секунд
            }
        }
    }

    private fun stopAutoSending() {
        autoSendEnabled = false
        sendingThread?.interrupt()
    }

    private fun prepareDataForSending(location: Location?, cellInfo: JSONObject?): JSONObject {
        val data = JSONObject()

        if (location != null) {
            val locationJson = JSONObject().apply {
                put("latitude", location.latitude)
                put("longitude", location.longitude)
                put("altitude", location.altitude)
                put("accuracy", location.accuracy)
                put("timestamp", location.time)
                put("provider", location.provider ?: "unknown")
            }
            data.put("location", locationJson)
        }

        if (cellInfo != null && cellInfo.has("cellInfo")) {
            data.put("cellInfo", cellInfo.getJSONArray("cellInfo"))
        }

        data.put("deviceId", Build.MODEL)
        data.put("timestamp", System.currentTimeMillis())
        data.put("androidVersion", Build.VERSION.RELEASE)

        return data
    }

    private fun sendDataToServer(data: JSONObject) {
        if (!isConnected || zmqSocket == null) return

        try {
            val message = data.toString()
            val sent = zmqSocket?.send(message)

            runOnUiThread {
                tvFile.text = "Отправлено: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
                tvFile.postDelayed({ if (autoSendEnabled) tvFile.text = "" }, 2000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                showStatus("Ошибка отправки: ${e.message}", false)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            getLocationAndCellInfo()
        } else {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                getLocationAndCellInfo()
            } else {
                tvResult.text = "Разрешения не предоставлены"
            }
        }
    }

    private fun getLocationAndCellInfo() {
        thread {
            val location = getLocation()
            val cellInfo = getCellInfo()

            runOnUiThread {
                if (location != null) {
                    showLocation(location)
                } else {
                    tvResult.text = "Местоположение недоступно.\nВключите GPS/Location и попробуйте снова."
                }

                if (cellInfo != null) {
                    showCellInfo(cellInfo)
                } else {
                    tvCellInfo.text = "Информация о соте недоступна"
                }

                saveAllData(location, cellInfo)

                // Отправка данных на сервер если подключены
                if (isConnected && (location != null || cellInfo != null)) {
                    val data = prepareDataForSending(location, cellInfo)
                    sendDataToServer(data)
                }

                scrollView.post {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }
    }

    private fun getLocation(): Location? {
        return try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                var location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }
                location
            } else {
                null
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            null
        }
    }

    private fun getCellInfo(): JSONObject? {
        return try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

                val allCellInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    telephonyManager.allCellInfo
                } else {
                    @Suppress("DEPRECATION")
                    telephonyManager.allCellInfo
                }

                if (allCellInfo != null && allCellInfo.isNotEmpty()) {
                    processCellInfo(allCellInfo)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun processCellInfo(cellInfoList: MutableList<CellInfo>): JSONObject {
        val result = JSONObject()
        val cellInfoArray = JSONArray()

        for (cellInfo in cellInfoList) {
            val cellData = JSONObject()

            when (cellInfo) {
                is CellInfoLte -> {
                    cellData.put("networkType", "LTE")
                    val identity = cellInfo.cellIdentity
                    val identityJson = JSONObject().apply {
                        put("cellIdentity", identity?.ci ?: 0)
                        put("earfcn", identity?.earfcn ?: 0)
                        put("mcc", identity?.mccString ?: "")
                        put("mnc", identity?.mncString ?: "")
                        put("pci", identity?.pci ?: 0)
                        put("tac", identity?.tac ?: 0)
                    }
                    cellData.put("cellIdentity", identityJson)

                    val signal = cellInfo.cellSignalStrength
                    val signalJson = JSONObject().apply {
                        put("asuLevel", signal?.asuLevel ?: 0)
                        put("cqi", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) signal?.cqi ?: 0 else 0)
                        put("rsrp", signal?.rsrp ?: 0)
                        put("rsrq", signal?.rsrq ?: 0)
                        put("rssi", signal?.rssi ?: 0)
                        put("rssnr", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) signal?.rssnr ?: 0 else 0)
                        put("timingAdvance", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) signal?.timingAdvance ?: 0 else 0)
                    }
                    cellData.put("signalStrength", signalJson)
                }

                is CellInfoGsm -> {
                    cellData.put("networkType", "GSM")
                    val identity = cellInfo.cellIdentity
                    val identityJson = JSONObject().apply {
                        put("cellIdentity", identity?.cid ?: 0)
                        put("bsic", identity?.bsic ?: 0)
                        put("arfcn", identity?.arfcn ?: 0)
                        put("lac", identity?.lac ?: 0)
                        put("mcc", identity?.mccString ?: "")
                        put("mnc", identity?.mncString ?: "")
                        put("psc", identity?.psc ?: 0)
                    }
                    cellData.put("cellIdentity", identityJson)

                    val signal = cellInfo.cellSignalStrength
                    val signalJson = JSONObject().apply {
                        put("dbm", signal?.dbm ?: 0)
                        put("timingAdvance", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) signal?.timingAdvance ?: 0 else 0)
                    }
                    cellData.put("signalStrength", signalJson)
                }

                is CellInfoWcdma -> {
                    cellData.put("networkType", "WCDMA")
                    val identity = cellInfo.cellIdentity
                    val identityJson = JSONObject().apply {
                        put("cellIdentity", identity?.cid ?: 0)
                        put("lac", identity?.lac ?: 0)
                        put("mcc", identity?.mccString ?: "")
                        put("mnc", identity?.mncString ?: "")
                        put("psc", identity?.psc ?: 0)
                        put("uarfcn", identity?.uarfcn ?: 0)
                    }
                    cellData.put("cellIdentity", identityJson)

                    val signal = cellInfo.cellSignalStrength
                    val signalJson = JSONObject().apply {
                        put("dbm", signal?.dbm ?: 0)
                        put("asuLevel", signal?.asuLevel ?: 0)
                    }
                    cellData.put("signalStrength", signalJson)
                }

                is CellInfoCdma -> {
                    cellData.put("networkType", "CDMA")
                    val identity = cellInfo.cellIdentity
                    val identityJson = JSONObject().apply {
                        put("basestationId", identity?.basestationId ?: 0)
                        put("latitude", identity?.latitude ?: 0)
                        put("longitude", identity?.longitude ?: 0)
                        put("networkId", identity?.networkId ?: 0)
                        put("systemId", identity?.systemId ?: 0)
                    }
                    cellData.put("cellIdentity", identityJson)

                    val signal = cellInfo.cellSignalStrength
                    val signalJson = JSONObject().apply {
                        put("dbm", signal?.dbm ?: 0)
                        put("asuLevel", signal?.asuLevel ?: 0)
                    }
                    cellData.put("signalStrength", signalJson)
                }

                else -> {
                    cellData.put("networkType", "UNKNOWN")
                }
            }

            cellInfoArray.put(cellData)
        }

        result.put("cellInfo", cellInfoArray)
        return result
    }

    private fun showLocation(location: Location) {
        val date = Date(location.time)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        tvResult.text = buildString {
            append("МЕСТОПОЛОЖЕНИЕ\n")
            append("═══════════════════════\n")
            append("Широта: ${"%.6f".format(location.latitude)}\n")
            append("Долгота: ${"%.6f".format(location.longitude)}\n")
            append("Высота: ${"%.1f".format(location.altitude)} м\n")
            append("Точность: ${"%.1f".format(location.accuracy)} м\n")
            append("Время: ${dateFormat.format(date)}\n")
            append("Провайдер: ${location.provider ?: "Неизвестно"}")
        }
    }

    private fun showCellInfo(cellInfo: JSONObject) {
        val cellInfoArray = cellInfo.getJSONArray("cellInfo")
        if (cellInfoArray.length() == 0) {
            tvCellInfo.text = "Информация о соте недоступна"
            return
        }

        tvCellInfo.text = buildString {
            append("📡 ИНФОРМАЦИЯ О СОТОВОЙ СЕТИ\n")
            append("═══════════════════════════════════\n")

            for (i in 0 until cellInfoArray.length()) {
                val cell = cellInfoArray.getJSONObject(i)
                val networkType = cell.getString("networkType")
                val identity = cell.getJSONObject("cellIdentity")
                val signal = cell.getJSONObject("signalStrength")

                append("\n━━━ $networkType ━━━\n")

                when (networkType) {
                    "LTE" -> {
                        append("Cell ID: ${identity.getInt("cellIdentity")}\n")
                        append("MCC/MNC: ${identity.getString("mcc")}/${identity.getString("mnc")}\n")
                        append("EARFCN: ${identity.getInt("earfcn")}\n")
                        append("PCI: ${identity.getInt("pci")}\n")
                        append("TAC: ${identity.getInt("tac")}\n")
                        append("━━━━━━━━━━━━━━━━━━━━\n")
                        append("ASU Level: ${signal.getInt("asuLevel")}\n")
                        append("CQI: ${signal.getInt("cqi")}\n")
                        append("RSRP: ${signal.getInt("rsrp")} dBm\n")
                        append("RSRQ: ${signal.getInt("rsrq")} dB\n")
                        append("RSSI: ${signal.getInt("rssi")} dBm\n")
                        append("RSSNR: ${signal.getInt("rssnr")} dB\n")
                        append("Timing Advance: ${signal.getInt("timingAdvance")}")
                    }

                    "GSM" -> {
                        append("Cell ID: ${identity.getInt("cellIdentity")}\n")
                        append("BSIC: ${identity.getInt("bsic")}\n")
                        append("ARFCN: ${identity.getInt("arfcn")}\n")
                        append("LAC: ${identity.getInt("lac")}\n")
                        append("MCC/MNC: ${identity.getString("mcc")}/${identity.getString("mnc")}\n")
                        append("PSC: ${identity.getInt("psc")}\n")
                        append("━━━━━━━━━━━━━━━━━━━━\n")
                        append("Signal: ${signal.getInt("dbm")} dBm\n")
                        append("Timing Advance: ${signal.getInt("timingAdvance")}")
                    }

                    "WCDMA" -> {
                        append("Cell ID: ${identity.getInt("cellIdentity")}\n")
                        append("LAC: ${identity.getInt("lac")}\n")
                        append("MCC/MNC: ${identity.getString("mcc")}/${identity.getString("mnc")}\n")
                        append("PSC: ${identity.getInt("psc")}\n")
                        append("UARFCN: ${identity.getInt("uarfcn")}\n")
                        append("━━━━━━━━━━━━━━━━━━━━\n")
                        append("Signal: ${signal.getInt("dbm")} dBm\n")
                        append("ASU: ${signal.getInt("asuLevel")}")
                    }

                    "CDMA" -> {
                        append("Base Station ID: ${identity.getInt("basestationId")}\n")
                        append("Network ID: ${identity.getInt("networkId")}\n")
                        append("System ID: ${identity.getInt("systemId")}\n")
                        append("Lat/Lon: ${identity.getInt("latitude")}/${identity.getInt("longitude")}\n")
                        append("━━━━━━━━━━━━━━━━━━━━\n")
                        append("Signal: ${signal.getInt("dbm")} dBm\n")
                        append("ASU: ${signal.getInt("asuLevel")}")
                    }

                    else -> {
                        append("Неизвестный тип сети")
                    }
                }

                if (i < cellInfoArray.length() - 1) {
                    append("\n\n")
                }
            }
        }
    }

    private fun saveAllData(location: Location?, cellInfo: JSONObject?) {
        try {
            val jsonData = JSONObject()

            if (location != null) {
                val locationJson = JSONObject().apply {
                    put("latitude", location.latitude)
                    put("longitude", location.longitude)
                    put("altitude", location.altitude)
                    put("accuracy", location.accuracy)
                    put("timestamp", location.time)
                    put("provider", location.provider ?: "unknown")
                }
                jsonData.put("location", locationJson)
            }

            if (cellInfo != null) {
                jsonData.put("cellInfo", cellInfo.getJSONArray("cellInfo"))
            }

            jsonData.put("deviceId", Build.MODEL)
            jsonData.put("timestamp", System.currentTimeMillis())

            val file = File(filesDir, "android_data.json")
            val existingData = if (file.exists() && file.length() > 0) {
                JSONArray(file.readText())
            } else {
                JSONArray()
            }

            existingData.put(jsonData)
            file.writeText(existingData.toString(2))

            tvFile.text = "Данные сохранены: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
            tvFile.postDelayed({ if (!autoSendEnabled) tvFile.text = "" }, 3000)

        } catch (e: Exception) {
            e.printStackTrace()
            tvFile.text = "Ошибка сохранения: ${e.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectFromServer()
    }
}