# ---------------------------------------------------------------------------
# FuelTrack - R8 / resource shrinking rules
# ---------------------------------------------------------------------------

# Keep line numbers so release crash reports can be de-obfuscated with
# app/build/outputs/mapping/release/mapping.txt (retrace). The source file name is
# rewritten to the literal "SourceFile" to save space: the real file name is
# recovered from mapping.txt anyway. Remove both lines for the smallest APK, at
# the cost of losing line numbers in stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Enums persisted as text ------------------------------------------------
# VehicleType, ScheduleLifecycleStatus and MaintenanceDueStatus are written to
# Room TEXT columns and to SharedPreferences via Enum.name, and read back with
# valueOf(). Without this, R8 may rename the constants and every stored value
# ("MOTORCYCLE", "ACTIVE", "OVERDUE") becomes unreadable, silently falling back
# to defaults or throwing IllegalArgumentException.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

# --- WorkManager ------------------------------------------------------------
# WorkManager persists the worker class name in its own database and recreates
# the worker by reflection. HiltWorkerFactory additionally looks the class up by
# name. Renaming MaintenanceReminderWorker would make every scheduled reminder
# fail with "Could not instantiate worker".
-keep class com.example.core.work.MaintenanceReminderWorker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepnames class * extends androidx.work.ListenableWorker

# --- Room -------------------------------------------------------------------
# room-runtime ships consumer rules, but the generated _Impl classes are looked
# up by name from AppDatabase, so their names must survive.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keepnames class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- Hilt / Dagger ----------------------------------------------------------
# Hilt generates components that reference members by name at runtime.
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

# --- Coroutines -------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# --- Kotlin metadata --------------------------------------------------------
# Keeps Kotlin reflection metadata for classes that still need it (Hilt, Room).
-keep class kotlin.Metadata { *; }

# --- Misc -------------------------------------------------------------------
# Keep annotation types referenced from kept classes.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# Compose and AndroidX ship their own consumer rules; nothing to add here.
