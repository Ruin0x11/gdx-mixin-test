package xyz.ruin.gdxtest.mod2

import xyz.ruin.gdxtest.core.IMod
import xyz.ruin.gdxtest.core.ModEntry
import xyz.ruin.gdxtest.core.ThingStorage

@ModEntry(id = Mod2.MOD_ID)
class Mod2 : IMod {
    override val name: String = MOD_ID

    override fun onInit() {
        ThingStorage.INSTANCE.register(Putit2Thing())
    }

    companion object {
        const val MOD_ID = "test_mod2"
    }
}