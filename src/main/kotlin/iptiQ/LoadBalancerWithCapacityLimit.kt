package iptiQ

import java.util.concurrent.atomic.AtomicInteger

class NotEnoughProviders(override val message: String) : RuntimeException(message)

class LoadBalancerWithCapacityLimit(
        override val id: String,
        private val underlyingLoadBalancer: LoadBalancer,
        private val providerLimit: Int
) : LoadBalancer by underlyingLoadBalancer {

    private val activeRequestCount: AtomicInteger = AtomicInteger(0)

    override fun get(): String {
        try {
            val activeRequestCount = activeRequestCount.incrementAndGet()
            val maxAllowed = providerLimit * providerCount()
            if (activeRequestCount > maxAllowed) {
                throw NotEnoughProviders("Can't handle $activeRequestCount request(s) at once. Max Allowed: $maxAllowed")
            } else {
                return underlyingLoadBalancer.get()
            }
        } finally {
            activeRequestCount.decrementAndGet()
        }
    }

}
