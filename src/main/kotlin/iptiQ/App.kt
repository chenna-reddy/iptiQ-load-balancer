package iptiQ

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.time.ExperimentalTime

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

fun consume(loadBalancer: LoadBalancer) {
    val thread = Thread( {
        val response = try {
            "Response: ${loadBalancer.get()}"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
        println(response)
    })
    thread.isDaemon = true
    thread.start()
}

@ExperimentalTime
suspend fun main(args: Array<String>) {
    val loadBalancer = loadBalancer("LB1", RoundRobinLoadBalancingAlgorithm())
            .withCapacityLimit(2)
            .withHealthCheck()

    Executors.newSingleThreadScheduledExecutor()
            .scheduleWithFixedDelay({
                println("Triggering Health check")
                loadBalancer.inspectAll()
            }, 2, 5, TimeUnit.SECONDS)

    println("Started Load Balancer")

    loadBalancer.addProvider(HealthyProvider1)
    loadBalancer.addProvider(HealthyProvider2)
    loadBalancer.addProvider(UnHealthyProvider)
    loadBalancer.addProvider(DanglingProvider)

    repeat(1000) {
        Thread.sleep(250)
        consume(loadBalancer)
    }

}
