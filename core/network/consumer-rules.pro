# Keep Kotlin Serialization DTOs and their generated serializers
-keep,allowobfuscation class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class **$serializer {
    public static final **$serializer INSTANCE;
}

# Keep Firestore DTOs in the network module
-keep class com.bsdevs.network.dto.** { *; }
