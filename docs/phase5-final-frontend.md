# Cierre técnico del frontend

## Arquitectura y archivos

HTML semántico en `src/main/webapp/index.html`, presentación oscura responsive en `css/styles.css` y JavaScript Vanilla en `js/app.js`. No hay frameworks, dependencias nuevas, servicios externos ni almacenamiento de archivos. La capa Java existente permanece intacta.

## Flujos

El selector y drag & drop comparten validación de un solo archivo .rb/.txt, sin distinguir mayúsculas, y lectura UTF-8 mediante FileReader. El contenido va al textarea editable. Los errores conservan el código anterior y las lecturas supersedidas se descartan.

Analizar envía únicamente el texto actual mediante POST relativo a `api/analyze`, con `application/x-www-form-urlencoded;charset=UTF-8`. AnalysisServlet usa AnalysisService y devuelve JSON real; el frontend conserva el resultado y su código, valida el resumen y representa las colecciones. Durante la petición se bloquean duplicados y se indica el estado ocupado.

Los modos Léxico, Sintáctico, Semántico y Todo reutilizan el mismo resultado; cambiar de modo no realiza otra petición. Editar o cargar nuevo código elimina los resultados anteriores. Errores HTTP, de red o de JSON tienen un mensaje integrado y permiten reintentar.

Limpiar restablece editor, archivo, mensajes, resumen, detalle y modo Todo, limpia el input y devuelve el foco al editor, sin realizar peticiones. Aborta lecturas activas y descarta respuestas HTTP antiguas mediante una versión de petición. No cancela el procesamiento ya iniciado en el servidor: esas respuestas no se aplican a la interfaz.

## UX, accesibilidad y responsive

Se retiraron las etiquetas de fase y se mantuvieron mensajes en español. Hay enlaces manuales para saltar al contenido, ir a Resultados y volver arriba; no se desplaza automáticamente al usuario. Controles con altura mínima de 44px, estados disabled legibles, foco visible, labels, radio buttons nativos, aria-live y aria-busy coherentes.

El encabezado y las acciones se reorganizan; los radios pasan a dos columnas en pantallas pequeñas. El editor reduce su padding y mantiene una altura usable. Los conteos usan grid adaptable. Las tablas tienen scroll interno horizontal/vertical, encabezados sticky, caption, scope y región enfocables. Se conservan todas las filas, incluidos EOF y los 174 tokens de alta.rb. El alfabeto y símbolos léxicos son colapsables.

Los valores arbitrarios se insertan mediante textContent/createElement/appendChild. Los invisibles tienen etiquetas visuales sin modificar el JSON. HTML/script se muestra como texto.

## Ejecución y pruebas

Abrir `C:\Ruby` en NetBeans 11.3 y ejecutar con su Tomcat ya configurado. `index.html` sigue en la raíz del WAR. Se requieren Java 11 y el Maven existente; no cambiar pom.xml.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11'
& 'C:\Program Files\NetBeans-11.3\netbeans\java\maven\bin\mvn.cmd' -B clean verify
& C:\Ruby\tests\frontend\verify.ps1
```

La suite frontend utiliza jjs de Java 11, DOM/FileReader/fetch simulados y las clases compiladas de target/classes para obtener respuestas reales de AnalysisService/AnalysisJson. Mantiene las 71 comprobaciones anteriores y agrega 12: estado inicial, reinicio y carreras asíncronas, reselección y flujos completos.

Resultados finales: 83 pruebas frontend aprobadas, 0 fallidas; 432 pruebas Java, 0 failures, 0 errors y 0 skipped. clean verify final terminó con BUILD SUCCESS. El WAR contiene los recursos actuales y ambas clases web, verificados byte por byte.

Los tres archivos oficiales se prueban por carga, envío, respuesta real, vistas y reanálisis: baja.rb (38 tokens, 3 símbolos), media.rb (73, 7), alta.rb (174, 10). Las cifras solo son expectativas de pruebas. Casos reales de error: `x = 10 ?`, `x =`, `puts nombre`; `?` produce categorías léxica y sintáctica. También se comprueban HTTP/red, JSON inválido, arrays vacíos, XSS y texto largo.

## Límites y entrega

Las verificaciones frontend son lógicas, no una prueba de navegador real ni una petición HTTP desplegada en Tomcat. Antes de la presentación, comprobar manualmente en NetBeans/Tomcat: entrada principal, selector/drop, modo Todo con alta.rb, reinicio durante carga/análisis y scroll/foco a aproximadamente 1440, 1024, 768 y 375px. Las reglas CSS se revisaron para esas disposiciones; no se certifica visualmente su renderizado ni cumplimiento completo de WCAG.

Nashorn/jjs emite una advertencia de futura retirada; forma parte del Java 11 fijado para el proyecto. El log del fallo interno simulado en las pruebas Java es esperado y no se expone al navegador. El AST no se representa. Los archivos locales del usuario nunca se sobrescriben.

Se conservan pom.xml, AnalysisService, lexer/parser/ast/semantic/service, AnalysisServlet, AnalysisJson y todas las pruebas Java. No se utilizó Git. No se reemplaza la documentación de fases anteriores.
