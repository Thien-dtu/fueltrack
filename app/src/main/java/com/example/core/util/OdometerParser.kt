package com.example.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Parses user odometer input according to product rules:
 * - Accepts integers (e.g. "1222") -> 1,222,000 meters
 * - Accepts up to 1 decimal place (e.g. "1222,4" or "1222.4") -> 1,222,400 meters
 * - Accepts "0,5" -> 500 meters
 * - Accepts "0" -> 0 meters
 * - Rejects inputs with more than 1 decimal digit (e.g. "1222,45")
 * - Rejects negative numbers or non-numeric inputs
 * - Stores internally as Long meters
 */
object OdometerParser {

    sealed class ParseResult {
        data class Success(val meters: Long) : ParseResult()
        data class Error(val message: String) : ParseResult()
    }

    fun parse(rawInput: String?): ParseResult {
        if (rawInput.isNullOrBlank()) {
            return ParseResult.Error("Vui lòng nhập chỉ số ODO")
        }

        // Clean up spaces
        val input = rawInput.trim().replace(" ", "")

        // Support both comma and dot as decimal separator
        val normalized = input.replace(',', '.')

        if (normalized.startsWith("-")) {
            return ParseResult.Error("ODO không được là số âm")
        }

        // Check format: digits optionally followed by single dot and exactly 0 or 1 digit
        // Regex: ^\d+(\.\d)?$
        val regex = Regex("""^\d+(\.\d)?$""")
        if (!regex.matches(normalized)) {
            if (normalized.contains(".") && normalized.substringAfter(".").length > 1) {
                return ParseResult.Error("ODO chỉ được có tối đa 1 chữ số thập phân (ví dụ: 1222 hoặc 1222,4)")
            }
            return ParseResult.Error("Định dạng ODO không hợp lệ")
        }

        return try {
            val bigDecimalKm = BigDecimal(normalized)
            if (bigDecimalKm < BigDecimal.ZERO) {
                return ParseResult.Error("ODO không được nhỏ hơn 0")
            }
            // 1 km = 1000 meters
            val meters = bigDecimalKm.multiply(BigDecimal(1000)).setScale(0, RoundingMode.HALF_UP).toLong()
            ParseResult.Success(meters)
        } catch (e: NumberFormatException) {
            ParseResult.Error("Không thể đọc chỉ số ODO")
        }
    }

    /**
     * Formats Long meters into user-friendly km string.
     * E.g. 1,222,400 meters -> "1.222,4" or "1,222.4" depending on comma flag
     */
    // Cached per (decimal style, has-fraction) combination. DecimalFormat is not
    // thread-safe, hence ThreadLocal; the four variants cover every call site.
    private val kmFormatCommaWhole = ThreadLocal.withInitial { kmFormatter(true, false) }
    private val kmFormatCommaFraction = ThreadLocal.withInitial { kmFormatter(true, true) }
    private val kmFormatDotWhole = ThreadLocal.withInitial { kmFormatter(false, false) }
    private val kmFormatDotFraction = ThreadLocal.withInitial { kmFormatter(false, true) }

    private fun kmFormatter(useCommaDecimal: Boolean, withFraction: Boolean): DecimalFormat {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            if (useCommaDecimal) {
                groupingSeparator = '.'
                decimalSeparator = ','
            } else {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
        }
        val pattern = if (withFraction) "#,##0.0" else "#,##0"
        return DecimalFormat(pattern, symbols)
    }

    fun formatKm(meters: Long, useCommaDecimal: Boolean = true): String {
        val hasFraction = meters % 1000 != 0L
        val formatter = when {
            useCommaDecimal && hasFraction -> kmFormatCommaFraction.get()!!
            useCommaDecimal -> kmFormatCommaWhole.get()!!
            hasFraction -> kmFormatDotFraction.get()!!
            else -> kmFormatDotWhole.get()!!
        }
        return formatter.format(meters / 1000.0)
    }

    /**
     * Formats strictly for input text field editing (e.g. "1222,4" without thousands separators)
     */
    fun formatForEdit(meters: Long, useComma: Boolean = true): String {
        val kmWhole = meters / 1000
        val tenths = (Math.abs(meters) % 1000) / 100
        val sep = if (useComma) "," else "."
        return if (tenths == 0L && meters % 1000 == 0L) {
            "$kmWhole"
        } else {
            "$kmWhole$sep$tenths"
        }
    }
}

object FuelCalculator {

    /**
     * Liters = amountPaid / pricePerLiter
     * e.g. 50,000 / 25,000 = 2.0 L
     * Returns 0.0 if invalid or division by zero.
     */
    fun calculateLiters(amountPaid: Long, pricePerLiter: Long): Double {
        if (amountPaid <= 0 || pricePerLiter <= 0) return 0.0
        val bdAmount = BigDecimal(amountPaid)
        val bdPrice = BigDecimal(pricePerLiter)
        return bdAmount.divide(bdPrice, 3, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Distance between two ODO readings in meters.
     * Returns null if current < previous (ODO rollback/reset).
     */
    fun calculateDistance(previousOdometerMeters: Long, currentOdometerMeters: Long): Long? {
        val diff = currentOdometerMeters - previousOdometerMeters
        return if (diff >= 0) diff else null
    }

    /**
     * Full tank to full tank consumption:
     * Consumption (L/100km) = (liters / distanceKm) * 100
     * Returns null if distance is 0 or negative, or liters <= 0
     */
    fun calculateConsumptionLPer100Km(liters: Double, distanceMeters: Long): Double? {
        if (liters <= 0.0 || distanceMeters <= 0L) return null
        val distanceKm = distanceMeters / 1000.0
        if (distanceKm <= 0.0) return null
        val result = (liters / distanceKm) * 100.0
        return BigDecimal(result).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * km per Liter:
     * km/L = distanceKm / liters
     */
    fun calculateKilometersPerLiter(liters: Double, distanceMeters: Long): Double? {
        if (liters <= 0.0 || distanceMeters <= 0L) return null
        val distanceKm = distanceMeters / 1000.0
        if (distanceKm <= 0.0) return null
        val result = distanceKm / liters
        return BigDecimal(result).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Cost per km in VND:
     * Cost/km = fuelCost / distanceKm
     */
    fun calculateCostPerKm(fuelCostVnd: Long, distanceMeters: Long): Double? {
        if (fuelCostVnd <= 0L || distanceMeters <= 0L) return null
        val distanceKm = distanceMeters / 1000.0
        if (distanceKm <= 0.0) return null
        val result = fuelCostVnd.toDouble() / distanceKm
        return BigDecimal(result).setScale(0, RoundingMode.HALF_UP).toDouble()
    }
}

object Formatters {

    // SimpleDateFormat/DecimalFormat are NOT thread-safe. Each coroutine/IO thread gets
    // its own instance via ThreadLocal, so we pay the construction cost once per thread
    // instead of once per call (these run inside composition, so that matters).
    private val currencyFormat = ThreadLocal.withInitial {
        DecimalFormat("#,###", DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = '.'
        })
    }

    private val litersFormat = ThreadLocal.withInitial {
        DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US).apply {
            decimalSeparator = ','
        })
    }

    private val dateTimeFormat = ThreadLocal.withInitial {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }

    private val dateOnlyFormat = ThreadLocal.withInitial {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    }

    private val monthYearFormat = ThreadLocal.withInitial {
        SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("vi-VN"))
    }

    fun formatCurrencyVnd(amount: Long): String {
        return "${currencyFormat.get()!!.format(amount)} đ"
    }

    fun formatLiters(liters: Double): String {
        return "${litersFormat.get()!!.format(liters)} L"
    }

    fun formatDistanceKm(meters: Long): String {
        return "${OdometerParser.formatKm(meters, useCommaDecimal = true)} km"
    }

    fun formatConsumption(lPer100Km: Double?): String {
        if (lPer100Km == null) return "Chưa đủ dữ liệu"
        return "${litersFormat.get()!!.format(lPer100Km)} L/100km"
    }

    fun formatDateTime(epochMillis: Long): String {
        return dateTimeFormat.get()!!.format(Date(epochMillis))
    }

    fun formatDateOnly(epochMillis: Long): String {
        return dateOnlyFormat.get()!!.format(Date(epochMillis))
    }

    fun formatMonthYear(epochMillis: Long): String {
        val formatted = monthYearFormat.get()!!.format(Date(epochMillis))
        return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}
