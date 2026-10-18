plugins {
    id("interviewprep.jvm.library")
}

dependencies {
    api(project(":core:model"))
    implementation(libs.javax.inject)
}
