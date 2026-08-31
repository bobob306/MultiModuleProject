# Keep form data classes used with Kotlin serialization (navigation routes)
-keep class com.bsdevs.forms.navigation.FormRoute { *; }

# Keep form field data sealed class hierarchy (used in when expressions at runtime)
-keep class com.bsdevs.data.FormFieldData { *; }
-keep class com.bsdevs.data.FormFieldData$* { *; }
-keep class com.bsdevs.data.FormFieldCondition { *; }
-keep class com.bsdevs.data.FormSchemaData { *; }

# Keep form DTO classes used with Firestore HashMap mapping
-keep class com.bsdevs.network.dto.FormFieldDto { *; }
-keep class com.bsdevs.network.dto.FormSchemaDto { *; }
-keep class com.bsdevs.network.dto.FormSubmissionDto { *; }
-keep class com.bsdevs.network.dto.FormFieldConditionDto { *; }
