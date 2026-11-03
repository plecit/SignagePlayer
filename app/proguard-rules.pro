# Android TV Signage Player ProGuard Rules

# Preserve Room database models and DAOs
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Preserve OkHttp internals
-dontwarn okhttp3.**
-dontwarn okio.**

# Preserve Media3 ExoPlayer
-keep class androidx.media3.** { *; }

# Preserve XML models
-keepclassmembers class com.example.tvsignage.** {
    public <init>(...);
    public <methods>;
    public <fields>;
}
