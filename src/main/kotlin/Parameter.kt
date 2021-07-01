package com.example

const val RANGE_SEPARATOR = ".."

interface RangeSupporting<T> {
    val isRangeSpecified: Boolean

    val minValue: T
    val maxValue: T
}

abstract class Parameter(val name: String, val stringValue: String) {
    // val config: ParameterConfig? = null
    companion object {
        fun fromParameterConfig(parameterConfig: ParameterConfig, value: String?): Parameter? {
            if (value == null) return null
            when (parameterConfig.type.uppercase()) {
                // TODO also split by range support
                "INT" -> return IntParameter(parameterConfig.name, value)
                "FLOAT" -> return FloatParameter(parameterConfig.name, value)
                else -> return null
            }
            // TODO also return null if cannot decode
        }
    }

    abstract fun generateSQLWhere(): String

}

class IntParameter(name: String, stringValue: String): Parameter(name, stringValue), RangeSupporting<Int> {
    override val isRangeSpecified = stringValue.contains(RANGE_SEPARATOR)
    override val minValue =
        if (isRangeSpecified) stringValue.substringBefore(RANGE_SEPARATOR).toInt() else stringValue.toInt()
    override val maxValue =
        if (isRangeSpecified) stringValue.substringAfter(RANGE_SEPARATOR).toInt() else stringValue.toInt()

    override fun generateSQLWhere(): String {
        return if (isRangeSpecified) {
            " $name BETWEEN $minValue and $maxValue"
        } else {
            " $name = $minValue"
        }
    }

}

class FloatParameter(name: String, stringValue: String) : Parameter(name, stringValue), RangeSupporting<Float> {
    override val isRangeSpecified = stringValue.contains(RANGE_SEPARATOR)
    override val minValue =
        if (isRangeSpecified) stringValue.substringBefore(RANGE_SEPARATOR).toFloat() else stringValue.toFloat()
    override val maxValue =
        if (isRangeSpecified) stringValue.substringAfter(RANGE_SEPARATOR).toFloat() else stringValue.toFloat()

    override fun generateSQLWhere(): String {
        return if (isRangeSpecified) {
            "WHERE $name BETWEEN $minValue and $maxValue"
        } else {
            "WHERE $name = $minValue"
        }
    }

}