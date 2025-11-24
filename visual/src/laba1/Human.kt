package laba2

open class Human(
    val id: Int,
    override var position: Double,
    override val speed: Double
) : Movable {

    override fun move(timeStep: Double) {
        val distance = speed * timeStep
        position += distance
        println("Человек $id прошел ${"%.2f".format(distance)} м, новая позиция: ${"%.2f".format(position)}")
    }

    override fun displayInfo() {
        println("Человек $id: позиция=${"%.2f".format(position)}, скорость=$speed м/с")
    }
}