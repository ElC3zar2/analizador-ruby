'use strict';

// Carga local de texto: no envía código ni ejecuta análisis.
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
