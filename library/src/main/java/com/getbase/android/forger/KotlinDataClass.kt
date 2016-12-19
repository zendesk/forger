package com.getbase.android.forger

import org.chalup.microorm.annotations.Column

data class KotlinDataClass(
        @Column("data") val data: Long
)