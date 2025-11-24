package laba2

interface Movable {
    var position: Double
    val speed: Double

    fun move(timeStep: Double)

    fun displayInfo()
}