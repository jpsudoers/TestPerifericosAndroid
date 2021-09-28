package com.ingenico.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.lang.Exception
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.text.NumberFormat

class NumberTextWatcher(editText: EditText?) : TextWatcher {
    private val editTextWeakReference: WeakReference<EditText> = WeakReference<EditText>(editText)
    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(editable: Editable) {
        val editText: EditText = editTextWeakReference.get() ?: return
        val s = editable.toString()

        if (s.isEmpty())
            return

        editText.removeTextChangedListener(this)

        val cleanString = s.replace("[^0-9]".toRegex(), "")
        val parsed: BigDecimal =
            BigDecimal(cleanString).setScale(2, BigDecimal.ROUND_FLOOR).divide(BigDecimal(100), BigDecimal.ROUND_FLOOR)
        val formatted: String = NumberFormat.getCurrencyInstance().format(parsed)
        editText.setText(formatted)
        try {
            var maxLn: Int

            if(formatted.length > 13)
                maxLn = 13
            else
                maxLn = formatted.length

            editText.setSelection(maxLn)
        }
        catch(e : Exception){

        }
        editText.addTextChangedListener(this)
    }

}