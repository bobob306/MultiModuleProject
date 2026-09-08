# Keep baby care navigation routes
-keep class com.bsdevs.babycare.presentation.navigation.** { *; }

# Keep sealed classes used in activity feed
-keep class com.bsdevs.babycare.presentation.home.HomeFeedItem { *; }
-keep class com.bsdevs.babycare.presentation.home.HomeFeedItem$* { *; }
-keep class com.bsdevs.babycare.presentation.common.BabyActivity { *; }
-keep class com.bsdevs.babycare.presentation.common.BabyActivity$* { *; }
