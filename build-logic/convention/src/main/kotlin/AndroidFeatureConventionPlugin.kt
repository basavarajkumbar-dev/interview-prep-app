import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** A feature module: Android library + Compose + Hilt + the core modules every screen needs. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("interviewprep.android.library")
        pluginManager.apply("interviewprep.android.compose")
        pluginManager.apply("interviewprep.android.hilt")
        dependencies {
            add("implementation", project(":core:model"))
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:data"))
            add("implementation", project(":core:designsystem"))
            add("implementation", libs.findLibrary("hilt.navigation.compose").get())
            add("implementation", libs.findLibrary("androidx.navigation.compose").get())
            add("implementation", libs.findLibrary("androidx.lifecycle.runtime.compose").get())
            add("implementation", libs.findLibrary("androidx.lifecycle.viewmodel.compose").get())
            add("implementation", libs.findLibrary("kotlinx.coroutines.android").get())
        }
    }
}
