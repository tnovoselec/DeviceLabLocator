package com.tnovoselec.android.devicelocator

import android.content.Context
import android.preference.PreferenceManager
import com.google.firebase.firestore.FirebaseFirestore
import com.tnovoselec.android.devicelocator.internal.PreferenceAccessor
import com.tnovoselec.android.devicelocator.repository.DeviceRepository

object ObjectGraph {

  private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
  private val deviceRepository = DeviceRepository(firestore)


  fun provideDeviceRepository(): DeviceRepository {
    return deviceRepository
  }

  fun providePreferenceAccessor(context: Context): PreferenceAccessor {
    return PreferenceAccessor(PreferenceManager.getDefaultSharedPreferences(context))
  }
}