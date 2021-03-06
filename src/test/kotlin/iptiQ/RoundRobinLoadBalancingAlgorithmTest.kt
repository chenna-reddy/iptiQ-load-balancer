package iptiQ

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class RoundRobinLoadBalancingAlgorithmTest {

    private object Provider1: Provider {
        override val id: String = "p1"
        override fun check(): Boolean = true
    }

    private object Provider2: Provider {
        override val id: String = "p2"
        override fun check(): Boolean = true
    }

    @Test
    fun `it should return no provider if no there are no providers`() {
        val classUnderTest = RoundRobinLoadBalancingAlgorithm()
        assertNull(classUnderTest.get())
    }

    @Test
    fun `it should return same provider always with only one provider`() {
        val classUnderTest = RoundRobinLoadBalancingAlgorithm()
        classUnderTest.addProvider(Provider1)
        assertSame(classUnderTest.get(), Provider1)
        assertSame(classUnderTest.get(), Provider1)
    }

    @Test
    fun `it should return providers in round robin fashion`() {
        val classUnderTest = RoundRobinLoadBalancingAlgorithm()
        classUnderTest.addProvider(Provider1)
        classUnderTest.addProvider(Provider2)
        assertSame(classUnderTest.get(), Provider1)
        assertSame(classUnderTest.get(), Provider2)
        assertSame(classUnderTest.get(), Provider1)
        assertSame(classUnderTest.get(), Provider2)
    }

    @Test
    fun testRemoveProvider() {
        val classUnderTest = RoundRobinLoadBalancingAlgorithm()
        classUnderTest.removeProvider(Provider1)
        classUnderTest.addProvider(Provider1)
        classUnderTest.removeProvider(Provider1)
        assertNull(classUnderTest.get())
        classUnderTest.addProvider(Provider1)
        classUnderTest.addProvider(Provider2)
        classUnderTest.removeProvider(Provider1)
        assertEquals(classUnderTest.get(), Provider2)
    }

}
