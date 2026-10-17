import androidx.room.gradle.RoomExtension
import com.jrussell.fitness.buildlogic.libs
import com.jrussell.fitness.buildlogic.library
import com.jrussell.fitness.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin `app.android.room`: a module that defines a Room (SQLite) database.
 *
 * Used by `:core:database` (`user.db`, the user's data) and `:core:reference` (`reference.db`,
 * the read-only bundled food catalog). Apply it alongside `app.android.library`.
 *
 * Applying it:
 * - applies KSP, which runs Room's code generator for `@Entity`, `@Dao` and `@Database` classes;
 * - applies the Room Gradle plugin and writes each database version's schema as JSON to the
 *   module's `schemas/` directory. Those files are committed: a test in each database module
 *   checks the current version's JSON matches the compiled database, and migration tests compare
 *   every migration against them (the design allows explicit migrations only);
 * - adds the Room runtime and coroutine extensions, plus Room's migration-test helper and
 *   Robolectric so DAO and migration tests run on the JVM without a device.
 *
 * ```
 * plugins {
 *     alias(libs.plugins.app.android.library)
 *     alias(libs.plugins.app.android.room)
 * }
 * ```
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    /** Called by Gradle once when a module applies `app.android.room`. */
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("ksp"))
            pluginManager.apply(libs.pluginId("room"))

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                add("implementation", libs.library("room-runtime"))
                add("implementation", libs.library("room-ktx"))
                add("ksp", libs.library("room-compiler"))
                add("testImplementation", libs.library("room-testing"))
                add("testImplementation", libs.library("robolectric"))
            }
        }
    }
}
