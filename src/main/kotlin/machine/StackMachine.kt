package machine

import androidx.compose.runtime.*
import util.get
import util.set

@Stable
class StackMachine {
    var counter: UShort by mutableStateOf(0u)
        private set

    var stackPointer: UShort by mutableStateOf(UShort.MAX_VALUE)
        private set
    val stack = MutableList(STACK_SIZE) { 0.toUShort() }.toMutableStateList()

    var instructionPointer: UShort by mutableStateOf(0u)
        private set
    var instructionRegister: UShort by mutableStateOf(0u)
        private set

    val memory = MutableList(MEMORY_SIZE) { 0.toUShort() }.toMutableStateList()

    var flags by mutableStateOf(FlagsRegister())
        private set

    private fun stackPush(value: UShort) {
        stackPointer++
        stack[stackPointer] = value
    }

    private fun stackPop(): UShort = stack[stackPointer].also {
        stackPointer--
    }

    private inline fun command(block: () -> Unit) {
        instructionPointer++
        block()
    }

    fun peek(fromTop: UShort = 0u): UShort = stack[(stackPointer - fromTop).toUShort()]

    fun setMemory(data: List<Any>) {
        data.forEachIndexed { index, value ->
            when (value) {
                is Command -> memory[index] = value.memoryRepresentation
                is Number -> memory[index] = value.toShort().toUShort()
                is UShort -> memory[index] = value
                else -> throw Exception()
            }
        }
        instructionRegister = memory[instructionPointer]
    }

    private fun push() = command {
        stackPush(memory[instructionPointer])
        instructionPointer++
    }

    private fun read() = command {
        val address = stackPop()
        stackPush(memory[address])
    }

    private fun write() = command {
        val destination = stackPop()
        val value = stackPop()
        memory[destination] = value
    }

    private fun duplicate() = command {
        stackPush(peek())
    }

    private fun drop() = command {
        stackPop()
    }

    private fun loadCounter() = command {
        counter = stackPop()
    }

    private fun storeCounter() = command {
        stackPush(counter)
    }

    private fun compare(a: UShort, b: UShort) = command {
        val equal = a == b
        val greater = a > b
        val less = a < b
        flags = flags.copy(equal = equal, less = less, greater = greater)
    }

    private fun increment() = command {
        stack[stackPointer]++
    }

    private fun decrement() = command {
        stack[stackPointer]--
    }

    private fun incrementCounter() = command {
        counter++
    }

    private fun decrementCounter() = command {
        counter--
    }

    private fun add() = command {
        val a = stackPop()
        val b = stackPop()

        val result = a + b
        val carry = result > UShort.MAX_VALUE.toUInt() || result < UShort.MIN_VALUE.toUInt()
        flags = flags.copy(carry = carry)
        stackPush(result.toUShort())
    }

    private fun addWithCarry() = command {
        val a = stackPop()
        val b = stackPop()

        val result = a + b + if (flags.carry) 1u else 0u
        val carry = result > UShort.MAX_VALUE.toUInt() || result < UShort.MIN_VALUE.toUInt()
        flags = flags.copy(carry = carry)
        stackPush(result.toUShort())
    }

    private fun multiply() = command {
        val a = stackPop()
        val b = stackPop()

        val result = a * b
        val top = (0xFFFF0000 and result.toLong() shr 16).toUShort()
        val carry = top.toInt() != 0
        flags = flags.copy(carry = carry)
        stackPush(top)
        stackPush((0x0000FFFFu and result).toUShort())
    }

    private fun swap() = command {
        val a = peek()
        stack[stackPointer] = peek(1u)
        stack[(stackPointer - 1u).toInt()] = a
    }

    private fun rotateRight() = command {
        val a = stackPop()
        val b = stackPop()
        val c = stackPop()

        stackPush(b)
        stackPush(a)
        stackPush(c)
    }

    private fun rotateLeft() = command {
        val a = stackPop()
        val b = stackPop()
        val c = stackPop()

        stackPush(a)
        stackPush(c)
        stackPush(b)
    }

    private fun jump(condition: Boolean) = command {
        if (!condition) {
            instructionPointer++
            return
        }

        val address = memory[instructionPointer]
        instructionPointer = address.toUShort()
    }

    private fun executeCommand(command: Command) {
        when (command) {
            Command.NULL -> {}
            Command.PUSH -> push()
            Command.READ -> read()
            Command.WRITE -> write()
            Command.DUP -> duplicate()
            Command.DROP -> drop()
            Command.LDC -> loadCounter()
            Command.STC -> storeCounter()
            Command.CMP -> compare(peek(), peek(1u))
            Command.INC -> increment()
            Command.DEC -> decrement()
            Command.INCC -> incrementCounter()
            Command.DECC -> decrementCounter()
            Command.CMPC -> compare(counter, peek())
            Command.ADD -> add()
            Command.ADDC -> addWithCarry()
            Command.MUL -> multiply()
            Command.SWAP -> swap()
            Command.ROR -> rotateRight()
            Command.ROL -> rotateLeft()
            Command.JMP -> jump(true)
            Command.JE -> jump(flags.equal)
            Command.JL -> jump(flags.less)
            Command.JG -> jump(flags.greater)
            Command.JGE -> jump(flags.greater || flags.equal)
            Command.JLE -> jump(flags.less || flags.equal)
            Command.JNE -> jump(!flags.equal)
        }
    }

    fun executeMemory() {
        while (canExecute()) {
            step()
        }
    }

    fun canExecute(): Boolean = memory[instructionPointer] != 0.s()

    fun step() {
        executeCommand(memory[instructionPointer].toCommand())
        instructionRegister = memory[instructionPointer]
    }

    companion object {
        private const val STACK_SIZE = 1024
        private const val MEMORY_SIZE = 1024 * 4
    }
}

data class FlagsRegister(
    val equal: Boolean = false,
    val less: Boolean = false,
    val greater: Boolean = false,
    val carry: Boolean = false
)

fun Int.s(): UShort = toUShort()

fun UShort.toCommand(): Command =
    Command.fromMemoryRepresentation(this)

