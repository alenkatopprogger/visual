package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class NetworkActivity : AppCompatActivity() {

    private lateinit var networkTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)

        networkTextView = findViewById(R.id.networkTextView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 3)
        } else {
            getNetworkInfo()
        }
    }

    private fun getNetworkInfo() {
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        val cellInfoList = telephonyManager.allCellInfo
        val stringBuilder = StringBuilder()

        for (cellInfo in cellInfoList) {
            when (cellInfo) {
                is CellInfoLte -> {
                    val cellIdentity = cellInfo.cellIdentity
                    val cellSignalStrength = cellInfo.cellSignalStrength
                    stringBuilder.append("LTE:\n")
                    stringBuilder.append("Band: ${cellIdentity.band}\n")
                    stringBuilder.append("Cell ID: ${cellIdentity.ci}\n")
                    stringBuilder.append("EARFCN: ${cellIdentity.earfcn}\n")
                    stringBuilder.append("MCC: ${cellIdentity.mccString}\n")
                    stringBuilder.append("MNC: ${cellIdentity.mncString}\n")
                    stringBuilder.append("PCI: ${cellIdentity.pci}\n")
                    stringBuilder.append("TAC: ${cellIdentity.tac}\n")
                    stringBuilder.append("ASU Level: ${cellSignalStrength.asuLevel}\n")
                    stringBuilder.append("CQI: ${cellSignalStrength.cqi}\n")
                    stringBuilder.append("RSRP: ${cellSignalStrength.rsrp}\n")
                    stringBuilder.append("RSRQ: ${cellSignalStrength.rsrq}\n")
                    stringBuilder.append("RSSI: ${cellSignalStrength.rssi}\n")
                    stringBuilder.append("RS SNR: ${cellSignalStrength.rssnr}\n")
                    stringBuilder.append("Timing Advance: ${cellSignalStrength.timingAdvance}\n\n")
                }
                is CellInfoGsm -> {
                    val cellIdentity = cellInfo.cellIdentity
                    val cellSignalStrength = cellInfo.cellSignalStrength
                    stringBuilder.append("GSM:\n")
                    stringBuilder.append("Cell ID: ${cellIdentity.cid}\n")
                    stringBuilder.append("BSIC: ${cellIdentity.bsic}\n")
                    stringBuilder.append("ARFCN: ${cellIdentity.arfcn}\n")
                    stringBuilder.append("LAC: ${cellIdentity.lac}\n")
                    stringBuilder.append("MCC: ${cellIdentity.mccString}\n")
                    stringBuilder.append("MNC: ${cellIdentity.mncString}\n")
                    stringBuilder.append("PSC: ${cellIdentity.psc}\n")
                    stringBuilder.append("Dbm: ${cellSignalStrength.dbm}\n")
                    stringBuilder.append("RSSI: ${cellSignalStrength.rssi}\n")
                    stringBuilder.append("Timing Advance: ${cellSignalStrength.timingAdvance}\n\n")
                }
                is CellInfoNr -> {
                    val cellIdentity = cellInfo.cellIdentity
                    val cellSignalStrength = cellInfo.cellSignalStrength
                    stringBuilder.append("NR:\n")
                    stringBuilder.append("Band: ${cellIdentity.band}\n")
                    stringBuilder.append("NCI: ${cellIdentity.nci}\n")
                    stringBuilder.append("PCI: ${cellIdentity.pci}\n")
                    stringBuilder.append("NRARFCN: ${cellIdentity.nrarfcn}\n")
                    stringBuilder.append("TAC: ${cellIdentity.tac}\n")
                    stringBuilder.append("MCC: ${cellIdentity.mccString}\n")
                    stringBuilder.append("MNC: ${cellIdentity.mncString}\n")
                    stringBuilder.append("SS-RSRP: ${cellSignalStrength.ssRsrp}\n")
                    stringBuilder.append("SS-RSRQ: ${cellSignalStrength.ssRsrq}\n")
                    stringBuilder.append("SS-SINR: ${cellSignalStrength.ssSinr}\n")
                    stringBuilder.append("Timing Advance: ${cellSignalStrength.timingAdvance}\n\n")
                }
            }
        }

        networkTextView.text = stringBuilder.toString()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 3 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getNetworkInfo()
        }
    }
}