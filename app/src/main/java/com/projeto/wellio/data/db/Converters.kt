package com.projeto.wellio.data.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

class Converters {
    // Datas em ISO (yyyy-MM-dd): ordenáveis como texto e legíveis no banco
    @TypeConverter fun fromLocalDate(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
