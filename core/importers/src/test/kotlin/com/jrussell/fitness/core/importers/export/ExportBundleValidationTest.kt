package com.jrussell.fitness.core.importers.export

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.uuid.Uuid
import org.junit.jupiter.api.Test

class ExportBundleValidationTest {
    private val builtIns: Set<Uuid> = setOf(BUILT_IN_WEIGHT)

    @Test
    fun validBundle_hasNoProblems() {
        assertThat(validateExportBundle(aBundle(), builtIns)).isEmpty()
    }

    @Test
    fun validate_dayStart1440_isOutOfRange() {
        val row: DayBoundaryRecord = aDayBoundary(minutes = 1440)

        assertThat(validateExportBundle(aBundle(dayBoundaries = listOf(row)), builtIns))
            .containsExactly(BundleProblem.DayStartOutOfRange(row.id))
    }

    @Test
    fun validate_dayStart0And1439_areFine() {
        assertThat(validateExportBundle(aBundle(dayBoundaries = listOf(aDayBoundary(0))), builtIns)).isEmpty()
        assertThat(validateExportBundle(aBundle(dayBoundaries = listOf(aDayBoundary(1439))), builtIns)).isEmpty()
    }

    @Test
    fun validate_badZone_isUnknownTimeZone() {
        val row: MeasurementRecord = aMeasurement(timeZone = "Mars/Olympus")

        assertThat(validateExportBundle(aBundle(measurements = listOf(row)), builtIns))
            .containsExactly(BundleProblem.UnknownTimeZone(row.id, "Mars/Olympus"))
    }

    @Test
    fun validate_measurementOfMissingType_isUnknown() {
        val missing: Uuid = Uuid.parse("0199f0a1-2b3c-7d4e-8f50-6a7b8c9d0eff")
        val row: MeasurementRecord = aMeasurement(typeId = missing)

        assertThat(validateExportBundle(aBundle(measurements = listOf(row)), builtIns))
            .containsExactly(BundleProblem.UnknownMeasurementType(row.id, missing))
    }

    @Test
    fun validate_measurementOfBuiltInType_isFine() {
        val ofBuiltIn: MeasurementRecord = aMeasurement(typeId = BUILT_IN_WEIGHT)
        val ofBundled: MeasurementRecord = aMeasurement(typeId = CUSTOM_TYPE, enteredUnit = null)

        assertThat(validateExportBundle(aBundle(measurements = listOf(ofBuiltIn, ofBundled)), builtIns)).isEmpty()
    }

    @Test
    fun validate_NaNValue_isNonFinite() {
        val row: MeasurementRecord = aMeasurement(value = Double.NaN)

        assertThat(validateExportBundle(aBundle(measurements = listOf(row)), builtIns))
            .containsExactly(BundleProblem.NonFiniteValue(row.id))
    }

    @Test
    fun validate_unknownSource_isUnknownName() {
        val row: MeasurementRecord = aMeasurement(source = "FAX")

        assertThat(validateExportBundle(aBundle(measurements = listOf(row)), builtIns))
            .containsExactly(BundleProblem.UnknownName("source", "FAX"))
    }

    @Test
    fun validate_unknownUnitCode_isUnknownName() {
        assertThat(validateExportBundle(aBundle(types = listOf(aType(canonicalUnit = "furlong"))), builtIns))
            .containsExactly(BundleProblem.UnknownName("canonicalUnit", "furlong"))
        assertThat(validateExportBundle(aBundle(types = listOf(aType(canonicalUnit = "custom:mg/dL"))), builtIns))
            .isEmpty()
    }

    @Test
    fun validate_unknownEnteredUnit_isUnknownName() {
        assertThat(validateExportBundle(aBundle(measurements = listOf(aMeasurement(enteredUnit = "stone"))), builtIns))
            .containsExactly(BundleProblem.UnknownName("enteredUnit", "stone"))
        assertThat(validateExportBundle(aBundle(measurements = listOf(aMeasurement(enteredUnit = "lb"))), builtIns))
            .isEmpty()
    }
}
