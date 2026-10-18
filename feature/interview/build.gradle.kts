plugins {
    id("interviewprep.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.interviewprep.feature.interview"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
