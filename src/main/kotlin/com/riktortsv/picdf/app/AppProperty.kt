package com.riktortsv.picdf.app

import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Paths
import java.util.*

object AppProperty: Properties() {

    val propertyFilePath = Paths.get(System.getProperty("user.dir")).resolve("picdf.properties")

    init {
        // init properties
    }

    fun load() {
        try {
            Files.newBufferedReader(propertyFilePath, charset("utf-8")).use { load(it) }
        } catch (e: NoSuchFileException) {
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun save() {
        try {
            Files.newBufferedWriter(propertyFilePath, charset("utf-8")).use { store(it, null) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun <T> setValue(key: String, value: T) {
        set(key, value.toString())
    }

    fun getInt(key: String): Int {
        return get(key)?.toString()?.toInt() ?: throw RuntimeException()
    }

    fun getDouble(key: String): Double {
        return get(key)?.toString()?.toDouble() ?: throw RuntimeException()
    }

    fun getBoolean(key: String): Boolean {
        return get(key)?.toString()?.toBoolean() ?: throw RuntimeException()
    }

    fun getString(key: String): String {
        return get(key)?.toString() ?: throw RuntimeException()
    }

}