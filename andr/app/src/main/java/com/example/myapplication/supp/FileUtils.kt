package com.example.myapplication.supp

import com.example.myapplication.data.LocationData
import org.json.JSONObject
import java.io.File
import java.io.FileWriter

object FileUtils {
    fun saveLocationToFile(locationData: LocationData, filesDir: File) {
        val jsonObject = JSONObject()
        jsonObject.put("latitude", locationData.latitude)
        jsonObject.put("longitude", locationData.longitude)
        jsonObject.put("altitude", locationData.altitude)
        jsonObject.put("time", locationData.time)

        val file = File(filesDir, "location_data.json")
        FileWriter(file, true).use { writer ->
            writer.write(jsonObject.toString() + "\n")
        }
    }
}