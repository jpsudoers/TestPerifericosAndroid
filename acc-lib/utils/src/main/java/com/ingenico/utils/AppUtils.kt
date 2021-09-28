package com.ingenico.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import java.security.MessageDigest

class AppUtils {

    companion object {

        fun showKeyboard(editText: EditText, activity: Activity){
            val imm = (activity).getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }

        fun hideKeyboard(activity: Activity){
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
        }

        fun parseAmount(amountStr: String): Long {
            val dotPos = amountStr.indexOf('.')

            var result : Long
            if (dotPos >= 0)
                result = amountStr.substring(1, dotPos) .toLong() * 100 + amountStr.substring(dotPos + 1).toLong()
            else
                result = amountStr.substring(1, amountStr.length) .toLong() * 100

            return result
        }

        fun popUpDialogOneButton(context: Context, title : String, text : String, textButton : String, id : Int = 0){

            val builder = AlertDialog.Builder(context)

            builder.setTitle(title)
            builder.setMessage(text)
            builder.setIcon(id)

            builder.setPositiveButton(textButton){ _, _ ->
            }

            builder.create().show()
        }

        fun setFormatAmountDecimal(editText: EditText) {
            editText.setRawInputType(Configuration.KEYBOARD_12KEY)
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    val rg = Regex("^\\$(\\d{1,3}(\\,\\d{3})*|(\\d+))(\\.\\d{2})?$")

                    if (!s.toString().matches(rg)) {
                        val userInput = "" + s.toString().replace("[^\\d]".toRegex(), "")
                        val cashAmountBuilder =
                            StringBuilder(userInput)
                        while (cashAmountBuilder.length > 3 && cashAmountBuilder[0] == '0') {
                            cashAmountBuilder.deleteCharAt(0)
                        }
                        while (cashAmountBuilder.length < 3) {
                            cashAmountBuilder.insert(0, '0')
                        }
                        cashAmountBuilder.insert(cashAmountBuilder.length - 2, '.')
                        cashAmountBuilder.insert(0, '$')
                        editText.setText(cashAmountBuilder.toString())
                        // keeps the cursor always to the right
                        Selection.setSelection(
                            editText.text,
                            cashAmountBuilder.toString().length
                        )
                    }
                }
            })
        }

        fun setFormatAmountWODecimal(editText: EditText) {
            editText.setRawInputType(Configuration.KEYBOARD_12KEY)
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}

                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    val rg = Regex("^\\$(\\d{1,3}(\\,\\d{3})*|(\\d+))?$")

                    if (!s.toString().matches(rg)) {
                        val userInput = "" + s.toString().replace("[^\\d]".toRegex(), "")
                        val cashAmountBuilder =
                            StringBuilder(userInput)
                        cashAmountBuilder.insert(0, '$')
                        editText.setText(cashAmountBuilder.toString())
                        // keeps the cursor always to the right
                        Selection.setSelection(
                            editText.text,
                            cashAmountBuilder.toString().length
                        )
                    }
                }
            })
        }

        private fun hashString(input: String, algorithm: String): String {
            return MessageDigest
                .getInstance(algorithm)
                .digest(input.toByteArray())
                .fold("", { str, it -> str + "%02x".format(it) })
        }

        fun calculateSH256(secret: String): String {
            return hashString(secret, "SHA-256")
        }
    }
}