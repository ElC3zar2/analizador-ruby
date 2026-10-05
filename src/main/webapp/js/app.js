'use strict';

// Lectura local del archivo e integración HTTP mediante AnalysisService.
const dropZone = document.getElementById('drop-zone');
const fileInput = document.getElementById('source-file');
const selectFileButton = document.getElementById('select-file');
const editor = document.getElementById('source-code');
const fileName = document.getElementById('file-name');
const fileInfo = document.getElementById('file-info');
const uploadMessage = document.getElementById('upload-message');
let dragDepth = 0;
let activeReader = null;
let loadVersion = 0;

function showUploadMessage(message, state) {
    uploadMessage.textContent = message;
    uploadMessage.setAttribute('data-state', state);
}

// Selector y drop comparten la validación y la lectura del archivo.
function loadFiles(files) {
    if (!files || files.length === 0) {
        return;
    }

    // Invalida los callbacks anteriores antes de abortar una lectura pendiente.
    const version = ++loadVersion;
    if (activeReader && activeReader.readyState === FileReader.LOADING) {
        activeReader.abort();
    }
    activeReader = null;
    dropZone.setAttribute('aria-busy', 'false');

    if (files.length !== 1) {
        showUploadMessage('Selecciona un solo archivo .rb o .txt a la vez.', 'error');
        return;
    }

    const file = files[0];
    if (!/\.(rb|txt)$/i.test(file.name)) {
        showUploadMessage('Formato no permitido. Selecciona un archivo .rb o .txt.', 'error');
        return;
    }

    showUploadMessage('Leyendo archivo…', 'loading');
    dropZone.setAttribute('aria-busy', 'true');

    function readingFailed() {
        if (version !== loadVersion) {
            return;
        }
        activeReader = null;
        dropZone.setAttribute('aria-busy', 'false');
        showUploadMessage('No fue posible leer el archivo seleccionado.', 'error');
    }

    try {
        const reader = new FileReader();
        activeReader = reader;
        reader.onload = () => {
            if (version !== loadVersion) {
                return;
            }
            editor.value = reader.result;
            codeChanged();
            fileName.textContent = 'Archivo seleccionado: ' + file.name;
            fileInfo.classList.add('is-loaded');
            activeReader = null;
            dropZone.setAttribute('aria-busy', 'false');
            showUploadMessage(reader.result.length === 0
                ? 'Archivo vacío cargado. Puedes escribir código en el editor.'
                : 'Archivo cargado. Puedes editar su contenido.', 'success');
        };
        reader.onerror = readingFailed;
        reader.onabort = readingFailed;
        reader.readAsText(file, 'UTF-8');
    } catch (error) {
        readingFailed();
    }
}

selectFileButton.addEventListener('click', () => fileInput.click());

fileInput.addEventListener('change', () => {
    const files = Array.prototype.slice.call(fileInput.files || []);
    // Permite elegir nuevamente el mismo archivo, incluso después de un error.
    fileInput.value = '';
    loadFiles(files);
});

dropZone.addEventListener('dragenter', (event) => {
    event.preventDefault();
    dragDepth += 1;
    dropZone.classList.add('is-dragging');
});

dropZone.addEventListener('dragover', (event) => {
    event.preventDefault();
});

dropZone.addEventListener('dragleave', () => {
    dragDepth = Math.max(0, dragDepth - 1);
    if (dragDepth === 0) {
        dropZone.classList.remove('is-dragging');
    }
});

dropZone.addEventListener('drop', (event) => {
    event.preventDefault();
    dragDepth = 0;
    dropZone.classList.remove('is-dragging');
    loadFiles(event.dataTransfer ? event.dataTransfer.files : []);
});

// Evita que soltar un archivo fuera de la zona navegue fuera de la aplicación.
document.addEventListener('dragover', (event) => event.preventDefault());
document.addEventListener('drop', (event) => {
    event.preventDefault();
    dragDepth = 0;
    dropZone.classList.remove('is-dragging');
});

const analyzeButton = document.getElementById('analyze-button');
const analysisMessage = document.getElementById('analysis-message');
const resultsPlaceholder = document.getElementById('results-placeholder');
const analysisSummary = document.getElementById('analysis-summary');
const resultState = document.getElementById('result-state');
const resultValid = document.getElementById('result-valid');
const resultLexical = document.getElementById('result-lexical');
const resultSyntax = document.getElementById('result-syntax');
const resultSemantic = document.getElementById('result-semantic');
const resultTotal = document.getElementById('result-total');
const analysisOptions = document.querySelectorAll('input[name="analysis-type"]');
// Conserva JSON completo y el código al que corresponde para la futura vista detallada.
const analysisState = { result: null, code: null, type: 'todo' };
let analysisPending = false;

function updateAnalyzeButton() {
    analyzeButton.disabled = analysisPending || editor.value.trim().length === 0;
    analyzeButton.textContent = analysisPending ? 'Analizando…' : 'Analizar';
}

function showAnalysisMessage(message, state) {
    analysisMessage.textContent = message;
    analysisMessage.setAttribute('data-state', state);
}

function codeChanged() {
    analysisState.result = null;
    analysisState.code = null;
    analysisSummary.hidden = true;
    resultsPlaceholder.hidden = false;
    resultState.textContent = 'Sin análisis';
    showAnalysisMessage(analysisPending ? 'El código cambió. Vuelve a analizarlo al terminar.' : '', 'idle');
    updateAnalyzeButton();
}

function validSummary(result) {
    const counts = ['lexicalErrorCount', 'syntaxErrorCount', 'semanticErrorCount', 'totalErrorCount'];
    return result && typeof result.valid === 'boolean'
        && counts.every((name) => typeof result[name] === 'number'
            && isFinite(result[name]) && result[name] >= 0 && Math.floor(result[name]) === result[name])
        && result.totalErrorCount === result.lexicalErrorCount + result.syntaxErrorCount + result.semanticErrorCount
        && result.valid === (result.totalErrorCount === 0);
}

editor.addEventListener('input', codeChanged);
Array.prototype.forEach.call(analysisOptions, (option) => {
    if (option.checked) {
        analysisState.type = option.value;
    }
    option.addEventListener('change', () => {
        if (option.checked) {
            analysisState.type = option.value;
        }
    });
});

analyzeButton.addEventListener('click', () => {
    if (analysisPending || editor.value.trim().length === 0) {
        return;
    }
    const codigoRuby = editor.value;
    analysisPending = true;
    analysisState.result = null;
    analysisState.code = null;
    analysisSummary.hidden = true;
    resultsPlaceholder.hidden = true;
    resultState.textContent = 'En curso';
    showAnalysisMessage('Analizando…', 'loading');
    updateAnalyzeButton();

    function finishAnalysis() {
        analysisPending = false;
        updateAnalyzeButton();
    }
    function analysisFailed() {
        analysisState.result = null;
        analysisState.code = null;
        analysisSummary.hidden = true;
        resultsPlaceholder.hidden = true;
        resultState.textContent = 'No completado';
        showAnalysisMessage('No fue posible completar el análisis. Inténtalo nuevamente.', 'error');
    }

    try {
        // URL relativa al directorio de index.html: conserva el contexto WAR y el puerto.
        fetch('api/analyze', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8', 'Accept': 'application/json' },
            body: 'codigoRuby=' + encodeURIComponent(codigoRuby)
        }).then((response) => {
            if (!response.ok) {
                throw new Error('Respuesta HTTP no exitosa');
            }
            return response.json();
        }).then((result) => {
            if (!validSummary(result)) {
                throw new Error('Resumen JSON inválido');
            }
            analysisState.result = result;
            analysisState.code = codigoRuby;
            if (editor.value !== codigoRuby) {
                resultState.textContent = 'Código modificado';
                showAnalysisMessage('El código cambió durante el análisis. Vuelve a analizarlo.', 'idle');
                return;
            }
            resultValid.textContent = result.valid ? 'Válido' : 'Con errores';
            resultLexical.textContent = String(result.lexicalErrorCount);
            resultSyntax.textContent = String(result.syntaxErrorCount);
            resultSemantic.textContent = String(result.semanticErrorCount);
            resultTotal.textContent = String(result.totalErrorCount);
            resultState.textContent = result.valid ? 'Válido' : 'Con errores';
            analysisSummary.hidden = false;
            showAnalysisMessage('Análisis completado', 'success');
        }).catch(analysisFailed).then(finishAnalysis);
    } catch (error) {
        // También cubre un fallo síncrono antes de crear la promesa de fetch.
        analysisFailed();
        finishAnalysis();
    }
});

updateAnalyzeButton();