package iptiQ

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoadBalancerWithHealthCheckTest {

    private object HealthyProvider : Provider {
        override val id: String = "H1"
        override fun check(): Boolean = true
    }

    private object UnHealthyProvider : Provider {
        override val id: String = "UH1"
        override fun check(): Boolean = false
    }

    private object DeadHealthyProvider : Provider {
        override val id: String = "UH1"
        override fun check(): Boolean = throw RuntimeException("I'm Dead")
    }

    @Test
    fun `addProvider() should not add provider to underlying load balancer immediately`() {
        val underlying = mockk<LoadBalancer>()
        every { underlying.removeProvider(HealthyProvider) } just Runs

        val loadBalancerUnderTest = LoadBalancerWithHealthCheck("LB1", underlying)
        loadBalancerUnderTest.addProvider(HealthyProvider)
        verify(exactly = 0) {
            underlying.addProvider(HealthyProvider)
        }
    }

    @Test
    fun `addProvider() should add provider to underlying load balancer after 1 successful health check`() {
        val underlying = mockk<LoadBalancer>()
        every { underlying.removeProvider(HealthyProvider) } just Runs
        every { underlying.addProvider(HealthyProvider) } just Runs

        val loadBalancerUnderTest = LoadBalancerWithHealthCheck("LB1", underlying)
        loadBalancerUnderTest.addProvider(HealthyProvider)
        loadBalancerUnderTest.inspectAll()
        verify(exactly = 1) {
            underlying.addProvider(HealthyProvider)
        }
    }

    @Test
    fun `removeProvider() should remove provider from underlying load balancer`() {
        val underlying = mockk<LoadBalancer>()
        every { underlying.removeProvider(HealthyProvider) } just Runs

        val loadBalancerUnderTest = LoadBalancerWithHealthCheck("LB1", underlying)
        loadBalancerUnderTest.removeProvider(HealthyProvider)
        verify(exactly = 1) {
            underlying.removeProvider(HealthyProvider)
        }
    }

    @Test
    fun `inspect() should remove provider from underlying load balancer if health check fails`() {
        val provider = mockk<Provider>()
        every { provider.id } returns "P1"
        every { provider.check() } returnsMany listOf(true, true, false)

        val underlying = mockk<LoadBalancer>()
        every { underlying.removeProvider(provider) } just Runs
        every { underlying.addProvider(provider) } just Runs

        val loadBalancerUnderTest = LoadBalancerWithHealthCheck("LB1", underlying)
        loadBalancerUnderTest.addProvider(provider)
        verify(exactly = 1) {
            underlying.removeProvider(provider)
        }
        loadBalancerUnderTest.inspectAll()
        verify(exactly = 1) {
            underlying.addProvider(provider)
        }

        loadBalancerUnderTest.inspectAll()
        verify(exactly = 2) {
            underlying.removeProvider(provider)
        }
    }

    @Test
    fun testIsHealthy() {
        assertTrue(LoadBalancerWithHealthCheck.isHealthy(HealthyProvider))
        assertFalse(LoadBalancerWithHealthCheck.isHealthy(UnHealthyProvider))
        assertFalse(LoadBalancerWithHealthCheck.isHealthy(DeadHealthyProvider))
    }


    @Test
    fun testLatestHealth() {
        assertEquals(ProviderHealth.AliveButGettingBetter, LoadBalancerWithHealthCheck.latestHealth(HealthyProvider, null))
        assertEquals(ProviderHealth.AliveButGettingBetter, LoadBalancerWithHealthCheck.latestHealth(HealthyProvider, ProviderHealth.Dead))
        assertEquals(ProviderHealth.AliveAndKicking, LoadBalancerWithHealthCheck.latestHealth(HealthyProvider, ProviderHealth.AliveButGettingBetter))
        assertEquals(ProviderHealth.AliveAndKicking, LoadBalancerWithHealthCheck.latestHealth(HealthyProvider, ProviderHealth.AliveAndKicking))

        assertEquals(ProviderHealth.Dead, LoadBalancerWithHealthCheck.latestHealth(UnHealthyProvider, null))
        assertEquals(ProviderHealth.Dead, LoadBalancerWithHealthCheck.latestHealth(UnHealthyProvider, ProviderHealth.Dead))
        assertEquals(ProviderHealth.Dead, LoadBalancerWithHealthCheck.latestHealth(UnHealthyProvider, ProviderHealth.AliveButGettingBetter))
        assertEquals(ProviderHealth.Dead, LoadBalancerWithHealthCheck.latestHealth(UnHealthyProvider, ProviderHealth.AliveAndKicking))

        assertEquals(ProviderHealth.Dead, LoadBalancerWithHealthCheck.latestHealth(DeadHealthyProvider, null))
        assertEquals(ProviderHealth.Dead, LoadBalancerWithHealthCheck.latestHealth(DeadHealthyProvider, ProviderHealth.Dead))
        assertEquals(ProviderHealth.Dead, LoadBalancerWithHealthCheck.latestHealth(DeadHealthyProvider, ProviderHealth.AliveButGettingBetter))
        assertEquals(ProviderHealth.Dead, LoadBalancerWithHealthCheck.latestHealth(DeadHealthyProvider, ProviderHealth.AliveAndKicking))

    }
}
