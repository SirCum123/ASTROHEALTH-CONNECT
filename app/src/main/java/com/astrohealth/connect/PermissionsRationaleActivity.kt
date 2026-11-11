package com.astrohealth.connect

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class PermissionsRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = TextView(this).apply {
            text = """
                ASTROHEALTH CONNECT solicita permiso para leer:

                • Frecuencia cardíaca
                • Saturación de oxígeno (si el reloj la registra)
                • Pasos

                Los datos se muestran en el teléfono dentro del prototipo.
                Esta versión no los envía a un servidor ni realiza diagnósticos.
                Puedes retirar los permisos desde los ajustes de Health Connect.
            """.trimIndent()
            textSize = 18f
            setPadding(40, 40, 40, 40)
        }
        setContentView(text)
    }
}
