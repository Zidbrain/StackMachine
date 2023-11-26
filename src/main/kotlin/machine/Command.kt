package machine

enum class Command(val memoryRepresentation: UShort) {
    NULL(0x00u),
    /**
     * Next value is appended to stack. IP += 2. (READ 50)
     */
    PUSH(0x01u),

    /**
     * Top of the stack is replaced with value from memory [[top of the stack]]
     */
    READ(0x02u),

    /**
     * Memory[[top of the stack]] = stack[1]. SP -= 2.
     */
    WRITE(0x03u),

    /**
     * Duplicates top of the stack
     */
    DUP(0x04u),

    /**
     * SP--
     */
    DROP(0x05u),

    /**
     * C = top of the stack. SP--
     */
    LDC(0x06u),

    /**
     * Top of the stack = C. SP++
     */
    STC(0x07u),

    /**
     * Compare two top numbers.
     */
    CMP(0x08u),

    /**
     * Top of the stack++
     */
    INC(0x09u),

    /**
     * Top of the stack--
     */
    DEC(0x0Au),

    /**
     * C++
     */
    INCC(0x0Bu),

    /**
     * C--
     */
    DECC(0x0Cu),

    /**
     * Compare C and top of the stack.
     */
    CMPC(0x0Du),

    /**
     * Add two top numbers. Numbers are deleted. Result is appended to the top of the stack.
     */
    ADD(0x0Eu),

    /**
     * Add two top numbers + carry. Numbers are deleted. Result is appended to the top of the stack.
     */
    ADDC(0x0Fu),

    /**
     * Multiply two top numbers. Result is two top numbers.
     */
    MUL(0x10u),

    /**
     * Swap two top numbers
     */
    SWAP(0x11u),

    /**
     * Rotate three numbers right. ROR(1234) -> (3124)
     */
    ROR(0x12u),

    /**
     * Rotate three numbers left. ROL(1234) -> (2314)
     */
    ROL(0x13u),
    JE(0x14u),
    JNE(0x15u),
    JL(0x16u),
    JG(0x17u),
    JGE(0x18u),
    JLE(0x19u),
    JMP(0x1Au);

    companion object {
        private val representations = entries.associateBy { it.memoryRepresentation }

        fun fromMemoryRepresentation(memoryRepresentation: UShort): Command =
            representations[memoryRepresentation]!!
    }
}