package xyz.ruin.gdxtest

import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileSet

class InstancedMap(val mapWidth: Int, val mapHeight: Int,
                   val map: TiledMap, val tileSet: TiledMapTileSet,
                   val tileWidth: Int, val tileHeight: Int) {
}