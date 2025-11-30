package laba1

import kotlin.concurrent.thread

fun main() {
    val humans = arrayOf(
        Human(1, 25.0, 1.5),
        Human(2, 30.0, 2.0),
        Human(3, 35.0, 1.0),
        Human(4, 28.0, 1.8)
    )

    val driver = Driver(5, 40.0, 2.5, "ABC123")

    val simulationTime = 10.0
    val timeStep = 0.5
    val totalSteps = (simulationTime / timeStep).toInt()

    println("Начало симуляции движения ${humans.size} людей и 1 водителя")
    println("Общее время: $simulationTime сек, шаг: $timeStep сек")
    println("=" * 50)

    println("Начальные позиции:")
    humans.forEach { it.displayInfo() }
    driver.displayInfo()
    println("=" * 50)

    val threads = mutableListOf<Thread>()

    humans.forEach { human ->
        val thread = thread {
            for (step in 1..totalSteps) {
                human.move(timeStep)
                Thread.sleep(500)
            }
        }
        threads.add(thread)
    }

    val driverThread = thread {
        for (step in 1..totalSteps) {
            driver.move(timeStep)
            Thread.sleep(500)
        }
    }
    threads.add(driverThread)

    threads.forEach { it.join() }

    println("\n" + "=" * 50)
    println("Финальные позиции после симуляции:")
    humans.forEach { it.displayInfo() }
    driver.displayInfo()
}