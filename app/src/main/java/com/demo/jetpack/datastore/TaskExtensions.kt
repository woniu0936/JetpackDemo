package com.demo.jetpack.datastore

fun Task.toFormattedString(): String {
    return """
        Task ID: $id
        Task Title: $title
        Task Content: $content
        Create Time: $createTime
        Modify Time: $modifyTime
    """.trimIndent()
}
