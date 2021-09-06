package xyz.ruin.gdxtest

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import kotlin.math.max

class InstancedMapRenderer(val map: InstancedMap) {
    private val unitScale = 1f / max(map.tileWidth, map.tileHeight)
    private val renderer = OrthogonalTiledMapRenderer(map.map, unitScale)

    fun setView(camera: OrthographicCamera) {
        renderer.setView(camera)
    }

    fun render() {
        renderer.render()
    }

    fun dispose() {
        renderer.dispose()
    }
}