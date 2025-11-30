package laba1

class Driver(
    id: Int,
    position: Double,
    speed: Double,
    val licensePlate: String
) : Human(id, position, speed) {

    override fun move(timeStep: Double) {
        val distance = speed * timeStep
        position += distance
        println("Водитель $id (авто $licensePlate) проехал ${"%.2f".format(distance)} м, новая позиция: ${"%.2f".format(position)}")
    }

    override fun displayInfo() {
        println("Водитель $id: позиция=${"%.2f".format(position)}, скорость=$speed м/с, номер авто: $licensePlate")
    }
}