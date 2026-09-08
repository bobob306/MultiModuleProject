# Keep models used for dynamic UI mapping
-keep class com.bsdevs.data.NetworkScreenData { *; }
-keep class com.bsdevs.data.NetworkScreenData$* { *; }
-keep class com.bsdevs.data.SpacerTypeData { *; }
-keep class com.bsdevs.data.LocationTypeData { *; }
-keep class com.bsdevs.data.ButtonTypeData { *; }

# Keep form field data sealed class hierarchy
-keep class com.bsdevs.data.FormFieldData { *; }
-keep class com.bsdevs.data.FormFieldData$* { *; }
-keep class com.bsdevs.data.FormFieldCondition { *; }
-keep class com.bsdevs.data.FormSchemaData { *; }

# Keep sync related models
-keep interface com.bsdevs.data.Syncable { *; }
-keep interface com.bsdevs.data.repository.Clearable { *; }
