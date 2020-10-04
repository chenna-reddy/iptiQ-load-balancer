package iptiQ

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock

enum class ProviderHealth {
    Dead,
    AliveButGettingBetter,
    AliveAndKicking,
}

data class ProviderWithHealth(val provider: Provider, val health: ProviderHealth)

fun Map<String, ProviderWithHealth>.withProvider(provider: Provider, health: ProviderHealth): Map<String, ProviderWithHealth> {
    return plus(Pair(provider.id, ProviderWithHealth(provider, health)))
}

fun Map<String, ProviderWithHealth>.withoutProvider(provider: Provider): Map<String, ProviderWithHealth> {
    return minus(provider.id)
}


class LoadBalancerWithHealthCheck(
        override val id: String,
        private val underlyingLoadBalancer: LoadBalancer
) : LoadBalancer by underlyingLoadBalancer {

    // To prevent health check and manual inclusion/exclusion racing with internal data structures
    private val lock = ReentrantLock()
    // We don't want Load Balancer consumers to go through `lock` above.
    // Same time, internal structures updated should be visible to consumers
    // So Using `AtomicReference` to make sure, what consumers see is latest.
    private var allProviders: AtomicReference<Map<String, ProviderWithHealth>> = AtomicReference(emptyMap())

    override fun addProvider(provider: Provider) = withLock(lock) {
            inspect(provider, null)
    }

    override fun removeProvider(provider: Provider) = withLock(lock) {
        allProviders.set(allProviders.get().withoutProvider(provider))
        underlyingLoadBalancer.removeProvider(provider)
    }

    fun inspectAll() {
        allProviders.get().forEach { (_, ph) -> inspect(ph.provider, ph.health) }
    }

    private fun inspect(provider: Provider, lastKnownHealth: ProviderHealth?) = withLock(lock) {
        val latestHealth = latestHealth(provider, lastKnownHealth)
        if (latestHealth != lastKnownHealth) {
            allProviders.set(allProviders.get().withProvider(provider, latestHealth))
            if (latestHealth == ProviderHealth.AliveAndKicking) {
                underlyingLoadBalancer.addProvider(provider)
            } else {
                underlyingLoadBalancer.removeProvider(provider)
            }
        }
    }

    companion object {

        internal fun latestHealth(provider: Provider, lastKnownHealth: ProviderHealth?): ProviderHealth {
            return if (!isHealthy(provider)) {
                ProviderHealth.Dead
            } else when (lastKnownHealth) {
                ProviderHealth.AliveButGettingBetter ->
                    ProviderHealth.AliveAndKicking
                ProviderHealth.Dead ->
                    ProviderHealth.AliveButGettingBetter
                ProviderHealth.AliveAndKicking ->
                    ProviderHealth.AliveAndKicking
                null ->
                    ProviderHealth.AliveButGettingBetter
            }
        }

        internal fun isHealthy(provider: Provider): Boolean {
            return try {
                provider.check();
            } catch (e: Exception) {
                false
            }
        }

    }
}
