package iptiQ

fun loadBalancer(id: String, loadBalancingAlgorithm: LoadBalancingAlgorithm): LoadBalancer {
    return SimpleLoadBalancer(id, loadBalancingAlgorithm)
}

fun LoadBalancer.withCapacityLimit(limit: Int): LoadBalancerWithCapacityLimit {
    return LoadBalancerWithCapacityLimit(id, this, limit)
}

fun LoadBalancer.withHealthCheck(): LoadBalancerWithHealthCheck {
    return LoadBalancerWithHealthCheck(id, this)
}
