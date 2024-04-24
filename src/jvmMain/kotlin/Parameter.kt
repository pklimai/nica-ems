package ru.mipt.npm.nica.ems

const val RANGE_SEPARATOR = "|"

interface RangeSupporting<T> {
    val isRangeSpecified: Boolean

    var minValue: T?
    var maxValue: T?
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

    var validParameter: Boolean = true   // If not, we should not try to return filtered result

    abstract fun generateSQLWhere(): String
}

class IntParameter(config: ParameterConfig, stringValue: String): Parameter(config, stringValue), RangeSupporting<Int> {

    override val isRangeSpecified = stringValue.contains(RANGE_SEPARATOR)
    override var minValue: Int? = null
    override var maxValue: Int? = null

    init {
        val minValueStr = stringValue.substringBefore(RANGE_SEPARATOR)
        val maxValueStr = stringValue.substringAfter(RANGE_SEPARATOR)

        if (minValueStr.isNotEmpty()) {
            minValue = minValueStr.toIntOrNull()
            if (minValue == null) {
                validParameter = false
            }
        }

        if (maxValueStr.isNotEmpty()) {
            maxValue = maxValueStr.toIntOrNull()
            if (maxValue == null) {
                validParameter = false
            }
        }

        if (minValue == null && maxValue == null) {
            validParameter = false
        }
    }

    override fun generateSQLWhere(): String {
        if (!isRangeSpecified) {
            return " ${config.name} = $minValue"
        }
        if (maxValue == null) {
            return " ${config.name} >= $minValue"
        }
        else if (minValue == null) {
            return " ${config.name} <= $maxValue"
        }
        return " ${config.name} BETWEEN $minValue AND $maxValue"
    }
}

class FloatParameter(config: ParameterConfig, stringValue: String): Parameter(config, stringValue), RangeSupporting<Float> {

    override val isRangeSpecified = stringValue.contains(RANGE_SEPARATOR)
    override var minValue: Float? = null
    override var maxValue: Float? = null

    init {
        val minValueStr = stringValue.substringBefore(RANGE_SEPARATOR)
        val maxValueStr = stringValue.substringAfter(RANGE_SEPARATOR)

        if (minValueStr.isNotEmpty()) {
            minValue = minValueStr.toFloatOrNull()
            if (minValue == null) {
                validParameter = false
            }
        }

        if (maxValueStr.isNotEmpty()) {
            maxValue = maxValueStr.toFloatOrNull()
            if (maxValue == null) {
                validParameter = false
            }
        }

        if (minValue == null && maxValue == null) {
            validParameter = false
        }
    }

    override fun generateSQLWhere(): String {
        if (!isRangeSpecified) {
            return " ${config.name} = $minValue"
        }
        if (maxValue == null) {
            return " ${config.name} >= $minValue"
        }
        else if (minValue == null) {
            return " ${config.name} <= $maxValue"
        }
        return " ${config.name} BETWEEN $minValue AND $maxValue"
    }
}

class StringParameter(config: ParameterConfig, stringValue: String) : Parameter(config, stringValue) {
    override fun generateSQLWhere(): String {
        return if (stringValue.startsWith("~")) {
            "${config.name} LIKE '${stringValue.substring(1)}'"
        }
        else {
            "${config.name} = '$stringValue'"
        }
    }
}

class BooleanParameter(config: ParameterConfig, stringValue: String) : Parameter(config, stringValue) {
    override fun generateSQLWhere(): String {
        val sqlValue =  stringValue.uppercase()   // if (stringValue.uppercase() == "TRUE") 1 else 0
        return " ${config.name} = $sqlValue"
    }
}
