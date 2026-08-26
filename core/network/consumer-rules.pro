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
