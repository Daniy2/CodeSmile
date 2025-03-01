package util

import com.google.gson.Gson
import formatCsvToReadableText
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.swing.JTextArea

data class ApiResponse(val status: String, val message: String, val output_path: String)

fun sendAnalysisRequest(projectPath: String?, resultsArea: JTextArea) {
    val client = OkHttpClient()

    // Crea la richiesta JSON con il percorso del progetto
    val json = """
        {
            "input_directory": "$projectPath",
            "output_directory": "$projectPath\\OUTPUT"
        }
    """.trimIndent()

    val body = json
        .toRequestBody("application/json".toMediaTypeOrNull())

    // Crea la richiesta HTTP
    val request = Request.Builder()
        .url("http://localhost:5000/analyze")
        .post(body)
        .build()

    // Esegue richiesta in modo asincrono
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace() // Gestione degli errori
            resultsArea.append("Errore nella richiesta: ${e.message}\n")
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val responseBody = response.body?.string()

                //Gson per deserializzare la risposta JSON
                val apiResponse = Gson().fromJson(responseBody, ApiResponse::class.java)

                // Mostra il messaggio e il percorso del file
                resultsArea.append("Risultati dell'analisi:\n")


                val csvFilePath = apiResponse.output_path // Percorso completo al file overview.csv

                // Chiama la funzione per formattare il CSV
                val formattedResults = formatCsvToReadableText(csvFilePath)

                // Aggiungi i risultati formattati all'area di testo
                resultsArea.append(formattedResults)
            } else {
                resultsArea.append("Errore nella risposta del server.\n")
            }
        }
    })
}
