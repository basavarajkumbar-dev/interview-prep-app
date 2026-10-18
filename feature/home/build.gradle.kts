plugins {
    id("interviewprep.android.feature")
}

android {
    namespace = "com.interviewprep.feature.home"
}

dependencies {
    implementation(project(":feature:progress"))
}
