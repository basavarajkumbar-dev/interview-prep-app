plugins {
    id("interviewprep.android.library")
    id("interviewprep.android.compose")
}

android {
    namespace = "com.interviewprep.core.designsystem"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.core.ktx)
}
