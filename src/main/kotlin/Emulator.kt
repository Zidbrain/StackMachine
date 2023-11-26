import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import machine.Command
import machine.StackMachine
import java.util.logging.Level

@Stable
class Emulator {
    var machine by mutableStateOf(StackMachine())
        private set

    var isCompiling by mutableStateOf(false)
        private set

    var compilationError: CompilationError? by mutableStateOf(null)
        private set

    var currentCommand: Int? by mutableStateOf(null)
        private set

    private val scope = CoroutineScope(Dispatchers.Default)
    private val logger = java.util.logging.Logger.getGlobal()
    private val labels = mutableMapOf<String, Int>()
    private var programStart: Int = 0

    fun reset() {
        isCompiling = false
        compilationError = null
        currentCommand = null
        labels.clear()
        programStart = 0
        machine = StackMachine()
    }

    private fun parseCommand(command: String): Any {
        return when {
            command == "DATALOC" -> 500
            command.contains(":") -> Command.valueOf(command.substringAfter(":").trim())
            labels.containsKey(command) -> labels[command]!!.toShort()
            else -> command.toShortOrNull() ?: Command.valueOf(command)
        }
    }

    private inline fun parseInstructions(
        instructions: List<String>,
        programStart: Int,
        block: (Int, String) -> Any
    ): List<Any>? = instructions
        .mapIndexed { i, it ->
            try {
                val res = block(i, it)
                if (res !is CompilationError) return@mapIndexed res
                else {
                    compilationError = res
                    isCompiling = false
                    return null
                }
            } catch (ex: Exception) {
                compilationError = CompilationError.LineError(i + programStart + 1)
                logger.log(Level.SEVERE, ex.message)
                isCompiling = false
                return null
            }
        }

    private suspend fun compileAndSet(program: List<String>) = coroutineScope {
        isCompiling = true
        val dataStart = program.indexOf("DATA")
        val programStart = program.indexOf("START")
        if (programStart == -1) {
            isCompiling = false
            compilationError = CompilationError.NoProgramStart
            return@coroutineScope
        }

        try {
            program.subList(dataStart + 1, if (dataStart < programStart) programStart else program.size).flatMap {
                it.split(",").mapNotNull { it.filterNot { it.isWhitespace() }.toUShortOrNull() }
            }.forEachIndexed { index, s ->
                machine.memory[500 + index] = s
            }
        } catch (ex: Exception) {
            isCompiling = false
            compilationError = CompilationError.DataError
            logger.log(Level.SEVERE, ex.message)
            return@coroutineScope
        }

        val instructions = program.subList(programStart + 1, if (programStart < dataStart) dataStart else program.size)
        parseInstructions(instructions, programStart) { i, command ->
            if (command.contains(':')) {
                val (label, _) = command.substringBefore(":") to command.substringAfter(":")
                if (Command.entries.find { it.name == label } != null) {
                    isCompiling = false
                    compilationError = CompilationError.WrongLabelName(i + programStart + 1, label)
                    return@coroutineScope
                }
                labels[label] = i
            }
        } ?: return@coroutineScope
        val commands = parseInstructions(instructions, programStart) { i, it ->
            parseCommand(it.trim())
        } ?: return@coroutineScope

        isCompiling = false
        this@Emulator.programStart = programStart
        machine.setMemory(commands)
    }

    fun compileAndRun(program: List<String>) = scope.launch {
        if (currentCommand == null || !machine.canExecute()) {
            reset()
            compileAndSet(program)
        }
        try {
            machine.executeMemory()
        }
        catch (ex: Exception) {
            compilationError = CompilationError.RuntimeError
        }
        currentCommand = null
    }

    fun step(program: List<String>) = scope.launch {
        currentCommand = if (currentCommand == null || !machine.canExecute()) {
            reset()
            compileAndSet(program)
            machine.instructionPointer.toInt() + programStart + 1
        } else {
            try {
                machine.step()
            } catch (ex: Exception) {
                compilationError = CompilationError.RuntimeError
            }
            machine.instructionPointer.toInt() + programStart + 1
        }
    }
}

sealed class CompilationError {
    data object DataError : CompilationError()
    data object NoProgramStart : CompilationError()
    open class LineError(open val line: Int) : CompilationError()
    data class WrongLabelName(override val line: Int, val label: String) : LineError(line)
    data object RuntimeError : CompilationError()
}