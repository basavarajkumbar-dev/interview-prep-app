plugins {
    id("interviewprep.android.feature")
}

android {
    namespace = "com.interviewprep.feature.settings"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
