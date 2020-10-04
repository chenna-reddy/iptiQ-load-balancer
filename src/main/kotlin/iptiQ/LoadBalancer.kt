package iptiQ

import java.util.concurrent.locks.ReentrantLock
import kotlin.random.Random

class NoProviders(override val message: String) : RuntimeException(message)

interface ProviderSubscriber {
    fun addProvider(provider: Provider)
    fun removeProvider(provider: Provider)
}

fun List<Provider>.withProvider(provider: Provider): List<Provider> {
    val existing = this.firstOrNull { p -> p.id == provider.id }
    return if (existing == null) {
        plus(provider)
    } else {
        this
    }
}

fun List<Provider>.withoutProvider(provider: Provider): List<Provider> {
    return filter { p -> p.id != provider.id }
}

fun withLock(lock: ReentrantLock, action: () -> Unit) {
    lock.lock()
    try {
        action.invoke()
    } finally {
        lock.unlock()
    }
}

interface LoadBalancingAlgorithm : ProviderSubscriber {
    fun get(): Provider?
}

class RandomLoadBalancingAlgorithm(randomInt: ((Int) -> Int)? = null) : LoadBalancingAlgorithm {
    private val randomInt = randomInt ?: { i -> Random.nextInt(i) }
    private var providers: List<Provider> = emptyList()

    override fun get(): Provider? {
        println("Has ${providers.size}")
        val providersSnapshot = providers
        return if (providersSnapshot.isEmpty()) null else providersSnapshot.getOrNull(randomInt(providersSnapshot.size))
    }

    override fun addProvider(provider: Provider) {
        providers = providers.withProvider(provider)
    }

    override fun removeProvider(provider: Provider) {
        providers = providers.withoutProvider(provider)
    }
}

class RoundRobinLoadBalancingAlgorithm : LoadBalancingAlgorithm {
    private var providers: List<Provider> = emptyList()
    private var nextProviderIndex: Int = 0

    override fun get(): Provider? {
        val providersSnapshot = providers
        if (providersSnapshot.isEmpty()) return null
        val provider = providersSnapshot.getOrNull(nextProviderIndex % providersSnapshot.size)
        nextProviderIndex = (nextProviderIndex + 1) % providersSnapshot.size
        return provider
    }

    override fun addProvider(provider: Provider) {
        providers = providers.withProvider(provider)
    }

    override fun removeProvider(provider: Provider) {
        providers = providers.withoutProvider(provider)
    }
}


interface LoadBalancer : Provider, ProviderSubscriber {
    fun providerCount(): Int
}

class SimpleLoadBalancer(override val id: String, private val algorithm: LoadBalancingAlgorithm) : LoadBalancer {

    private var activeProviderIds: Set<String> = HashSet()

    override fun providerCount(): Int {
        return activeProviderIds.size
    }

    override fun get(): String {
        return algorithm.get()?.get() ?: throw NoProviders("No Service Provider");
    }

    override fun check(): Boolean {
        return activeProviderIds.isNotEmpty()
    }

    override fun addProvider(provider: Provider) {
        activeProviderIds = activeProviderIds.plus(provider.id)
        algorithm.addProvider(provider)
    }

    override fun removeProvider(provider: Provider) {
        activeProviderIds = activeProviderIds.minus(provider.id)
        algorithm.removeProvider(provider)
    }

}
