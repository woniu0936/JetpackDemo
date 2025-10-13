
package com.demo.core.logger

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipFile

class ZipUtilTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `zipFiles should return true for valid files`() {
        val file1 = tempFolder.newFile("file1.txt")
        file1.writeText("hello")
        val file2 = tempFolder.newFile("file2.txt")
        file2.writeText("world")
        val zipFile = tempFolder.newFile("test.zip")

        val result = ZipUtil.zipFiles(listOf(file1, file2), zipFile)

        assertTrue(result)
        ZipFile(zipFile).use {
            assertTrue(it.getEntry("file1.txt") != null)
            assertTrue(it.getEntry("file2.txt") != null)
        }
    }

    @Test
    fun `zipFiles should return false for empty list`() {
        val zipFile = tempFolder.newFile("test.zip")
        val result = ZipUtil.zipFiles(emptyList(), zipFile)
        assertFalse(result)
    }

    @Test
    fun `zipFiles should skip non-existent files`() {
        val file1 = tempFolder.newFile("file1.txt")
        file1.writeText("hello")
        val nonExistentFile = File(tempFolder.root, "nonexistent.txt")
        val zipFile = tempFolder.newFile("test.zip")

        val result = ZipUtil.zipFiles(listOf(file1, nonExistentFile), zipFile)

        assertTrue(result)
        ZipFile(zipFile).use {
            assertTrue(it.getEntry("file1.txt") != null)
            assertFalse(it.getEntry("nonexistent.txt") != null)
        }
    }
}
