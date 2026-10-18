package com.jrussell.fitness.core.importers.export

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/** The result of reading an export file. */
sealed interface BundleDecode {
    /** A readable bundle; its rows still need [validateExportBundle]. */
    data class Decoded(val bundle: ExportBundle) : BundleDecode

    /** Made by a newer GetFit, in format [found]; this version can't read it. */
    data class NewerVersion(val found: Int) : BundleDecode

    /** Not a GetFit export, or a damaged one, for the reason in [reason]. */
    data class Unreadable(val reason: String) : BundleDecode
}

/**
 * Pretty-printed so a person can read an export. Unknown keys are ignored, so a later build that
 * adds fields within format v1 doesn't make its files unreadable here.
 */
private val exportJson: Json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    explicitNulls = true
}

private const val FORMAT_VERSION_KEY: String = "formatVersion"

fun encodeExportBundle(bundle: ExportBundle): String {
    return exportJson.encodeToString(ExportBundle.serializer(), bundle)
}

/**
 * Reads [text] as an export. The format version is checked first, so a file from a newer GetFit
 * is reported as such even when the rest of it would not parse with this version's classes.
 */
fun decodeExportBundle(text: String): BundleDecode {
    return try {
        val root: JsonObject = exportJson.parseToJsonElement(text).jsonObject
        val version: Int? = (root[FORMAT_VERSION_KEY] as? JsonPrimitive)?.intOrNull
        when {
            version == null -> BundleDecode.Unreadable("no $FORMAT_VERSION_KEY")
            version > EXPORT_FORMAT_VERSION -> BundleDecode.NewerVersion(version)
            else -> BundleDecode.Decoded(exportJson.decodeFromJsonElement(ExportBundle.serializer(), root))
        }
    } catch (error: SerializationException) {
        BundleDecode.Unreadable(error.message ?: "not valid JSON")
    } catch (error: IllegalArgumentException) {
        BundleDecode.Unreadable(error.message ?: "not a GetFit export")
    }
}
