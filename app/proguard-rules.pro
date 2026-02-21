# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Markwon
-keep class io.noties.markwon.** { *; }

# SQLCipher (JNI expects exact fields/methods)
-keep class net.sqlcipher.** { *; }
-keepclasseswithmembers class net.sqlcipher.** {
    native <methods>;
}
