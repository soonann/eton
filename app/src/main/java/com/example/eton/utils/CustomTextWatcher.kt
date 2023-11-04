package com.example.eton.utils

import android.text.Editable
import android.text.TextWatcher
import android.util.Log

class CustomTextWatcher: TextWatcher {
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        Log.i("before string changed", p0.toString())
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        Log.i("on text changed", p0.toString())
    }

    override fun afterTextChanged(p0: Editable?) {
        Log.i("after string changed", p0.toString())
    }
}