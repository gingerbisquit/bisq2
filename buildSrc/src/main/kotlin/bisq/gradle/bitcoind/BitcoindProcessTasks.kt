package bisq.gradle.bitcoind

import bisq.gradle.tasks.bitcoind.BitcoindStopTask
import bisq.gradle.tasks.bitcoind.StartBitcoinQtTask
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.File

class BitcoindProcessTasks(
    project: Project,
    private val bitcoindDataDir: File,
    private val taskNameSuffix: String = ""
) {
    private val tasks: TaskContainer = project.tasks

    lateinit var startBitcoinQtTask: TaskProvider<StartBitcoinQtTask>
    lateinit var stopBitcoindTask: TaskProvider<BitcoindStopTask>

    fun registerTasks() {
        startBitcoinQtTask = registerBitcoindStartTask()
        stopBitcoindTask = registerBitcoindStopTask()
        registerCleanTask(stopBitcoindTask)
    }

    private fun registerBitcoindStartTask(): TaskProvider<StartBitcoinQtTask> =
        tasks.register<StartBitcoinQtTask>("bitcoindStart$taskNameSuffix") {
            dataDir.set(bitcoindDataDir)
        }

    private fun registerBitcoindStopTask(): TaskProvider<BitcoindStopTask> =
        tasks.register<BitcoindStopTask>("bitcoindStop$taskNameSuffix")

    private fun registerCleanTask(stopBitcoindTask: TaskProvider<BitcoindStopTask>): TaskProvider<Delete> =
        tasks.register<Delete>("cleanBitcoindRegtest$taskNameSuffix") {
            dependsOn(stopBitcoindTask)
            delete = setOf(bitcoindDataDir.absolutePath)
        }
}