package com.tnovoselec.android.devicelocator

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentFilter.MalformedMimeTypeException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.tnovoselec.android.devicelocator.internal.PreferenceAccessor
import com.tnovoselec.android.devicelocator.model.Device
import com.tnovoselec.android.devicelocator.model.Table
import com.tnovoselec.android.devicelocator.repository.DeviceRepository
import kotlinx.android.synthetic.main.activity_main.*
import java.io.UnsupportedEncodingException
import java.util.*
import kotlin.experimental.and


class MainActivity : AppCompatActivity() {

  private val MIME_TEXT_PLAIN = "text/plain"

  private lateinit var deviceRepository: DeviceRepository
  private lateinit var preferenceAccessor: PreferenceAccessor

  private var nfcAdapter: NfcAdapter? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    deviceRepository = ObjectGraph.provideDeviceRepository()
    preferenceAccessor = ObjectGraph.providePreferenceAccessor(this)

    nfcAdapter = NfcAdapter.getDefaultAdapter(this)

    val device = createDevice()

    deviceId.setText(device.id)
    deviceName.setText(device.name)

    deviceRegister.setOnClickListener({ _ ->
      registerDevice()
    })
  }

  private fun registerDevice() {

    val name = deviceName.text.toString()
    val id = deviceId.text.toString()

    deviceRepository.registerDevice(Device(id, name, null, null)).subscribe({
      preferenceAccessor.setDeviceId(id)
      preferenceAccessor.setDeviceName(name)
    }, { error ->
      error.printStackTrace()
    })
  }

  private fun createDevice(): Device {
    val id = preferenceAccessor.getDeviceId() ?: ""
    val name = preferenceAccessor.getDeviceName() ?: Build.MODEL

    return Device(id, name, null, null)
  }

  override fun onResume() {
    super.onResume()
    setupForegroundDispatch(this, nfcAdapter)
  }

  override fun onPause() {
    super.onPause()
    stopForegroundDispatch(this, nfcAdapter)
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)

    if (intent != null && NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
      val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
      if (rawMessages != null) {
        val messages = arrayListOf<NdefMessage>()
        rawMessages.forEach { it -> messages.add(it as NdefMessage) }
        val message = processMessages(messages)
        val messageSplit = message.split(";")
        val tableId = messageSplit[0]
        val tableName = messageSplit[1]
        val table = Table(tableId, tableName)
        val device = createDevice()
        deviceRepository.bindDeviceToTable(device, table).subscribe()
      }

    }
  }

  private fun processMessages(messages: List<NdefMessage>): String {
    messages.flatMap { it -> it.records.toList() }.forEach { ndefRecord ->
      if (ndefRecord.tnf === NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.type, NdefRecord.RTD_TEXT)) {
        try {
          val text = readText(ndefRecord)
          Toast.makeText(this, "Muc: " + text, Toast.LENGTH_SHORT).show()
          return text
        } catch (e: UnsupportedEncodingException) {
          Log.e("processMessages", "Unsupported Encoding", e)
        }

      }
    }
    return ""
  }

  @Throws(UnsupportedEncodingException::class)
  private fun readText(record: NdefRecord): String {
    val payload = record.payload

    // Get the Text Encoding
    val textEncoding = if (payload[0].toInt() and 128 == 0) Charsets.UTF_8 else Charsets.UTF_16

    // Get the Language Code
    val languageCodeLength = payload[0] and 51

    // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
    // e.g. "en"

    // Get the Text
    return String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, textEncoding)
  }

  private fun setupForegroundDispatch(activity: Activity, adapter: NfcAdapter?) {
    val intent = Intent(activity.applicationContext, activity.javaClass)
    intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

    val pendingIntent = PendingIntent.getActivity(activity.applicationContext, 0, intent, 0)

    val filters = arrayOfNulls<IntentFilter>(1)
    val techList = arrayOf<Array<String>>()

    // Notice that this is the same filter as in our manifest.
    filters[0] = IntentFilter()
    filters[0]!!.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
    filters[0]!!.addCategory(Intent.CATEGORY_DEFAULT)
    try {
      filters[0]!!.addDataType(MIME_TEXT_PLAIN)
    } catch (e: MalformedMimeTypeException) {
      throw RuntimeException("Check your mime type.")
    }

    adapter?.enableForegroundDispatch(activity, pendingIntent, filters, techList)
  }

  private fun stopForegroundDispatch(activity: Activity, adapter: NfcAdapter?) {
    adapter?.disableForegroundDispatch(activity)
  }
}