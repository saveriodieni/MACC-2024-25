package com.example.macc2425

import org.json.JSONArray
import org.json.JSONObject

data class Obstacle(val x: Int, val y: Int)

// Funzione helper per il parsing del JSON in una Map<Int, List<Obstacle>>
fun parseObstaclesFromJson(obstaclesJson: String): Map<Int, List<Obstacle>> {
    return try {
        val jsonObject = JSONObject(obstaclesJson)
        val result = mutableMapOf<Int, List<Obstacle>>()

        jsonObject.keys().forEach { key ->
            val level = key.toInt() // Converte la chiave (level) in un intero
            val jsonArray = jsonObject.getJSONArray(key)
            val obstacles = (0 until jsonArray.length()).map { index ->
                val innerArray = jsonArray.getJSONArray(index)
                val x = innerArray.getInt(0) // Primo valore (x)
                val y = innerArray.getInt(1) // Secondo valore (y)
                Obstacle(x = x, y = y)
            }
            result[level] = obstacles
        }
        result
    } catch (e: Exception) {
        e.printStackTrace()
        emptyMap()
    }
}

fun serializeObstaclesToJson(obstaclesMap: Map<Int, List<Obstacle>>?): String {
    val jsonObject = JSONObject()
    obstaclesMap?.forEach { (level, obstaclesList) ->
        val jsonArray = JSONArray()
        obstaclesList.forEach { obstacle ->
            val obstacleArray = JSONArray()
            obstacleArray.put(obstacle.x)
            obstacleArray.put(obstacle.y)
            jsonArray.put(obstacleArray)
        }
        jsonObject.put(level.toString(), jsonArray)
    }
    return jsonObject.toString()
}



