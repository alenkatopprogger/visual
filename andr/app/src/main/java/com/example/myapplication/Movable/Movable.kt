package com.example.myapplication.Movable

interface Movable {
    var position: Double
    val speed: Double

    fun move(timeStep: Double)

    fun displayInfo()
}