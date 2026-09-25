package com.foodario.inventory.presentation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

internal fun formatDate(date: LocalDate): String =
    "${date.day.toString().padStart(2, '0')}/${date.month.number.toString().padStart(2, '0')}/${date.year}"
