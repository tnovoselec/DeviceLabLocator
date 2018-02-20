package com.tnovoselec.android.devicelocator.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.tnovoselec.android.devicelocator.model.Device
import com.tnovoselec.android.devicelocator.model.Table
import io.reactivex.Completable
import io.reactivex.Single


class DeviceRepository(val firestore: FirebaseFirestore) {

  companion object {
    val DEVICES = "devices"
  }

  fun registerDevice(device: Device): Completable {

    return listDevices().map { it -> it.filter { remoteDevice -> remoteDevice.id == device.id } }
        .flatMapCompletable { deviceList ->
          if (deviceList.isEmpty()) {
            Completable.create { emitter ->
              val deviceData = HashMap<String, Any>()
              deviceData.put(Device.ID, device.id)
              deviceData.put(Device.NAME, device.name)

              firestore.collection(DEVICES).document(device.id)
                  .set(deviceData)
                  .addOnSuccessListener { emitter.onComplete() }
                  .addOnFailureListener { e -> emitter.onError(e) }
            }
          } else {
            Completable.complete()
          }
        }
  }

  fun listDevices(): Single<List<Device>> {
    return Single.create<List<Device>> { emitter ->
      firestore.collection(DEVICES)
          .get()
          .addOnCompleteListener { it ->
            if (it.isSuccessful) {
              val devices = toDevices(it.result)
              emitter.onSuccess(devices)
            } else {
              emitter.onError(RuntimeException("Failed to list devices"))
            }
          }
          .addOnFailureListener { e ->
            emitter.onError(e)
          }
    }

  }

  fun bindDeviceToTable(device: Device, table: Table): Completable {
    return Completable.create { emitter ->
      val deviceReference = firestore.collection(DEVICES).document(device.id)

      val deviceWithTable = HashMap<String, Any>()
      deviceWithTable.put(Device.ID, device.id)
      deviceWithTable.put(Device.NAME, device.name)
      deviceWithTable.put(Device.TABLE_ID, table.id)
      deviceWithTable.put(Device.TABLE_NAME, table.name)

      deviceReference.update(deviceWithTable)
          .addOnSuccessListener {
            emitter.onComplete()
          }
          .addOnFailureListener { e ->
            emitter.onError(e)
          }
    }
  }

  private fun toDevices(document: QuerySnapshot): List<Device> {
    val devices = ArrayList<Device>()

    if (document.isEmpty) {
      return devices
    }

    return document.map { it ->
      val id = it.data[Device.ID] as String
      val name = it.data[Device.NAME] as String
      val tableId = it.data[Device.TABLE_ID] as String?
      val tableName = it.data[Device.TABLE_NAME] as String?
      Device(id, name, tableId, tableName)
    }
  }
}