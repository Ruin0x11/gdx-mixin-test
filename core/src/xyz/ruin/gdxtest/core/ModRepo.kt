package xyz.ruin.gdxtest.core

class ModRepo(val mods: MutableMap<String, InstancedMod>) {
    fun initMods() {
        mods.forEach { (modId, mod) ->
            mod.impl.onInit()
        }
    }

    fun getMod(id: String): InstancedMod? {
        return mods[id]
    }

    fun<T: IMod> getMod(mod: Class<T>): InstancedMod? {
        return getMod(mod.getAnnotation(ModEntry::class.java)!!.id)
    }
}