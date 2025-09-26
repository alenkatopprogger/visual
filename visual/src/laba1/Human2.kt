package laba1

fun main() {
    // Создаем массив людей с double позициями
    val humans = arrayOf(
        Human(1, 25.0, 1.5),
        Human(2, 30.0, 2.0),
        Human(3, 35.0, 1.0),
        Human(4, 28.0, 1.8),
        Human(5, 32.0, 2.2),
        Human(6, 38.0, 2.4)
    )

    // Параметры симуляции
    val simulationTime = 10.0
    val timeStep = 0.5
    val totalSteps = (simulationTime / timeStep).toInt()

    println("Начало симуляции движения ${humans.size} людей")
    println("Общее время: $simulationTime сек, шаг: $timeStep сек")
    println("=" * 50)

    // Начальные позиции
    println("Начальные позиции:")
    humans.forEach { it.displayInfo() }
    println("=" * 50)

    // Основной цикл симуляции
    for (step in 1..totalSteps) {
        println("\nШаг $step (время: ${"%.1f".format(step * timeStep)} сек):")
        humans.forEach { it.move(timeStep) }

        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            break
        }
    }

    // Финальные позиции
    println("\n" + "=" * 50)
    println("Финальные позиции после симуляции:")
    humans.forEach { it.displayInfo() }
}

operator fun String.times(n: Int): String = this.repeat(n)

class Human(val id: Int, var position: Double, val speed: Double) {

    fun move(timeStep: Double) {
        val distance = speed * timeStep
        position += distance
        println("Человек $id прошел ${"%.2f".format(distance)} м, новая позиция: ${"%.2f".format(position)}")
    }

    fun displayInfo() {
        println("Человек $id: позиция=${"%.2f".format(position)}, скорость=$speed м/с")
    }
}