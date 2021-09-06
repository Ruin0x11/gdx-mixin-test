package xyz.ruin.gdxtest.core

class ThingStorage {
    companion object {
        val INSTANCE: ThingStorage = ThingStorage()
    }

    val things: MutableList<IThing> = mutableListOf()

    fun register(clazz: IThing) {
        things.add(clazz)
    }
}