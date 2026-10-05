# Integración web — Fase 3

## Contrato HTTP

El Servlet se registra por `@WebServlet` en `/api/analyze`. Vive en el mismo WAR que `index.html`; JavaScript usa la ruta relativa `api/analyze`, sin fijar puerto ni contexto y sin configurar CORS.

- Método: POST.
- Entrada: `application/x-www-form-urlencoded;charset=UTF-8`.
- Único parámetro requerido: `codigoRuby`, texto codificado como componente de formulario.
- Salida: `application/json;charset=UTF-8`, con `Cache-Control: no-store`.
- No se envía el archivo ni su nombre: se envía el contenido actual del textarea, incluidas las modificaciones manuales.

`AnalysisServlet` establece UTF-8 antes de leer parámetros y utiliza exclusivamente `new AnalysisService().analyze(codigoRuby)`. El servicio siempre ejecuta todas las etapas. El selector permanece en JavaScript (`analysisState.type`) para la futura vista detallada; no modifica el análisis.

## Estados HTTP

| Caso | Estado | Respuesta |
| --- | --- | --- |
| Código válido o con errores Ruby | 200 | Resultado real de AnalysisService |
| `codigoRuby=` vacío | 200 | Se analiza con la conducta natural del servicio |
| Parámetro ausente | 400 | Objeto con `error` |
| Método diferente de POST | 405 | Objeto con `error`, cabecera `Allow: POST` |
| Tipo de entrada diferente al formulario indicado | 415 | Objeto con `error` |
| Fallo interno de ejecución | 500 | Mensaje genérico; detalles solo en el log del servidor |

Un error Ruby no equivale a un fallo HTTP: produce 200, `valid: false` y los diagnósticos reales.

## JSON de análisis

Campos de resumen: `valid` (booleano), `lexicalErrorCount`, `syntaxErrorCount`, `semanticErrorCount`, `totalErrorCount` (números enteros).

El total suma las tres categorías, sin volver a contar los errores de etapas previas conservados en los DTOs posteriores.

La respuesta también conserva estos datos reales para la fase 4:

- `tokens`: objetos con `type`, `lexeme`, `position`.
- `lexicalErrors`: objetos con `message`, `lexeme`, `position`.
- `syntaxErrors`: objetos con `message` y `token` (incluye posición).
- `semanticErrors`: objetos con `message`, `position`.
- `semanticSymbols`: objetos con `name`, `kind`, `type`, `scope`, `arity`, `position`.
- `alphabet`: cadenas distintas observadas en el código.
- `lexicalSymbols`: objetos con `value`, `position` para cada ocurrencia.

Las posiciones contienen `line` y `column`, empezando en 1. Los enums se serializan por su nombre. Se incluyen NEWLINE y EOF de los resultados reales. No se convierte ningún objeto Java a JSON mediante `toString()`.

`AnalysisJson` escapa comillas, backslash, LF, CR, tabulación, backspace, form feed y los demás controles U+0000 a U+001F; también escapa unidades UTF-16 surrogate. La serialización del AST queda para una fase posterior. La interfaz de esta fase solo muestra estado y conteos, sin tablas ni diagnósticos detallados.

Los errores HTTP tienen únicamente la estructura `{ "error": "mensaje" }` y no representan un análisis Ruby.

## Comportamiento del frontend

El botón se habilita con contenido que no sea solo espacio en blanco. Durante la petición muestra `Analizando…` y bloquea peticiones simultáneas. Recupera su estado al completar o fallar.

JavaScript valida los campos del resumen antes de mostrarlos y conserva el JSON completo junto con el código enviado en `analysisState`. Editar o cargar otro archivo oculta el resumen anterior. Si el código cambia mientras se espera la respuesta, el resultado se conserva asociado al código enviado pero no se muestra como resultado del código nuevo; se solicita analizar nuevamente.

Los fallos HTTP, de red, de lectura JSON o de estructura JSON muestran un mensaje integrado de error, sin valores inventados. El archivo original nunca se modifica.

## Pruebas reproducibles

Desde PowerShell en `C:\Ruby`:

```powershell
& C:\Ruby\tests\frontend\verify.ps1
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11'
& 'C:\Program Files\NetBeans-11.3\netbeans\java\maven\bin\mvn.cmd' -B -Dtest=AnalysisServletTest test
& 'C:\Program Files\NetBeans-11.3\netbeans\java\maven\bin\mvn.cmd' -B clean verify
```

Frontend: DOM, FileReader y fetch simulados con jjs de Java 11; no representa una prueba de navegador real. Java: servicio real, Servlet mediante proxies estándar y validación JSON con el parser Nashorn de Java 11. No se agregan dependencias. Los fixtures oficiales son `archivos-prueba/baja.rb`, `media.rb` y `alta.rb`.

## Validación manual en NetBeans / Tomcat

1. Ejecutar el proyecto con el Tomcat ya configurado en NetBeans.
2. En la página principal, escribir código manual (`x = 1` seguido de `puts x`). Verificar que Analizar se habilita y entrega Válido con cuatro conteos en cero.
3. Cargar sucesivamente los tres archivos oficiales y analizar. Cada uno debe mostrar Válido y total cero.
4. Analizar `x = 10 ?`, `x =` y `puts variable_no_definida`: deben mostrar Con errores y conteos reales.
5. Cambiar los cuatro tipos: la selección permanece y el servicio sigue analizando todas las etapas.
6. En las herramientas de red del navegador, confirmar POST al contexto actual más `/api/analyze`, cuerpo de formulario con `codigoRuby`, JSON UTF-8 y bloqueo de peticiones duplicadas.
7. Comprobar edición/carga durante una petición y recuperación ante un fallo del servidor. No deben aparecer tablas detalladas ni datos ficticios.

La prueba Servlet no arranca Tomcat ni modifica su configuración. La validación del despliegue real corresponde a esta comprobación manual si no hay un servidor disponible en el entorno.
