'use strict';

// Fase 1: comportamiento visual. No se leen ni procesan archivos.
const dropZone = document.getElementById('drop-zone');
const fileInput = document.getElementById('source-file');
const selectFileButton = document.getElementById('select-file');
let dragDepth = 0;

selectFileButton.addEventListener('click', () => fileInput.click());

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

// Evita que soltar un archivo navegue fuera de la aplicación.
document.addEventListener('dragover', (event) => event.preventDefault());
document.addEventListener('drop', (event) => {
    event.preventDefault();
    dragDepth = 0;
    dropZone.classList.remove('is-dragging');
});
