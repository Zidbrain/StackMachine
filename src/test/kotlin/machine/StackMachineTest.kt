package machine

import machine.Command.*
import util.get
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StackMachineTest {
    private lateinit var machine: StackMachine

    @BeforeTest
    fun setup() {
        machine = StackMachine()
    }

    private fun test(
        vararg commands: Any,
        setup: StackMachine.() -> Unit = {},
        assert: StackMachine.() -> Unit
    ) = with(machine) {
        setMemory(commands.toList())
        setup()
        executeMemory()
        assert()
    }

    @Test
    fun push() = test(PUSH, 50) {
        assertEquals(50u, stack[stackPointer])
    }

    @Test
    fun read() = test(PUSH, 50, READ,
        setup = {
            memory[50] = 23.s()
        }
    ) {
        assertEquals(23u, peek())
    }

    @Test
    fun write() = test(
        PUSH,
        50,
        PUSH,
        20,
        WRITE
    ) {
        assertEquals(50u, memory[20])
    }

    @Test
    fun dup() = test(PUSH, 50, DUP) {
        assertEquals(50u, peek())
        assertEquals(50u, stack[stackPointer.toInt() - 1])
    }

    @Test
    fun drop() = test(PUSH, 50, DROP) {
        assertEquals(UShort.MAX_VALUE, stackPointer)
    }

    @Test
    fun ldc() = test(PUSH, 50, LDC) {
        assertEquals(50u, counter)
    }

    @Test
    fun stc() = test(PUSH, 50, LDC, PUSH, 100, STC) {
        assertEquals(50u, peek())
    }

    @Test
    fun cmp() {
        test(PUSH, 50, PUSH, 100, CMP) {
            assertEquals(FlagsRegister(equal = false, less = false, greater = true), flags)
        }
        machine = StackMachine()
        test(PUSH, 100, PUSH, 50, CMP) {
            assertEquals(FlagsRegister(equal = false, less = true, greater = false), flags)
        }
        machine = StackMachine()
        test(PUSH, 100, PUSH, 100, CMP) {
            assertEquals(FlagsRegister(equal = true, less = false, greater = false), flags)
        }
    }

    @Test
    fun inc() = test(PUSH, 50, INC) {
        assertEquals(51u, peek())
    }

    @Test
    fun dec() = test(PUSH, 50, DEC) {
        assertEquals(49u, peek())
    }

    @Test
    fun incc() = test(PUSH, 50, LDC, INCC) {
        assertEquals(51u, counter)
    }

    @Test
    fun decc() = test(PUSH, 50, LDC, DECC) {
        assertEquals(49u, counter)
    }

    @Test
    fun cmpc() {
        test(PUSH, 50, LDC, PUSH, 100, CMPC) {
            assertEquals(FlagsRegister(equal = false, less = true, greater = false), flags)
        }
        machine = StackMachine()
        test(PUSH, 100, LDC, PUSH, 50, CMPC) {
            assertEquals(FlagsRegister(equal = false, less = false, greater = true), flags)
        }
        machine = StackMachine()
        test(PUSH, 100, LDC, PUSH, 100, CMPC) {
            assertEquals(FlagsRegister(equal = true, less = false, greater = false), flags)
        }
    }

    @Test
    fun add() {
        test(PUSH, 3, PUSH, 3, ADD) {
            assertEquals(6u, peek())
            assertEquals(FlagsRegister(), flags)
        }
        machine = StackMachine()
        test(PUSH, UShort.MAX_VALUE, PUSH, 3, ADD) {
            assertEquals(UShort.MIN_VALUE + 2u, peek().toUInt())
            assertEquals(FlagsRegister(carry = true), flags)
        }
    }

    @Test
    fun addc() = test(PUSH, UShort.MAX_VALUE, PUSH, 3, ADD, PUSH, 3, ADDC) {
        assertEquals(UShort.MIN_VALUE + 6u, peek().toUInt())
        assertEquals(FlagsRegister(carry = false), flags)
    }

    @Test
    fun mul() {
        test(PUSH, 10, PUSH, 10, MUL) {
            assertEquals(0u, peek(1u))
            assertEquals(100u, peek())
            assertEquals(FlagsRegister(), flags)
        }
        machine = StackMachine()
        test(PUSH, Short.MAX_VALUE, PUSH, 4, MUL) {
            assertEquals(1u, peek(1u))
            assertEquals(0xFFFC.toUShort(), peek())
            assertEquals(FlagsRegister(carry = true), flags)
        }
    }

    @Test
    fun swap() = test(PUSH, 10, PUSH, 20, SWAP) {
        assertEquals(10u, peek())
        assertEquals(20u, peek(1u))
    }

    @Test
    fun ror() = test(PUSH, 4, PUSH, 3, PUSH, 2, PUSH, 1, ROR) {
        val (d, c, b, a) = stack
        assertEquals(3u, a)
        assertEquals(1u, b)
        assertEquals(2u, c)
        assertEquals(4u, d)
    }

    @Test
    fun rol() = test(PUSH, 4, PUSH, 3, PUSH, 2, PUSH, 1, ROL) {
        val (d, c, b, a) = stack
        assertEquals(2u, a)
        assertEquals(3u, b)
        assertEquals(1u, c)
        assertEquals(4u, d)
    }

    @Test
    fun jmp() = test(JMP, 6, 0, 0, 0, 0, PUSH, 100) {
        assertEquals(100u, peek())
    }

    /**
     * stack = MIN_VAL
     * start = memory[500] + 500
     * check:
     *  if (memory[start] > stack):
     *      stack = memory[start]
     *  start--
     *  if (start != 500):
     *      jmp check
     */
    @Test
    fun findMaximum() = test(

        PUSH, UShort.MIN_VALUE,
        PUSH, 500, PUSH, 500, READ, ADD, LDC,
        STC, READ, CMP, DROP, JLE, 18,
        DROP, STC, READ,
        DECC,
        STC, PUSH, 500, CMP, DROP, DROP,
        JNE, 9,

        setup = {
            listOf(8, 10, 3, 5, 23, 2, 15, 32, 11).forEachIndexed { index, i ->
                memory[500 + index] = i.toUShort()
            }
        }) {
        assertEquals(32u, peek())
    }

}