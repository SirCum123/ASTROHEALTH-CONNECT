# ASTROHEALTH CONNECT

Prototipo Android (Kotlin + Health Connect) con un panel HTML local en WebView.

## Qué consulta

- Frecuencia cardíaca: muestra la muestra más reciente de las últimas 24 horas.
- Saturación de oxígeno (SpO2): muestra el registro más reciente disponible.
- Pasos: total agregado de las últimas 24 horas.

No todos los relojes registran todas las variables. Health Connect solo permite leer datos que una aplicación/dispositivo compatible haya sincronizado y para los que el usuario haya concedido permiso.

## Requisitos

- Android Studio reciente.
- Teléfono Android 9 (API 28) o superior.
- Health Connect disponible y actualizado.
- Reloj emparejado con su aplicación oficial y sincronizando datos hacia Health Connect.
- JDK 17 para la configuración Gradle incluida.

## Abrir y ejecutar

1. Descomprime `ASTROHEALTH_CONNECT.zip`.
2. Abre Android Studio.
3. Selecciona **Open** y elige la carpeta `ASTROHEALTH_CONNECT`.
4. Espera a que Gradle sincronice y descargue dependencias.
5. Conecta el teléfono Android por USB con depuración USB activada.
6. Pulsa Run para instalar la app.
7. En ASTROHEALTH CONNECT pulsa **Autorizar / actualizar datos**.
8. Concede solamente los permisos que quieras probar.
9. Asegúrate de que el reloj y su aplicación ya hayan sincronizado registros con Health Connect.
10. Pulsa de nuevo el botón para consultar.

## Si no aparecen datos

- Abre Health Connect desde Ajustes del teléfono.
- Revisa **Permisos de aplicaciones** y confirma que la aplicación del reloj puede escribir datos.
- Comprueba que haya registros recientes de frecuencia cardíaca, oxígeno o pasos.
- Vuelve a ASTROHEALTH CONNECT y actualiza.
- Si un campo dice "Sin dato", el reloj podría no medirlo o su aplicación podría no sincronizarlo.

## Arquitectura

Reloj -> aplicación del fabricante -> Health Connect -> MainActivity.kt -> puente JavaScript -> app/src/main/assets/index.html.

El puente JavaScript solo está expuesto en el WebView que carga el HTML local de la aplicación.

## Privacidad y límites

- Esta versión no sube datos a internet: la consulta y el panel ocurren en el teléfono.
- El usuario puede retirar permisos desde Health Connect.
- El prototipo consulta registros almacenados, no transmite señales biométricas en directo.
- No es un dispositivo médico, no diagnostica y no debe usarse para decisiones de salud.
- No es un sistema aprobado por NASA ni está conectado a una estación espacial.

## SDK

Se usa `androidx.health.connect:connect-client:1.1.0`, con `compileSdk 36` y Android Gradle Plugin 8.9.1 o posterior, según la guía vigente de Android.


## Adaptación FitCloudPro (Android 14)

La política de privacidad oficial de FitCloudPro indica que, con autorización, puede escribir en Health Connect datos como pasos, sueño, frecuencia cardíaca, presión arterial, oxígeno en sangre y temperatura corporal. La disponibilidad depende de la versión de FitCloudPro, del reloj y de las mediciones realmente sincronizadas.

En FitCloudPro:
1. Empareja el reloj desde FitCloudPro.
2. Espera a que sincronice. En la pantalla principal, desliza hacia abajo para forzar la sincronización si hace falta.
3. Busca en la sección "Mi" o en ajustes una opción de Health Connect / conexión de salud / compartir datos.
4. Activa Health Connect y autoriza los tipos de datos que quieras compartir.
5. En Android 14, abre Ajustes > Seguridad y privacidad > Health Connect > Permisos de aplicaciones y confirma que FitCloudPro pueda escribir los datos.
6. Instala ASTROHEALTH CONNECT y concede permisos de lectura para las métricas que quieras consultar.
7. En el panel pulsa "Autorizar / actualizar datos".

La ubicación exacta del interruptor puede variar por versión. Si no aparece, actualiza FitCloudPro y consulta su soporte. No se debe asumir que todas las métricas se exportan para todos los modelos.

## Métricas adicionales de esta edición

Además de frecuencia cardíaca, SpO2 y pasos, esta edición consulta presión arterial, temperatura corporal y sesiones de sueño cuando existan registros en Health Connect. Los datos faltantes aparecen como "Sin dato"; el software no inventa valores.

**Nota de seguridad:** las lecturas de relojes de consumo, especialmente presión arterial, temperatura y SpO2, son orientativas y no deben usarse para diagnósticos ni decisiones médicas.
