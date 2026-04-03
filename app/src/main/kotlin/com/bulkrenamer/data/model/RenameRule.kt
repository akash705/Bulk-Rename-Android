package com.bulkrenamer.data.model

sealed class RenameRule {
    /**
     * Apply this rule to [name], which may already be an intermediate result from a prior rule
     * in a chain. [isDirectory] is needed for extension-aware rules. [index] is the file's
     * position in the batch (0-based) and is only meaningful for [AddNumbering].
     */
    abstract fun apply(name: String, isDirectory: Boolean, index: Int = 0): String

    /** Human-readable summary for display in the rule chain UI. */
    abstract val displayLabel: String

    data class AddPrefix(val prefix: String) : RenameRule() {
        override fun apply(name: String, isDirectory: Boolean, index: Int): String = "$prefix$name"
        override val displayLabel: String get() = "Add prefix: \"$prefix\""
    }

    data class AddSuffix(
        val suffix: String,
        val beforeExtension: Boolean = true
    ) : RenameRule() {
        override fun apply(name: String, isDirectory: Boolean, index: Int): String {
            if (isDirectory || !beforeExtension) return "$name$suffix"
            val ext = nameExtension(name)
            return if (ext.isNotEmpty()) "${nameWithoutExt(name)}$suffix.$ext" else "$name$suffix"
        }
        override val displayLabel: String get() = "Add suffix: \"$suffix\""
    }

    data class ChangeExtension(val newExtension: String) : RenameRule() {
        override fun apply(name: String, isDirectory: Boolean, index: Int): String {
            if (isDirectory) return name
            return if (newExtension.isEmpty()) nameWithoutExt(name)
            else "${nameWithoutExt(name)}.$newExtension"
        }
        override val displayLabel: String
            get() = if (newExtension.isEmpty()) "Strip extension" else "Change ext → .$newExtension"
    }

    data class SetBaseName(
        val baseName: String,
        val keepExtension: Boolean = true
    ) : RenameRule() {
        override fun apply(name: String, isDirectory: Boolean, index: Int): String {
            if (isDirectory || !keepExtension) return baseName
            val ext = nameExtension(name)
            return if (ext.isNotEmpty()) "$baseName.$ext" else baseName
        }
        override val displayLabel: String get() = "Set base name: \"$baseName\""
    }

    data class ReplaceText(
        val find: String,
        val replace: String,
        val caseSensitive: Boolean = false,
        val useRegex: Boolean = false
    ) : RenameRule() {
        override fun apply(name: String, isDirectory: Boolean, index: Int): String {
            if (useRegex) {
                val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                return try {
                    Regex(find, options).replace(name, replace)
                } catch (_: Exception) {
                    name // invalid regex — return unchanged
                }
            }
            return if (caseSensitive) name.replace(find, replace)
            else name.replace(find, replace, ignoreCase = true)
        }
        override val displayLabel: String
            get() = if (useRegex) "Regex /$find/ → \"$replace\"" else "Replace \"$find\" → \"$replace\""
    }

    data class AddNumbering(
        val startAt: Int = 1,
        val step: Int = 1,
        val padWidth: Int = 0,
        val position: NumberPosition = NumberPosition.PREFIX,
        val separator: String = "_"
    ) : RenameRule() {
        override fun apply(name: String, isDirectory: Boolean, index: Int): String {
            val number = startAt + index * step
            val numStr = if (padWidth > 0) number.toString().padStart(padWidth, '0') else number.toString()
            return when (position) {
                NumberPosition.PREFIX -> "$numStr$separator$name"
                NumberPosition.SUFFIX -> {
                    val ext = nameExtension(name)
                    val base = nameWithoutExt(name)
                    if (ext.isNotEmpty() && !isDirectory) "$base$separator$numStr.$ext"
                    else "$name$separator$numStr"
                }
            }
        }
        override val displayLabel: String
            get() {
                val pad = if (padWidth > 0) ", ${padWidth}d" else ""
                return "Number (${position.name.lowercase()}$pad, start=$startAt)"
            }
    }
}

enum class NumberPosition { PREFIX, SUFFIX }

// Helpers shared by rules that need to split name/extension on an arbitrary string.
internal fun nameExtension(name: String): String {
    val lastDot = name.lastIndexOf('.')
    return if (lastDot > 0) name.substring(lastDot + 1) else ""
}

internal fun nameWithoutExt(name: String): String {
    val lastDot = name.lastIndexOf('.')
    return if (lastDot > 0) name.substring(0, lastDot) else name
}
