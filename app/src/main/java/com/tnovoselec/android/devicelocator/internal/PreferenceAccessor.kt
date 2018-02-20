package com.tnovoselec.android.devicelocator.internal

import android.content.SharedPreferences

class PreferenceAccessor(val sharedPreferences: SharedPreferences) {

  companion object {
    val DEVICE_ID = "deviceId"
    val DEVICE_NAME = "deviceName"
  }

  fun setDeviceId(deviceId: String) {
    sharedPreferences.edit().putString(DEVICE_ID, deviceId).apply()
  }

  fun getDeviceId(): String? = sharedPreferences.getString(DEVICE_ID, null)


  fun setDeviceName(deviceName: String) {
    sharedPreferences.edit().putString(DEVICE_NAME, deviceName).apply()
  }

  fun getDeviceName() :String? = sharedPreferences.getString(DEVICE_NAME, null)
}