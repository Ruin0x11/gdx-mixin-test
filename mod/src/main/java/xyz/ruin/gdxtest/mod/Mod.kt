package xyz.ruin.gdxtest.mod

import xyz.ruin.gdxtest.core.IMod
import xyz.ruin.gdxtest.core.ModEntry
import xyz.ruin.gdxtest.core.ThingStorage

@ModEntry(id = Mod.MOD_ID)
class Mod : IMod {
    override val name: String = MOD_ID

    override fun onInit() {
        ThingStorage.INSTANCE.register(PutitThing())
    }

    companion object {
        const val MOD_ID = "test_mod"
    }
}