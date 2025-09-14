import kotlin.random.Random
import kotlin.math.*

class HumanGaussMarkov(
    private var id: Int,
    private var age: Int,
    private var currentSpeed: Double,
    private var x: Double = 0.0,
    private var y: Double = 0.0,
    private var direction: Double = Random.nextDouble(0.0, 2 * Math.PI)
) {
    // Геттеры и сеттеры
    fun getId(): Int = id
    fun getAge(): Int = age
    fun getCurrentSpeed(): Double = currentSpeed
    fun getX(): Double = x
    fun getY(): Double = y

    fun setId(newId: Int) { id = newId }
    fun setAge(newAge: Int) { age = newAge }
    fun setCurrentSpeed(newSpeed: Double) { currentSpeed = newSpeed }

    // Метод move() с Gauss-Markov моделью
    fun move(timeStep: Double, alpha: Double = 0.8) {
        // Gauss-Markov модель: плавное изменение направления и скорости
        val randomAngle = Random.nextDouble(-Math.PI/4, Math.PI/4)
        val randomSpeedChange = Random.nextDouble(-0.2, 0.2)

        // Обновление направления с памятью (alpha - коэффициент памяти)
        direction = alpha * direction + (1 - alpha) * randomAngle
        direction = direction % (2 * Math.PI) // Нормализация угла

        // Обновление скорости с памятью
        currentSpeed = max(0.1, alpha * currentSpeed + (1 - alpha) * randomSpeedChange)

        // Перемещение
        val distance = currentSpeed * timeStep
        x += distance * cos(direction)
        y += distance * sin(direction)

        println("Human $id: скорость=${"%.2f".format(currentSpeed)}, направление=${"%.2f".format(Math.toDegrees(direction))}°")
    }

    fun displayInfo() {
        println("Human $id: возраст=$age, позиция=(${"%.2f".format(x)}, ${"%.2f".format(y)})")
    }
}