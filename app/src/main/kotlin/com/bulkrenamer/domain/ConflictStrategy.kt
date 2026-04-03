package com.bulkrenamer.domain

enum class ConflictStrategy(val label: String) {
    AUTO_RENAME("Auto-rename"),
    OVERWRITE("Overwrite"),
    SKIP("Skip")
}
