package iptiQ


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

    private var allProviders: Map<String, ProviderWithHealth> = emptyMap()

    override fun addProvider(provider: Provider) {
        inspect(provider, null)
    }

    override fun removeProvider(provider: Provider) {
        allProviders = allProviders.withoutProvider(provider)
        underlyingLoadBalancer.removeProvider(provider)
    }

    fun inspectAll() {
        allProviders.forEach { (_, ph) -> inspect(ph.provider, ph.health) }
    }

    private fun inspect(provider: Provider, lastKnownHealth: ProviderHealth?) {
        val latestHealth = latestHealth(provider, lastKnownHealth)
        if (latestHealth != lastKnownHealth) {
            allProviders = allProviders.withProvider(provider, latestHealth)
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
