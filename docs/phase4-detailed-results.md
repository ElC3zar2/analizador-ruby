# Resultados detallados — Fase 4

La interfaz reutiliza `analysisState.result` sin cambiar el endpoint `POST api/analyze`. El resumen general permanece visible y el modo seleccionado muestra Léxico, Sintáctico, Semántico o las tres secciones en ese orden. Cambiar el modo no llama al servidor ni vuelve a construir las tablas ya creadas.

## Datos representados

- Tokens: Lexema, Token, Línea, Columna. Conservan el orden, incluido EOF.
- Errores léxicos: Mensaje, Lexema, Línea, Columna.
- Alfabeto: lista de etiquetas en una sección colapsable.
- Símbolos léxicos: Símbolo, Línea, Columna en otra sección colapsable.
- Sintáctico: estado basado en `syntaxErrorCount`; errores con Mensaje, Token, Lexema, Línea, Columna.
- Símbolos semánticos: Nombre, Tipo de símbolo, Tipo inferido, Ámbito, Línea, Columna, Aridad. Los enums y la aridad (incluido -1) permanecen como los entrega el servidor.
- Errores semánticos: Mensaje, Línea, Columna.

No se añade un AST ni se alteran los datos del JSON. Cada colección vacía tiene una explicación; una colección ausente informa que el detalle no está disponible.

## Texto y seguridad

Los datos se insertan exclusivamente con `createElement`, `textContent` y `appendChild`. HTML, comillas y nombres arbitrarios se representan como texto.

Para lexemas, alfabeto y símbolos léxicos, la vista identifica espacios como `[espacio]`, tabulación como `\t`, LF como `\n`, CR como `\r` y otros controles como `\uXXXX`. La cadena vacía se indica con `[vacío]`; el JSON almacenado permanece intacto.

Las tablas tienen caption, encabezados con `scope="col"` y contenedores enfocables con scroll horizontal y vertical. Los lexemas largos se conservan y pueden partirse visualmente. Al editar o cargar otro código, iniciar otra petición o fallar el análisis, se eliminan los detalles anteriores.

## Pruebas

Desde PowerShell:

```powershell
& C:\Ruby\tests\frontend\verify.ps1
```

La suite amplía el DOM simulado existente para comprobar árboles de nodos, celdas y visibilidad. Utiliza Java 11 y las clases ya compiladas de `target/classes` para generar respuestas reales con AnalysisService y AnalysisJson, sin modificar Java ni añadir dependencias. Si `target/classes` no existe, generar las clases con el Maven existente antes de ejecutar la suite.

Las expectativas de 38/73/174 tokens y 3/7/10 símbolos semánticos solo aparecen en pruebas. También se verifican respuestas reales de `x = 10 ?`, `x =` y `puts nombre`.

La comprobación visual de scroll, plegado, foco y pantallas pequeñas queda para el navegador conectado al Tomcat de NetBeans; las pruebas con DOM simulado no verifican la apariencia real.
