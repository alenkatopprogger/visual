fun main() {
    // Создаем массив людей (количество зависит от номера в списке группы)
    val humans = arrayOf(
        Human(1, 25, 1.5),
        Human(2, 30, 2.0),
        Human(3, 35, 1.0),
        Human(4, 28, 1.8),
        Human(5, 32, 2.2),
        Human(6, 38, 2.4)
    )

    // Параметры симуляции
    val simulationTime = 10.0 // секунд
    val timeStep = 0.5 // шаг времени в секундах
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

        // Небольшая пауза для наглядности
        Thread.sleep(500)
    }

    // Финальные позиции
    println("\n" + "=" * 50)
    println("Финальные позиции после симуляции:")
    humans.forEach { it.displayInfo() }
}

// Вспомогательная функция для повторения строки
operator fun String.times(n: Int): String {
    return this.repeat(n)
}