package settingdust.calypsos_mobs

class WeightedMap<K, V : Number>(val original: Map<K, V>) {
    private val totalWeight: Double by lazy {
        original.values.sumOf { it.toDouble() }
    }

    fun randomByWeight(): K {
        var randomValue = Math.random() * totalWeight
        for ((key, weight) in original) {
            randomValue -= weight.toDouble()
            if (randomValue <= 0) {
                return key
            }
        }
        throw IllegalStateException("No item selected by weighted random")
    }
}