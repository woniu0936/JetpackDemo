
package com.demo.core.logger

import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CrashFileTreeTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `uncaughtException should write crash report`() {
        val logsDir = tempFolder.newFolder("logs")
        val crashFileTree = CrashFileTree(logsDir)
        crashFileTree.plant()

        val latch = CountDownLatch(1)
        val thread = Thread {
            try {
                throw RuntimeException("Test Crash")
            } finally {
                latch.countDown()
            }
        }
        thread.start()
        latch.await(1, TimeUnit.SECONDS)

        // Allow some time for the file to be written
        Thread.sleep(1000)

        val crashFile = logsDir.listFiles { f -> f.name.startsWith("crash-") }?.firstOrNull()
        assertTrue(crashFile != null)
        assertTrue(crashFile?.readText()?.contains("Test Crash") == true)
    }
}
