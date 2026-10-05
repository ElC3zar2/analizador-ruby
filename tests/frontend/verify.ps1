param(
    [string]$JdkHome = 'C:\Program Files\Java\jdk-11'
)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$htmlPath = Join-Path $projectRoot 'src/main/webapp/index.html'
$html = Get-Content -LiteralPath $htmlPath -Raw -Encoding utf8
# Normaliza atributos booleanos y elementos vacíos para comprobar anidación con XML.
# Es una verificación estructural local, no un validador completo de HTML5.
$normalized = $html -replace '<!DOCTYPE html>', '' -replace '<(meta|link|input)(\s[^>]*?)>', '<$1$2 />' -replace '\schecked(?=[\s/>])', ' checked="checked"' -replace '\sdisabled(?=[\s/>])', ' disabled="disabled"' -replace '\shidden(?=[\s/>])', ' hidden="hidden"' -replace '\sdefer(?=[\s/>])', ' defer="defer"'
$doc = [xml]$normalized
if ($doc.html.lang -ne 'es' -or $doc.html.head.meta[0].charset -ne 'UTF-8') { throw 'Metadatos HTML incorrectos.' }
if ($html.Contains('Hello World!')) { throw 'Página inicial antigua presente.' }
$ids = @($doc.SelectNodes('//*[@id]') | ForEach-Object { $_.id })
if (@($ids | Select-Object -Unique).Count -ne $ids.Count) { throw 'IDs duplicados.' }
foreach ($element in $doc.SelectNodes('//*[@for or @aria-labelledby or @aria-describedby]')) {
    foreach ($attribute in 'for','aria-labelledby','aria-describedby') {
        foreach ($reference in ($element.GetAttribute($attribute) -split ' ' | Where-Object { $_ })) {
            if ($reference -notin $ids) { throw "Referencia accesible rota: $reference" }
        }
    }
}
foreach ($resource in @($doc.html.head.link.href, $doc.html.head.script.src)) {
    if (!(Test-Path -LiteralPath (Join-Path $projectRoot "src/main/webapp/$resource"))) { throw "Recurso ausente: $resource" }
}
if ($doc.html.head.link.href -ne 'css/styles.css' -or $doc.html.head.script.src -ne 'js/app.js') { throw 'Referencias de recursos inesperadas.' }
if ($doc.SelectSingleNode('//input[@type="file"]').accept -ne '.rb,.txt') { throw 'Formatos accept incorrectos.' }
if ($doc.SelectNodes('//input[@type="radio"]').Count -ne 4 -or $doc.SelectSingleNode('//input[@checked]').value -ne 'todo') { throw 'Opciones de análisis incorrectas.' }
$textarea = $doc.SelectSingleNode('//textarea[@id="source-code"]')
if (!$textarea -or $textarea.HasAttribute('readonly') -or $textarea.HasAttribute('disabled')) { throw 'Editor no editable.' }
if (!$doc.SelectSingleNode('//button[@id="analyze-button" and @disabled and @aria-describedby="analysis-note"]')) { throw 'Analizar debe iniciar deshabilitado con editor vacío.' }
$message = $doc.SelectSingleNode('//*[@id="upload-message"]')
if (!$message -or $message.role -ne 'status' -or $message.'aria-live' -ne 'polite') { throw 'Mensajes accesibles ausentes.' }
$results = $doc.SelectSingleNode('//section[@aria-labelledby="results-title"]')
if ($results.SelectNodes('.//table').Count -ne 0 -or $results.InnerText -notmatch 'Los resultados del análisis aparecerán aquí\.') { throw 'Estado inicial de resultados alterado.' }
$js = Get-Content -LiteralPath (Join-Path $projectRoot 'src/main/webapp/js/app.js') -Raw -Encoding utf8
if ($js -notmatch 'fetch\(''api/analyze''' -or $js -match 'https?://|\b(XMLHttpRequest|WebSocket|EventSource|sendBeacon)\b') { throw 'Endpoint relativo incorrecto o transporte inesperado.' }
if ($js -match '\b(innerHTML|eval)\b|alert\s*\(') { throw 'Uso inseguro o alerta inesperada.' }
foreach ($match in [regex]::Matches($js, 'getElementById\(''([^'']+)''\)')) {
    if ($match.Groups[1].Value -notin $ids) { throw "Referencia JS rota: $($match.Groups[1].Value)" }
}
$css = Get-Content -LiteralPath (Join-Path $projectRoot 'src/main/webapp/css/styles.css') -Raw -Encoding utf8
if ($css -notmatch '@media \(max-width: 640px\)' -or $css -notmatch ':focus-visible' -or $css -notmatch 'upload-message\[data-state="error"\]') { throw 'Estilos responsive, focus o error ausentes.' }
if ($html -match 'style=|https?://') { throw 'Recursos externos o estilos inline inesperados.' }
if (!$doc.SelectSingleNode('//*[@id="result-details" and @hidden]')) { throw 'Detalle debe iniciar oculto.' }
if ($css -notmatch '\.table-scroll' -or $css -notmatch 'overflow: auto' -or $css -notmatch 'overflow-wrap: anywhere') { throw 'Scroll o manejo de lexemas largos ausente.' }
Write-Output 'PASS: estructura HTML, accesibilidad, recursos, editor editable, responsive y estados de error.'
Write-Output 'PASS: endpoint relativo, botón inicialmente deshabilitado y resultados sin datos ficticios.'
$jjsPath = Join-Path $JdkHome 'bin/jjs.exe'
if (!(Test-Path -LiteralPath $jjsPath)) { throw "No se encontró jjs de Java 11: $jjsPath" }
if (!(Test-Path -LiteralPath (Join-Path $projectRoot 'target/classes/com/umg/analizador/web/AnalysisJson.class'))) { throw 'Primero compila el proyecto con el Maven existente: se requieren target/classes para las respuestas reales.' }
& $jjsPath -cp (Join-Path $projectRoot 'target/classes') -scripting --language=es6 (Join-Path $PSScriptRoot 'file-loading.test.js') -- $projectRoot
if ($LASTEXITCODE -ne 0) { throw "Pruebas de comportamiento fallidas: código $LASTEXITCODE" }
