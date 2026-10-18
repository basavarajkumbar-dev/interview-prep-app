package com.interviewprep.core.data.repo

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class TimeProvider @Inject constructor() {
    open fun now(): Long = System.currentTimeMillis()
    open fun zone(): ZoneId = ZoneId.systemDefault()
    fun dayOf(millis: Long): Long = Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate().toEpochDay()
    fun today(): Long = dayOf(now())
    fun startOfDay(epochDay: Long): Long = LocalDate.ofEpochDay(epochDay).atStartOfDay(zone()).toInstant().toEpochMilli()
    fun hourOfDay(): Int = Instant.ofEpochMilli(now()).atZone(zone()).hour
}
