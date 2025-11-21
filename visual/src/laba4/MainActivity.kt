package laba4

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private lateinit var tvDisplay: TextView
    private var currentInput = StringBuilder()
    private var currentOperator: String? = null
    private var firstOperand: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupNumberButtons()
        setupOperatorButtons()
        setupActionButtons()
    }

    private fun initializeViews() {
        tvDisplay = findViewById(R.id.tvDisplay)
    }

    private fun setupNumberButtons() {
        // Обработчики для цифровых кнопок (0-9)
        val numberButtons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        numberButtons.forEach { buttonId ->
            findViewById<Button>(buttonId).setOnClickListener {
                val number = (it as Button).text.toString()
                appendNumber(number)
            }
        }
    }

    private fun setupOperatorButtons() {
        // Обработчики для операторов (+, -, *, /)
        findViewById<Button>(R.id.btnPlus).setOnClickListener { setOperator("+") }
        findViewById<Button>(R.id.btnMinus).setOnClickListener { setOperator("-") }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { setOperator("*") }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { setOperator("/") }
    }

    private fun setupActionButtons() {
        // Кнопка очистки
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            clearCalculator()
        }

        // Кнопка равно
        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            calculateResult()
        }
    }

    private fun appendNumber(number: String) {
        // Если текущий ввод "0", заменяем его на новое число
        if (currentInput.toString() == "0") {
            currentInput.clear()
        }

        currentInput.append(number)
        updateDisplay()
    }

    private fun setOperator(operator: String) {
        if (currentInput.isNotEmpty()) {
            if (currentOperator == null) {
                // Первый оператор
                firstOperand = currentInput.toString().toDouble()
                currentOperator = operator
                currentInput.clear()
                updateDisplayWithOperator(operator)
            } else {
                // Если оператор уже выбран, сначала вычисляем результат
                calculateResult()
                currentOperator = operator
                firstOperand = currentInput.toString().toDouble()
                currentInput.clear()
                updateDisplayWithOperator(operator)
            }
        }
    }

    private fun calculateResult() {
        if (currentOperator != null && currentInput.isNotEmpty()) {
            try {
                val secondOperand = currentInput.toString().toDouble()
                val result = when (currentOperator) {
                    "+" -> firstOperand + secondOperand
                    "-" -> firstOperand - secondOperand
                    "*" -> firstOperand * secondOperand
                    "/" -> {
                        if (secondOperand != 0.0) {
                            firstOperand / secondOperand
                        } else {
                            // Обработка деления на ноль
                            Double.NaN
                        }
                    }
                    else -> throw IllegalArgumentException("Неизвестный оператор")
                }

                if (result.isNaN()) {
                    currentInput.clear().append("Ошибка")
                } else {
                    // Форматируем результат: убираем .0 если число целое
                    currentInput.clear()
                    if (result % 1 == 0.0) {
                        currentInput.append(result.toLong())
                    } else {
                        currentInput.append(result)
                    }
                }

                currentOperator = null
                updateDisplay()

            } catch (e: Exception) {
                currentInput.clear().append("Ошибка")
                updateDisplay()
                currentOperator = null
            }
        }
    }

    private fun clearCalculator() {
        currentInput.clear().append("0")
        currentOperator = null
        firstOperand = 0.0
        updateDisplay()
    }

    private fun updateDisplay() {
        tvDisplay.text = if (currentInput.isEmpty()) "0" else currentInput.toString()
    }

    private fun updateDisplayWithOperator(operator: String) {
        val displayText = if (currentInput.isEmpty()) {
            "${firstOperand.toLong()} $operator"
        } else {
            "${firstOperand.toLong()} $operator ${currentInput.toString()}"
        }
        tvDisplay.text = displayText
    }
}