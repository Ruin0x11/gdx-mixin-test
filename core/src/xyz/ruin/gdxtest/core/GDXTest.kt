package xyz.ruin.gdxtest.core

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.SpriteBatch

class GDXTest : Game() {
    override fun create() {
        setScreen(TiledMapScreen(this))
    }

    override fun render() {
        super.render()
    }
}