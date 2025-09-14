import kotlin.random.Random

class Human(
    private var id: Int,
    private var age: Int,
    private var currentSpeed: Double,
    private var x: Double = 0.0,
    private var y: Double = 0.0
) {
    // Геттеры
    fun getId(): Int = id
    fun getAge(): Int = age
    fun getCurrentSpeed(): Double = currentSpeed
    fun getX(): Double = x
    fun getY(): Double = y

    // Сеттеры
    fun setId(newId: Int) { id = newId }
    fun setAge(newAge: Int) { age = newAge }
    fun setCurrentSpeed(newSpeed: Double) { currentSpeed = newSpeed }

    // Метод move() с Random Walk моделью
    fun move(timeStep: Double) {
        // Random Walk: случайное направление движения
        val angle = Random.nextDouble(0.0, 2 * Math.PI)

        // Перемещение с текущей скоростью
        val distance = currentSpeed * timeStep
        x += distance * Math.cos(angle)
        y += distance * Math.sin(angle)

        println("Human $id переместился в позицию (${"%.2f".format(x)}, ${"%.2f".format(y)})")
    }

    // Информация о человеке
    fun displayInfo() {
        println("Human $id: возраст=$age, скорость=${"%.2f".format(currentSpeed)}, позиция=(${"%.2f".format(x)}, ${"%.2f".format(y)})")
    }
}