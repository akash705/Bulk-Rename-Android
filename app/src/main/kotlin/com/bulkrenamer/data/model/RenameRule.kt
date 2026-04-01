package com.bulkrenamer.data.model

sealed class RenameRule {
    abstract fun apply(node: FileNode): String

    data class AddPrefix(val prefix: String) : RenameRule() {
        override fun apply(node: FileNode): String = "$prefix${node.name}"
    }

    data class AddSuffix(
        val suffix: String,
        val beforeExtension: Boolean = true
    ) : RenameRule() {
        override fun apply(node: FileNode): String {
            if (node.isDirectory || !beforeExtension) return "${node.name}$suffix"
            val ext = node.extension
            return if (ext.isNotEmpty()) {
                "${node.nameWithoutExtension}$suffix.$ext"
            } else {
                "${node.name}$suffix"
            }
        }
    }

    data class ChangeExtension(val newExtension: String) : RenameRule() {
        override fun apply(node: FileNode): String {
            if (node.isDirectory) return node.name
            return if (newExtension.isEmpty()) {
                node.nameWithoutExtension
            } else {
                "${node.nameWithoutExtension}.$newExtension"
            }
        }
    }

    data class SetBaseName(
        val baseName: String,
        val keepExtension: Boolean = true
    ) : RenameRule() {
        override fun apply(node: FileNode): String {
            if (node.isDirectory || !keepExtension) return baseName
            val ext = node.extension
            return if (ext.isNotEmpty()) "$baseName.$ext" else baseName
        }
    }

    data class ReplaceText(
        val find: String,
        val replace: String,
        val caseSensitive: Boolean = false
    ) : RenameRule() {
        override fun apply(node: FileNode): String =
            if (caseSensitive) node.name.replace(find, replace)
            else node.name.replace(find, replace, ignoreCase = true)
    }
}
