package xyz.ruin.gdxtest.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite

class Thing(val instanceData: IThing, val x: Int, val y: Int) {
    val sprite: Sprite = instanceData.image.getFileHandle().let { Sprite(Texture(it)) }
}
