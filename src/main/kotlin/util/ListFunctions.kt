package util

operator fun <T> MutableList<T>.set(index: UShort, value: T) {
    set(index.toInt(), value)
}

operator fun <T> List<T>.get(index: UShort): T =
    get(index.toInt())

operator fun <T> MutableList<T>.set(index: Short, value: T) {
    set(index.toInt(), value)
}

operator fun <T> List<T>.get(index: Short): T =
    get(index.toInt())
