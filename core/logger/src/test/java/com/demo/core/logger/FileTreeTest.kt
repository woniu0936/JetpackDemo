
package com.demo.core.logger

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileTreeTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `cleanup should delete old files`() {
        val logsDir = tempFolder.newFolder("logs")
        val keepDays = 7
        val fileTree = FileTree(logsDir, keepDays)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = Date()
        val oldDate = Calendar.getInstance().apply {
            time = today
            add(Calendar.DAY_OF_YEAR, -keepDays)
        }.time

        val newLogFile = File(logsDir, "tracker-${dateFormat.format(today)}.log")
        newLogFile.createNewFile()
        val oldLogFile = File(logsDir, "tracker-${dateFormat.format(oldDate)}.log")
        oldLogFile.createNewFile()
        oldLogFile.setLastModified(oldDate.time)

        // Trigger cleanup by logging
        fileTree.log(0, "test", "test", null)

        // Wait for the cleanup to finish
        Thread.sleep(1000)

        assertTrue(newLogFile.exists())
        assertFalse(oldLogFile.exists())
    }
}
