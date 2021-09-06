package xyz.ruin.gdxtest.launcher

import org.spongepowered.asm.service.IGlobalPropertyService
import org.spongepowered.asm.service.IPropertyKey


/**
 * Global property service backed by LaunchWrapper blackboard
 */
class Blackboard : IGlobalPropertyService {
    /**
     * Property key
     */
    internal inner class Key(private val key: String) : IPropertyKey {
        override fun toString(): String {
            return key
        }
    }

    override fun resolveKey(name: String): IPropertyKey {
        return Key(name)
    }

    /**
     * Get a value from the blackboard and duck-type it to the specified type
     *
     * @param key blackboard key
     * @return value
     * @param <T> duck type
    </T> */
    override fun <T> getProperty(key: IPropertyKey): T {
        return Launch.blackboard[key.toString()] as T
    }

    /**
     * Put the specified value onto the blackboard
     *
     * @param key blackboard key
     * @param value new value
     */
    override fun setProperty(key: IPropertyKey, value: Any) {
        Launch.blackboard[key.toString()] = value
    }

    /**
     * Get the value from the blackboard but return <tt>defaultValue</tt> if the
     * specified key is not set.
     *
     * @param key blackboard key
     * @param defaultValue value to return if the key is not set or is null
     * @return value from blackboard or default value
     * @param <T> duck type
    </T> */
    override fun <T> getProperty(key: IPropertyKey, defaultValue: T): T {
        val value = Launch.blackboard[key.toString()]
        return if (value != null) value as T else defaultValue
    }

    /**
     * Get a string from the blackboard, returns default value if not set or
     * null.
     *
     * @param key blackboard key
     * @param defaultValue default value to return if the specified key is not
     * set or is null
     * @return value from blackboard or default
     */
    override fun getPropertyString(key: IPropertyKey, defaultValue: String): String {
        val value = Launch.blackboard[key.toString()]
        return value?.toString() ?: defaultValue
    }

    init {
        Launch.classLoader.hashCode()
    }
}