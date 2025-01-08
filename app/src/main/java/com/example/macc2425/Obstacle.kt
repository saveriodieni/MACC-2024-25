package com.example.macc2425

import org.json.JSONArray
import org.json.JSONObject

data class Obstacle(val x: Int, val y: Int)

// Funzione helper per il parsing del JSON in oggetti Kotlin
fun parseObstaclesFromJson(obstaclesJson: String): List<Obstacle> {
    return try {
        val jsonArray = JSONArray(obstaclesJson)
        (0 until jsonArray.length()).map { index ->
            val innerArray = jsonArray.getJSONArray(index)
            val x = innerArray.getInt(0) // Primo valore (x)
            val y = innerArray.getInt(1) // Secondo valore (y)
            Obstacle(x = x, y = y)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}


fun serializeObstaclesToJson(obstacles: List<Obstacle>?): String {
    val jsonArray = JSONArray()
    obstacles?.forEach { obstacle ->  // Itera solo se la lista non è null
        val jsonObject = JSONObject()
        jsonObject.put("x", obstacle.x)
        jsonObject.put("y", obstacle.y)
        jsonArray.put(jsonObject)
    }
    return jsonArray.toString()
}



