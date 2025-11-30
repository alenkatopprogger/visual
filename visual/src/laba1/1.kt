package laba1

open class Mebel(
    val color: String,
    val material: String,
    override var position: Double,
    override val speed: Double
) : Movable {

    override fun move(timeStep: Double) {
        position += speed * timeStep
        println("$material $color мебель перемещена на позицию $position")
    }

    override fun displayInfo() {
        println("$material $color мебель: позиция=$position, скорость=$speed")
    }
}

class Chair(
    color: String,
    material: String,
    position: Double,
    speed: Double
) : Mebel(color, material, position, speed)

class Table(
    color: String,
    material: String,
    position: Double,
    speed: Double
) : Mebel(color, material, position, speed)

class Sofa(
    color: String,
    material: String,
    position: Double,
    speed: Double
) : Mebel(color, material, position, speed)

class Mel(
    val color: String,
    override var position: Double,
    override val speed: Double
) : Movable {

    override fun move(timeStep: Double) {
        position += speed * timeStep
        println("Мел цвета $color перемещён на позицию $position")
    }

    override fun displayInfo() {
        println("Мел цвета $color: позиция=$position, скорость=$speed")
    }
}

fun main() {
    val chair = Chair("коричневый", "дерево", 0.0, 2.0)
    val table = Table("белый", "стекло", 0.0, 1.5)
    val sofa = Sofa("синий", "ткань", 0.0, 0.8)
    val mel = Mel("белый", 0.0, 3.0)

    chair.move(2.0)
    chair.displayInfo()

    table.move(1.0)
    table.displayInfo()

    sofa.move(3.0)
    sofa.displayInfo()

    mel.move(1.5)
    mel.displayInfo()
}