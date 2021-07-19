package ru.mipt.npm.nica.emd

const val RANGE_SEPARATOR = "-"

interface RangeSupporting<T> {
    val isRangeSpecified: Boolean

    val minValue: T
    val maxValue: T
}

abstract class Parameter(val config: ParameterConfig, val stringValue: String) {
    companion object {
        fun fromParameterConfig(parameterConfig: ParameterConfig, value: String?): Parameter? {
            if (value.isNullOrEmpty()) return null
            when (parameterConfig.type.uppercase()) {
                // TODO also split by range support
                "INT" -> return IntParameter(parameterConfig, value)
                "FLOAT" -> return FloatParameter(parameterConfig, value)
                "STRING" -> return StringParameter(parameterConfig, value)
                "BOOL" -> return BooleanParameter(parameterConfig, value)
                else -> throw Exception("Unknown parameter type!")
            }
            // TODO also return null if cannot decode
        }
    }

    abstract fun generateSQLWhere(): String
}

class IntParameter(config: ParameterConfig, stringValue: String): Parameter(config, stringValue), RangeSupporting<Int> {
    override val isRangeSpecified = stringValue.contains(RANGE_SEPARATOR)
    override val minValue =
        if (isRangeSpecified) stringValue.substringBefore(RANGE_SEPARATOR).toInt() else stringValue.toInt()
    override val maxValue =
        if (isRangeSpecified) stringValue.substringAfter(RANGE_SEPARATOR).toInt() else stringValue.toInt()

    override fun generateSQLWhere(): String {
        return if (isRangeSpecified) {
            " ${config.name} BETWEEN $minValue and $maxValue"
        } else {
            " ${config.name} = $minValue"
        }
    }
}

class FloatParameter(config: ParameterConfig, stringValue: String) : Parameter(config, stringValue), RangeSupporting<Float> {
    override val isRangeSpecified = stringValue.contains(RANGE_SEPARATOR)
    override val minValue =
        if (isRangeSpecified) stringValue.substringBefore(RANGE_SEPARATOR).toFloat() else stringValue.toFloat()
    override val maxValue =
        if (isRangeSpecified) stringValue.substringAfter(RANGE_SEPARATOR).toFloat() else stringValue.toFloat()

    override fun generateSQLWhere(): String {
        return if (isRangeSpecified) {
            " ${config.name} BETWEEN $minValue and $maxValue"
        } else {
            " ${config.name} = $minValue"
        }
    }
}

class StringParameter(config: ParameterConfig, stringValue: String) : Parameter(config, stringValue) {
    override fun generateSQLWhere(): String {
        return " ${config.name} = '$stringValue'"
    }
}

class BooleanParameter(config: ParameterConfig, stringValue: String) : Parameter(config, stringValue) {
    override fun generateSQLWhere(): String {
        val sqlValue =  stringValue.uppercase()   // if (stringValue.uppercase() == "TRUE") 1 else 0
        return " ${config.name} = $sqlValue"
    }
}
