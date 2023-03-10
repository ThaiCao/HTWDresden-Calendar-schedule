package de.htwdd.htwdresden.utils.extensions

import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.htwdd.htwdresden.utils.holders.CryptoSharedPreferencesHolder
import java.util.*

fun <T: Any> T.TAG() = this::class.java.simpleName

inline fun <T> T.guard(block: T.() -> Unit): T {
    if (this == null) {
        block()
    }
    return this
}

fun <T: Any> T.verbose(message: Any?) = de.htwdd.htwdresden.utils.verbose(TAG(), message)

fun <T: Any> T.debug(message: Any?) = de.htwdd.htwdresden.utils.debug(TAG(), message)

fun <T: Any> T.info(message: Any?) = de.htwdd.htwdresden.utils.info(TAG(), message)

fun <T: Any> T.warn(message: Any?) = de.htwdd.htwdresden.utils.warn(TAG(), message)

fun <T: Any> T.error(message: Any?) = de.htwdd.htwdresden.utils.error(TAG(), message)

val Any.currentYear: Int
        get() {
            val calendar = Calendar.getInstance()
            return calendar[Calendar.YEAR]
        }

val Any.currentWeek: Int
    get() {
        val calendar = Calendar.getInstance()
        return calendar[Calendar.WEEK_OF_YEAR]
    }

val Any.today: String
    get() {
        val calendar = Calendar.getInstance()
        return calendar.time.format("dd.MM.yyyy")
    }


fun Any.getDaysOfWeek(currentWeek: Boolean = true): Array<Date?> {
    val calendar = Calendar.getInstance()
    calendar[Calendar.DAY_OF_WEEK] = Calendar.getInstance().firstDayOfWeek
    val daysOfWeek = arrayOfNulls<Date>(5)
    val daysOfNextWeek = arrayOfNulls<Date>(5)
    for (i in 0..4) {
        daysOfWeek[i] = calendar.time
        Calendar.getInstance().apply {
            time = calendar.time
            add(Calendar.DAY_OF_MONTH, 7)
            daysOfNextWeek[i] = time
        }
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }
    return if (currentWeek) {
        daysOfWeek
    } else {
        daysOfNextWeek
    }
}

fun Any.handleCrashlyticsChange() {
    if (CryptoSharedPreferencesHolder.instance.hasCrashlytics()) {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    } else {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
    }
}