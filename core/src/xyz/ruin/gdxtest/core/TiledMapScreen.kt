package xyz.ruin.gdxtest.core

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.FillViewport
import com.badlogic.gdx.utils.viewport.Viewport


open class TiledMapScreen(private val game: Game) : ScreenAdapter() {
    private val viewport: Viewport
    private val camera: OrthographicCamera = OrthographicCamera()
    private val cameraSprite: OrthographicCamera
    private val map: InstancedMap get() = renderer.map
    private var renderer: InstancedMapRenderer
    private var batch: SpriteBatch = SpriteBatch()

    private val things: MutableList<Thing> = mutableListOf()

    init {
        renderer = regenerateMap()
        viewport = FillViewport(map.mapWidth.toFloat(), map.mapHeight.toFloat(), camera)
        cameraSprite = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.width.toFloat())
        cameraSprite.position.set(0f, 0f, 0f)
        cameraSprite.update()
        println("Got back: " + testPrintThing("Test"))
    }

    override fun show() {
        things.clear()

        ThingStorage.INSTANCE.things.forEachIndexed { index, data ->
            things.add(Thing(data, index * 48 + 10, 10))
        }
    }

    private fun regenerateMap() : InstancedMapRenderer {
        val conf = TilesetConfig().also {
            it.tileWidth = 32
            it.tileHeight = 32
            it.texturePath = "RPGTiles.png"
            it.terrainDefs = Array.with(Array.with("sand", "water"), Array.with("sand", "grass"))
        }
        val autoTiler = AutoTiler(64, 64, conf)
        val map = autoTiler.generateInstancedMap()
        return InstancedMapRenderer(map)
    }

    private fun testPrintThing(dood: String): String {
        println("Hi! I'm dood: $dood")
        return "$dood?"
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        viewport.update(width, height)
    }

    private fun update(delta: Float) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            renderer = regenerateMap()
        }

        val moveDelta = 10f * delta
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            camera.position.y += moveDelta
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            camera.position.y -= moveDelta
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            camera.position.x -= moveDelta
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            camera.position.x += moveDelta
        }
        camera.update()
    }

    private fun draw() {
        ScreenUtils.clear(0.0f, 0.0f, 0.1f, 1f)
        renderer.setView(camera)
        renderer.render()

        batch.projectionMatrix = cameraSprite.combined
        batch.begin()
        things.forEach {
            batch.draw(it.sprite, it.x.toFloat(), it.y.toFloat())
        }
        batch.end()
    }

    override fun render(delta: Float) {
        update(delta)
        draw()
    }

    override fun dispose() {
        renderer.dispose()
    }
}