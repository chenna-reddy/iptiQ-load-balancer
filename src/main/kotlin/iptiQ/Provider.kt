package iptiQ

interface Provider {
    val id: String
    fun get(): String = id
    fun check(): Boolean
}
