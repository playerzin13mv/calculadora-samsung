package com.calc.samsung

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import java.math.BigDecimal
import java.math.MathContext

class MainActivity : Activity() {
    private val BG = Color.parseColor("#121212")
    private val KEY = Color.parseColor("#2B2B2B")
    private val GREEN = Color.parseColor("#4FD98B")
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

    private var expr = ""
    private var justEvaluated = false
    private lateinit var tvExpr: TextView
    private lateinit var tvPreview: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            fitsSystemWindows = true
        }
        val display = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM or Gravity.END
            setPadding(dp(24), dp(8), dp(24), dp(8))
        }
        tvExpr = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 56f
            gravity = Gravity.END
            maxLines = 3
        }
        tvPreview = TextView(this).apply {
            setTextColor(Color.parseColor("#9A9A9A"))
            textSize = 28f
            gravity = Gravity.END
            minHeight = dp(44)
        }
        val back = TextView(this).apply {
            text = "⌫"
            textSize = 26f
            setTextColor(GREEN)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener {
                justEvaluated = false
                expr = expr.dropLast(1)
                refresh()
            }
            setOnLongClickListener {
                expr = ""; justEvaluated = false; refresh(); true
            }
        }
        display.addView(tvExpr, LinearLayout.LayoutParams(MATCH, ViewGroup.LayoutParams.WRAP_CONTENT))
        display.addView(tvPreview, LinearLayout.LayoutParams(MATCH, ViewGroup.LayoutParams.WRAP_CONTENT))
        display.addView(back, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.END })

        val pad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), dp(8))
        }
        val rows = listOf(
            listOf("C", "( )", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("±", "0", ".", "=")
        )
        for (r in rows) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (l in r) row.addView(key(l))
            pad.addView(row, LinearLayout.LayoutParams(MATCH, 0, 1f))
        }
        root.addView(display, LinearLayout.LayoutParams(MATCH, 0, 1f))
        root.addView(pad, LinearLayout.LayoutParams(MATCH, 0, 1.9f))
        setContentView(root)
        refresh()
    }

    private fun key(label: String): TextView {
        val kind = when {
            label == "=" -> 2
            label in listOf("C", "( )", "%", "÷", "×", "−", "+") -> 1
            else -> 0
        }
        return TextView(this).apply {
            text = label
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(when (kind) { 2 -> BG; 1 -> GREEN; else -> Color.WHITE })
            background = GradientDrawable().apply {
                cornerRadius = dp(100).toFloat()
                setColor(if (kind == 2) GREEN else KEY)
            }
            isClickable = true
            setOnClickListener { press(label) }
            setOnTouchListener { v, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> v.alpha = 0.6f
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.alpha = 1f
                }
                false
            }
            layoutParams = LinearLayout.LayoutParams(0, MATCH, 1f).apply {
                setMargins(dp(5), dp(5), dp(5), dp(5))
            }
        }
    }

    private fun isOp(c: Char) = c in "+−×÷"
    private fun endsValue() = expr.isNotEmpty() && (expr.last().isDigit() || expr.last() == ')' || expr.last() == '%')

    private fun press(k: String) {
        when (k) {
            "C" -> { expr = ""; justEvaluated = false }
            "=" -> { equals(); return }
            "( )" -> paren()
            "%" -> if (endsValue()) { justEvaluated = false; expr += "%" }
            "±" -> toggleSign()
            "." -> dot()
            "+", "−", "×", "÷" -> op(k)
            else -> digit(k)
        }
        refresh()
    }

    private fun fresh() { if (justEvaluated) { expr = ""; justEvaluated = false } }

    private fun digit(k: String) {
        fresh()
        if (expr.isNotEmpty() && (expr.last() == ')' || expr.last() == '%')) expr += "×"
        val num = expr.takeLastWhile { it.isDigit() || it == '.' }
        if (num == "0") expr = expr.dropLast(1)
        expr += k
    }

    private fun dot() {
        fresh()
        val num = expr.takeLastWhile { it.isDigit() || it == '.' }
        if (num.contains('.')) return
        if (num.isEmpty()) {
            if (endsValue()) expr += "×"
            expr += "0"
        }
        expr += "."
    }

    private fun op(k: String) {
        justEvaluated = false
        if (expr.isEmpty() || expr.last() == '(') {
            if (k == "−") expr += k
            return
        }
        if (isOp(expr.last())) {
            expr = expr.dropLast(1)
            if (expr.isEmpty() && k != "−") return
        }
        expr += k
    }

    private fun paren() {
        fresh()
        val open = expr.count { it == '(' }
        val close = expr.count { it == ')' }
        if (open > close && endsValue()) {
            expr += ")"
        } else {
            if (endsValue()) expr += "×"
            expr += "("
        }
    }

    private fun toggleSign() {
        justEvaluated = false
        val num = expr.takeLastWhile { it.isDigit() || it == '.' }
        if (num.isEmpty()) return
        val start = expr.length - num.length
        val before = expr.substring(0, start)
        expr = when {
            before.isEmpty() || before.last() == '(' -> before + "−" + num
            before.last() == '−' -> {
                val unary = before.length == 1 || before[before.length - 2] in "(×÷+−"
                if (unary) before.dropLast(1) + num else before.dropLast(1) + "+" + num
            }
            before.last() == '+' -> before.dropLast(1) + "−" + num
            before.last() == '×' || before.last() == '÷' -> before + "−" + num
            else -> expr
        }
    }

    private fun equals() {
        if (expr.isEmpty()) return
        try {
            expr = eval(expr)
            justEvaluated = true
            refresh()
        } catch (e: ArithmeticException) {
            tvPreview.text = "Não é possível dividir por zero"
        } catch (e: Exception) {
            tvPreview.text = "Formato inválido"
        }
    }

    private fun refresh() {
        tvExpr.text = expr
        tvExpr.textSize = when {
            expr.length <= 8 -> 56f
            expr.length <= 11 -> 46f
            expr.length <= 15 -> 36f
            expr.length <= 22 -> 28f
            else -> 22f
        }
        tvPreview.text = if (!justEvaluated && expr.any { isOp(it) || it == '%' }) {
            try { eval(expr.trimEnd('+', '−', '×', '÷')) } catch (e: Exception) { "" }
        } else ""
    }

    private fun eval(e: String): String {
        val open = e.count { it == '(' } - e.count { it == ')' }
        val s = e + ")".repeat(maxOf(0, open))
        val r = Parser(s).parse()
        return r.round(MathContext(12)).stripTrailingZeros().toPlainString().replace('-', '−')
    }

    private class Parser(private val s: String) {
        private var i = 0
        private var factorPct = false
        private var termPct = false
        private val mc = MathContext(20)

        fun parse(): BigDecimal {
            val v = expr()
            if (i != s.length) throw IllegalArgumentException()
            return v
        }

        private fun expr(): BigDecimal {
            var v = term()
            while (i < s.length && (s[i] == '+' || s[i] == '−')) {
                val op = s[i++]
                val r = term()
                val d = if (termPct) v.multiply(r, mc) else r
                v = if (op == '+') v.add(d, mc) else v.subtract(d, mc)
            }
            return v
        }

        private fun term(): BigDecimal {
            var v = unary()
            var single = factorPct
            while (i < s.length && (s[i] == '×' || s[i] == '÷')) {
                val op = s[i++]
                val r = unary()
                v = if (op == '×') v.multiply(r, mc) else {
                    if (r.signum() == 0) throw ArithmeticException()
                    v.divide(r, mc)
                }
                single = false
            }
            termPct = single
            return v
        }

        private fun unary(): BigDecimal {
            if (i < s.length && s[i] == '−') { i++; return unary().negate() }
            if (i < s.length && s[i] == '+') { i++; return unary() }
            return postfix()
        }

        private fun postfix(): BigDecimal {
            var v = primary()
            var p = false
            while (i < s.length && s[i] == '%') {
                v = v.divide(BigDecimal(100), mc)
                p = true
                i++
            }
            factorPct = p
            return v
        }

        private fun primary(): BigDecimal {
            if (i < s.length && s[i] == '(') {
                i++
                val v = expr()
                if (i >= s.length || s[i] != ')') throw IllegalArgumentException()
                i++
                return v
            }
            val st = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            if (st == i) throw IllegalArgumentException()
            return BigDecimal(s.substring(st, i))
        }
    }
}
