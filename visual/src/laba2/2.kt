package laba2

import kotlin.concurrent.thread

fun main() {
    // Создаем массив людей с double позициями
    val humans = arrayOf(
        Human(1, 25.0, 1.5),
        Human(2, 30.0, 2.0),
        Human(3, 35.0, 1.0),
        Human(4, 28.0, 1.8)
    )

    // Создаем водителя
    val driver = Driver(5, 40.0, 2.5, "ABC123")

    // Параметры симуляции
    val simulationTime = 10.0
    val timeStep = 0.5
    val totalSteps = (simulationTime / timeStep).toInt()

    println("Начало симуляции движения ${humans.size} людей и 1 водителя")
    println("Общее время: $simulationTime сек, шаг: $timeStep сек")
    println("=" * 50)

    // Начальные позиции
    println("Начальные позиции:")
    humans.forEach { it.displayInfo() }
    driver.displayInfo()
    println("=" * 50)

    // Список для хранения потоков
    val threads = mutableListOf<Thread>()

    // Создаем и запускаем потоки для каждого объекта
    humans.forEach { human ->
        val thread = thread {
            for (step in 1..totalSteps) {
                human.move(timeStep)
                Thread.sleep(500) // Задержка между шагами
            }
        }
        threads.add(thread)
    }

    // Поток для водителя
    val driverThread = thread {
        for (step in 1..totalSteps) {
            driver.move(timeStep)
            Thread.sleep(500) // Задержка между шагами
        }
    }
    threads.add(driverThread)

    // Ожидаем завершения всех потоков
    threads.forEach { it.join() }

    // Финальные позиции
    println("\n" + "=" * 50)
    println("Финальные позиции после симуляции:")
    humans.forEach { it.displayInfo() }
    driver.displayInfo()
}

operator fun String.times(n: Int): String = this.repeat(n)

open class Human(val id: Int, var position: Double, val speed: Double) {

    open fun move(timeStep: Double) {
        val distance = speed * timeStep
        position += distance
        println("Человек $id прошел ${"%.2f".format(distance)} м, новая позиция: ${"%.2f".format(position)}")
    }

    open fun displayInfo() {
        println("Человек $id: позиция=${"%.2f".format(position)}, скорость=$speed м/с")
    }
}

class Driver(id: Int, position: Double, speed: Double, val licensePlate: String) : Human(id, position, speed) {

    override fun move(timeStep: Double) {
        // Прямолинейное движение с постоянной скоростью
        val distance = speed * timeStep
        position += distance
        println("Водитель $id (авто $licensePlate) проехал ${"%.2f".format(distance)} м, новая позиция: ${"%.2f".format(position)}")
    }

    override fun displayInfo() {
        println("Водитель $id: позиция=${"%.2f".format(position)}, скорость=$speed м/с, номер авто: $licensePlate")
    }
}