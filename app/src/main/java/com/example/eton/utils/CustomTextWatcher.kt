package com.example.eton.utils

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.RetryPolicy
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.eton.R
import org.json.JSONObject

class CustomTextWatcher(private val context: Context, private val etBody: EditText): TextWatcher {
    private lateinit var captureString: StringBuilder
    private var isCapturing = false
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // Do nothing
    }

    override fun onTextChanged(str: CharSequence?, p1: Int, p2: Int, p3: Int) {
        if (str!!.last() == '@') {
            if (!isCapturing) {
                // Start capturing the string
                captureString = StringBuilder()
                isCapturing = true
            } else {
                getResponse(captureString.toString())
                captureString.clear()
                isCapturing = false
            }
        } else if (isCapturing) {
            // Capture the character
            captureString.append(str.last())
        }
    }

    override fun afterTextChanged(p0: Editable?) {
        // Do nothing
    }

    private fun getResponse(query: String) {
        // creating a queue for request queue.
        val queue: RequestQueue = Volley.newRequestQueue(context)
        // creating a json object on below line.
        val jsonObject: JSONObject? = JSONObject()
        // adding params to json object.
        jsonObject?.put("model", "text-davinci-003")
        jsonObject?.put("prompt", query)
        jsonObject?.put("temperature", 0)
        jsonObject?.put("max_tokens", 200)
        jsonObject?.put("top_p", 1)
        jsonObject?.put("frequency_penalty", 0.0)
        jsonObject?.put("presence_penalty", 0.0)

        // on below line making json object request.
        val postRequest: JsonObjectRequest =
            // on below line making json object request.
            object : JsonObjectRequest(
                Method.POST, "https://api.openai.com/v1/completions", jsonObject,
                Response.Listener { response ->
                    // on below line getting response message and setting it to text view.
                    val responseMsg: String = response.getJSONArray("choices").getJSONObject(0).getString("text")
                    val temp = etBody.text.toString().split("@")[0]
                    etBody.setText("$temp$responseMsg")
                },
                // adding on error listener
                Response.ErrorListener { error ->
                    Log.e("TAGAPI", "Error is : " + error.message + "\n" + error)
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val params: MutableMap<String, String> = HashMap()
                    // adding headers on below line.
                    params["Content-Type"] = "application/json"
                    params["Authorization"] =
                        "Bearer ${context.getString(R.string.gpt_key)}"
                    return params;
                }
            }

        // on below line adding retry policy for our request.
        postRequest.setRetryPolicy(object : RetryPolicy {
            override fun getCurrentTimeout(): Int {
                return 10000
            }

            override fun getCurrentRetryCount(): Int {
                return 10000
            }

            @Throws(VolleyError::class)
            override fun retry(error: VolleyError) {
            }
        })
        // on below line adding our request to queue.
        queue.add(postRequest)
    }
}