package iptiQ

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class LoadBalancerWithCapacityLimitTest {

    @Test
    fun `get() should allow request if there are sufficient providers`() {
        val underlying = mockk<LoadBalancer>()
        every { underlying.providerCount() } returns 4
        every { underlying.get() } answers {
            Thread.sleep(2000L)
            "ULB1"
        }
        val loadBalancerUnderTest = LoadBalancerWithCapacityLimit("LB1", underlying, 4)
        repeat(16) {
            GlobalScope.launch { assertEquals("ULB1", loadBalancerUnderTest.get()) }
        }
    }

    @Test
    fun `get() should throw exception if there are no sufficient providers`() {
        val lock: Lock = ReentrantLock()
        val ready: Condition = lock.newCondition()

        val underlying = mockk<LoadBalancer>()
        every { underlying.providerCount() } returns 1
        every { underlying.get() } answers {
            lock.lock()
            ready.signal()
            lock.unlock()
            Thread.sleep(2000L)
            "ULB1"
        }
        val loadBalancerUnderTest = LoadBalancerWithCapacityLimit("LB1", underlying, 1)
        GlobalScope.launch { assertEquals("ULB1", loadBalancerUnderTest.get()) }
        lock.lock()
        ready.await()
        assertFailsWith<NotEnoughProviders> {
            assertEquals("ULB1", loadBalancerUnderTest.get())
        }
        lock.unlock()
    }


}
