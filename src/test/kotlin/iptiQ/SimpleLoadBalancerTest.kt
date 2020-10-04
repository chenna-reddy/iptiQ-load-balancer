package iptiQ

import io.mockk.*
import kotlin.test.*

class SimpleLoadBalancerTest {

    private object Provider1 : Provider {
        override val id: String = "p1"
        override fun check(): Boolean = true
    }

    private fun loadBalancingAlgorithm(provider: Provider?): LoadBalancingAlgorithm {
        return object : LoadBalancingAlgorithm {
            override fun get(): Provider? = provider
            override fun addProvider(provider: Provider) {}
            override fun removeProvider(provider: Provider) {}
        }
    }

    @Test
    fun `providerCount() return 0 with no providers`() {
        val loadBalancer = SimpleLoadBalancer("SLB0", loadBalancingAlgorithm(null))
        assertEquals(0, loadBalancer.providerCount())
    }

    @Test
    fun `providerCount() return number of providers`() {
        val loadBalancer = SimpleLoadBalancer("SLB0", loadBalancingAlgorithm(null))
        loadBalancer.addProvider(Provider1)
        assertEquals(1, loadBalancer.providerCount())
        loadBalancer.addProvider(Provider1)
        assertEquals(1, loadBalancer.providerCount())
    }

    @Test
    fun `get() throws NoProviders if there are no providers`() {
        val loadBalancerWithZeroProviders = SimpleLoadBalancer("SLB0", loadBalancingAlgorithm(null))
        assertFailsWith<NoProviders> {
            loadBalancerWithZeroProviders.get()
        }
    }

    @Test
    fun `get() return value returned by provider from algorithm`() {
        val loadBalancerWithProvider = SimpleLoadBalancer("SLB0", loadBalancingAlgorithm(Provider1))
        assertSame("p1", loadBalancerWithProvider.get())
    }

    @Test
    fun `check() should fail with no providers`() {
        val loadBalancer = SimpleLoadBalancer("SLB0", loadBalancingAlgorithm(null))
        assertFalse(loadBalancer.check())
    }

    @Test
    fun `check() should succeed with one provider`() {
        val loadBalancer = SimpleLoadBalancer("SLB0", loadBalancingAlgorithm(Provider1))
        loadBalancer.addProvider(Provider1)
        assertTrue(loadBalancer.check())
    }

    @Test
    fun `addProvider() should add provider to underlying algorithm`() {
        val algorithm = mockk<LoadBalancingAlgorithm>()
        every { algorithm.addProvider(Provider1) } just Runs

        val loadBalancer = SimpleLoadBalancer("SLB0", algorithm)
        loadBalancer.addProvider(Provider1)

        verify {
            algorithm.addProvider(Provider1)
        }
    }


    @Test
    fun `removeProvider() should remove provider from underlying algorithm`() {
        val algorithm = mockk<LoadBalancingAlgorithm>()
        every { algorithm.removeProvider(Provider1) } just Runs

        val loadBalancer = SimpleLoadBalancer("SLB0", algorithm)
        loadBalancer.removeProvider(Provider1)

        verify {
            algorithm.removeProvider(Provider1)
        }
    }
}
