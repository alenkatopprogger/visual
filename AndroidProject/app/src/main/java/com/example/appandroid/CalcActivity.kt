package com.example.appandroid

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CalcActivity : AppCompatActivity() {

    private lateinit var textViewResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calc)

        textViewResult = findViewById(R.id.textViewResult)

        setupDigitButton(R.id.btn0, "0")
        setupDigitButton(R.id.btn1, "1")
        setupDigitButton(R.id.btn2, "2")
        setupDigitButton(R.id.btn3, "3")
        setupDigitButton(R.id.btn4, "4")
        setupDigitButton(R.id.btn5, "5")
        setupDigitButton(R.id.btn6, "6")
        setupDigitButton(R.id.btn7, "7")
        setupDigitButton(R.id.btn8, "8")
        setupDigitButton(R.id.btn9, "9")

        setupOperationButton(R.id.btnPlus, "+")
        setupOperationButton(R.id.btnMinus, "-")
        setupOperationButton(R.id.btnMultiply, "*")  // Используем "*" для умножения
        setupOperationButton(R.id.btnDivide, "/")

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            textViewResult.text = "0"
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            calculateResult()
        }
    }

    private fun setupDigitButton(id: Int, value: String) {
        findViewById<Button>(id).setOnClickListener {
            appendToExpression(value)
        }
    }

    private fun setupOperationButton(id: Int, operation: String) {
        findViewById<Button>(id).setOnClickListener {
            appendToExpression(operation)
        }
    }

    private fun appendToExpression(value: String) {
        val currentText = textViewResult.text.toString()
        if (currentText == "0") {
            textViewResult.text = value
        } else {
            textViewResult.text = currentText + value
        }
    }

    private fun calculateResult() {
        val expression = textViewResult.text.toString()

        var operatorIndex = -1
        var operatorChar = ' '

        if (expression.contains('+')) {
            operatorIndex = expression.indexOf('+')
            operatorChar = '+'
        } else if (expression.contains('-')) {
            operatorIndex = expression.indexOf('-')
            operatorChar = '-'
        } else if (expression.contains('*')) {
            operatorIndex = expression.indexOf('*')
            operatorChar = '*'
        } else if (expression.contains('/')) {
            operatorIndex = expression.indexOf('/')
            operatorChar = '/'
        }

        if (operatorIndex == -1) {
            return
        }

        val left = expression.substring(0, operatorIndex).trim()
        val right = expression.substring(operatorIndex + 1).trim()

        if (left.isEmpty() || right.isEmpty()) {
            textViewResult.text = "Error"
            return
        }

        try {
            val num1 = left.toDouble()
            val num2 = right.toDouble()
            var result: Double = 0.0

            when (operatorChar) {
                '+' -> result = num1 + num2
                '-' -> result = num1 - num2
                '*' -> result = num1 * num2
                '/' -> {
                    if (num2 == 0.0) {
                        textViewResult.text = "Error"
                        return
                    }
                    result = num1 / num2
                }
            }

            // Форматируем результат - убираем .0 если число целое
            val resultText = if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                result.toString()
            }
            textViewResult.text = resultText

        } catch (e: NumberFormatException) {
            textViewResult.text = "Error"
        }
    }
}