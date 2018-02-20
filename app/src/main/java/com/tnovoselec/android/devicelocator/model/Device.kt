package com.tnovoselec.android.devicelocator.model

data class Device(val id: String, val name: String, val tableId: String?, val tableName: String?) {
  companion object {
    val ID = "id"
    val NAME = "name"
    val TABLE_ID = "tableId"
    val TABLE_NAME = "tableName"
  }
}