package iptiQ

import kotlinx.coroutines.*
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

object HealthyProvider1 : Provider {
    override val id: String = "Healthy1"
    override fun check(): Boolean = true
    override fun get(): String {
        Thread.sleep(2000)
        return id;
    }
}

object HealthyProvider2 : Provider {
    override val id: String = "Healthy2"
    override fun check(): Boolean = true
    override fun get(): String {
        Thread.sleep(2000)
        return id;
    }
}

object UnHealthyProvider : Provider {
    override val id: String = "UnHealthy"
    override fun check(): Boolean = false
}


object DanglingProvider : Provider {
    override val id: String = "Dangling"
    override fun check(): Boolean = Random.nextInt(3) > 0
    override fun get(): String {
        Thread.sleep(2000)
        return id;
    }
}


@ExperimentalTime
suspend fun main(args: Array<String>) = runBlocking {
    val loadBalancer = loadBalancer("LB1", RoundRobinLoadBalancingAlgorithm())
            .withCapacityLimit(2)
            .withHealthCheck()

    val healthCheckTask = launch(Dispatchers.Default) {
        for (i in 0 until 1000) {
            println("Triggering Health check. $i")
            delay(10.seconds)
            launch {
                withTimeout(10.seconds) {
                    loadBalancer.inspectAll()
                }
            }
        }
    }
    println("Started Load Balancer")

    loadBalancer.addProvider(HealthyProvider1)
    loadBalancer.addProvider(HealthyProvider2)
    loadBalancer.addProvider(UnHealthyProvider)
    loadBalancer.addProvider(DanglingProvider)

    for (i in 0 until 1000) {
        delay(300.milliseconds)
        GlobalScope.launch {
            val response = try {
                "Response($i): ${loadBalancer.get()}"
            } catch (e: Exception) {
                "Error($i): ${e.message}"
            }
            println(response)
        }
    }
    // healthCheckTask.join()
}
