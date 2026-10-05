// Ejecutar con tests/frontend/verify.ps1. DOM y FileReader simulados, sin dependencias.
var root = String($ARG[0]).replace(/\\/g, '/');
var Files = Java.type('java.nio.file.Files');
var Paths = Java.type('java.nio.file.Paths');
var StandardCharsets = Java.type('java.nio.charset.StandardCharsets');
var JavaString = Java.type('java.lang.String');
function readUtf8(path) {
    return String(new JavaString(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8));
}
function assert(condition, message) {
    if (!condition) throw new Error(message);
}
var passed = 0;
function test(name, callback) {
    callback();
    passed++;
    print('PASS: ' + name);
}
function mockElement() {
    var classes = {};
    return {
        value: '', textContent: '', files: [], attributes: {}, handlers: {}, clicks: 0,
        classList: {
            add: function(name) { classes[name] = true; },
            remove: function(name) { delete classes[name]; },
            contains: function(name) { return !!classes[name]; }
        },
        setAttribute: function(name, value) { this.attributes[name] = value; },
        addEventListener: function(name, callback) { this.handlers[name] = callback; },
        click: function() { this.clicks++; }
    };
}
var controls = {};
['drop-zone', 'source-file', 'select-file', 'source-code', 'file-name', 'file-info', 'upload-message'].forEach(function(id) {
    controls[id] = mockElement();
});
var document = {
    handlers: {},
    getElementById: function(id) {
        assert(!!controls[id], 'Referencia a un control desconocido: ' + id);
        return controls[id];
    },
    addEventListener: function(name, callback) { this.handlers[name] = callback; }
};
var readers = [];
var failConstructor = false;
function FileReader() {
    if (failConstructor) throw new Error('Constructor failure');
    this.readyState = 0;
    this.result = null;
    readers.push(this);
}
FileReader.LOADING = 1;
FileReader.prototype.readAsText = function(file, encoding) {
    assert(encoding === 'UTF-8', 'Lectura debe usar UTF-8');
    if (file.failRead) throw new Error('readAsText failure');
    this.file = file;
    this.readyState = 1;
};
FileReader.prototype.abort = function() {
    this.readyState = 2;
    this.aborted = true;
    if (this.onabort) this.onabort();
};
FileReader.prototype.complete = function() {
    // Permite entregar un callback tardío para comprobar la protección de concurrencia.
    this.result = this.file.content;
    this.readyState = 2;
    this.onload();
};
FileReader.prototype.fail = function() {
    this.readyState = 2;
    this.onerror();
};
function latestReader() { return readers[readers.length - 1]; }
function file(name, content) { return { name: name, content: content, type: '', size: content.length }; }
function select(files) {
    controls['source-file'].files = files;
    controls['source-file'].value = 'C:\\fakepath\\archivo.rb';
    controls['source-file'].handlers.change();
    assert(controls['source-file'].value === '', 'Input debe permitir reselección');
}
function dragEvent(files) {
    return {
        prevented: false,
        dataTransfer: { files: files },
        preventDefault: function() { this.prevented = true; }
    };
}
function drop(files) {
    var event = dragEvent(files);
    controls['drop-zone'].handlers.drop(event);
    document.handlers.drop(event); // Propagación normal al documento.
    assert(event.prevented, 'Drop debe impedir navegación');
    assert(!controls['drop-zone'].classList.contains('is-dragging'), 'Drop debe limpiar resaltado');
}
function loaded(expectedFile) {
    assert(controls['source-code'].value === expectedFile.content, 'Contenido alterado: ' + expectedFile.name);
    assert(controls['file-name'].textContent === 'Archivo seleccionado: ' + expectedFile.name, 'Nombre incorrecto');
    assert(controls['file-info'].classList.contains('is-loaded'), 'Estado seleccionado ausente');
    assert(controls['upload-message'].attributes['data-state'] === 'success', 'Mensaje de éxito ausente');
    assert(controls['drop-zone'].attributes['aria-busy'] === 'false', 'Estado ocupado no resuelto');
}
function errorPreserves(action) {
    var previousCode = controls['source-code'].value;
    var previousName = controls['file-name'].textContent;
    action();
    assert(controls['source-code'].value === previousCode, 'Error sobrescribe el editor');
    assert(controls['file-name'].textContent === previousName, 'Error sobrescribe el nombre');
    assert(controls['upload-message'].attributes['data-state'] === 'error', 'Mensaje de error ausente');
    assert(controls['upload-message'].textContent.length > 0, 'Error debe ser visible');
    assert(controls['drop-zone'].attributes['aria-busy'] === 'false', 'Error deja carga pendiente');
}
load(root + '/src/main/webapp/js/app.js');

test('Botón abre el selector', function() {
    controls['select-file'].handlers.click();
    assert(controls['source-file'].clicks === 1, 'Selector no abierto');
});
test('Selección .rb y contenido UTF-8 íntegro', function() {
    var source = file('programa.rb', '# Español: áéíóú ñ ◆\n  puts "hola"\n\tputs "Ruby"\n');
    select([source]);
    assert(controls['upload-message'].attributes['data-state'] === 'loading', 'Estado de lectura ausente');
    assert(controls['drop-zone'].attributes['aria-busy'] === 'true', 'aria-busy ausente');
    latestReader().complete();
    loaded(source);
});
test('Selección .txt', function() {
    var source = file('programa.txt', 'puts "texto"\n');
    select([source]); latestReader().complete(); loaded(source);
});
['rb', 'txt'].forEach(function(extension) {
    test('Drag & drop .' + extension, function() {
        var source = file('drop.' + extension, '  puts "drop"\n');
        drop([source]); latestReader().complete(); loaded(source);
    });
});
test('Efectos dragenter/dragover/dragleave y elementos anidados', function() {
    var event = dragEvent([]);
    controls['drop-zone'].handlers.dragenter(event);
    controls['drop-zone'].handlers.dragenter(event);
    controls['drop-zone'].handlers.dragover(event);
    assert(event.prevented, 'Dragover debe impedir navegación');
    controls['drop-zone'].handlers.dragleave();
    assert(controls['drop-zone'].classList.contains('is-dragging'), 'Salida de hijo no debe quitar resaltado');
    controls['drop-zone'].handlers.dragleave();
    controls['drop-zone'].handlers.dragleave();
    assert(!controls['drop-zone'].classList.contains('is-dragging'), 'Salida no limpia resaltado');
});
test('Extensiones inválidas rechazadas sin lectura ni sobrescritura', function() {
    ['pdf', 'docx', 'java', 'js', 'exe', 'png', 'rb.exe', ''].forEach(function(extension) {
        var count = readers.length;
        var source = file('prueba.' + extension, 'INVALIDO');
        errorPreserves(function() { select([source]); });
        errorPreserves(function() { drop([source]); });
        assert(readers.length === count, 'Archivo inválido leído');
    });
});
['RB', 'TXT'].forEach(function(extension) {
    test('Extensión mayúscula .' + extension, function() {
        var source = file('PRUEBA.' + extension, 'puts "mayúsculas"\n');
        select([source]); latestReader().complete(); loaded(source);
    });
});
test('Reemplazo A por B limpia errores previos', function() {
    select([file('A.rb', 'A')]); latestReader().complete();
    errorPreserves(function() { select([file('error.pdf', 'error')]); });
    var replacement = file('B.txt', 'B');
    drop([replacement]); latestReader().complete(); loaded(replacement);
});
test('Archivo vacío válido', function() {
    var source = file('vacio.rb', '');
    select([source]); latestReader().complete(); loaded(source);
    assert(controls['upload-message'].textContent.indexOf('vacío') !== -1, 'Aviso informativo ausente');
});
test('Error asíncrono de FileReader conserva archivo y editor', function() {
    errorPreserves(function() { select([file('ilegible.rb', 'no cargar')]); latestReader().fail(); });
});
test('Excepción de readAsText manejada', function() {
    var source = file('error.txt', 'no cargar'); source.failRead = true;
    errorPreserves(function() { select([source]); });
});
test('Excepción al crear FileReader manejada', function() {
    failConstructor = true;
    try { errorPreserves(function() { select([file('error.rb', 'no cargar')]); }); }
    finally { failConstructor = false; }
});
test('Cancelación de lectura manejada', function() {
    errorPreserves(function() { select([file('abort.rb', 'no cargar')]); latestReader().abort(); });
});
test('Editor permite modificaciones locales sin modificar el archivo', function() {
    var source = file('original.rb', 'puts "original"');
    select([source]); latestReader().complete();
    controls['source-code'].value = 'puts "editado"';
    assert(source.content === 'puts "original"', 'Archivo original modificado');
    assert(controls['source-code'].value === 'puts "editado"', 'Editor no editable');
});
test('Reseleccionar el mismo archivo vuelve a leerlo', function() {
    var source = file('mismo.rb', 'original');
    select([source]); latestReader().complete();
    controls['source-code'].value = 'editado';
    select([source]); latestReader().complete(); loaded(source);
});
test('Cancelar selector o drop sin archivos conserva estado', function() {
    var count = readers.length;
    var before = controls['source-code'].value;
    var message = controls['upload-message'].textContent;
    select([]); drop([]);
    controls['drop-zone'].handlers.drop({ preventDefault: function() {} });
    assert(readers.length === count && controls['source-code'].value === before, 'Cancelación cambia editor');
    assert(controls['upload-message'].textContent === message, 'Cancelación cambia mensaje');
});
test('Múltiples archivos rechazados sin lectura', function() {
    var count = readers.length;
    errorPreserves(function() { drop([file('A.rb', 'A'), file('B.rb', 'B')]); });
    assert(readers.length === count, 'Se leyó una carga múltiple');
});
test('Última selección prevalece ante callbacks tardíos', function() {
    select([file('lento.rb', 'ANTERIOR')]);
    var first = latestReader();
    var last = file('ultimo.txt', 'ULTIMO');
    select([last]);
    assert(first.aborted, 'No abortó la lectura anterior');
    latestReader().complete(); loaded(last);
    first.complete(); loaded(last);
    first.fail(); loaded(last);
});
test('Intento inválido cancela lectura pendiente sin sobrescribir editor', function() {
    select([file('pendiente.rb', 'NO CARGAR')]);
    var pending = latestReader();
    errorPreserves(function() { drop([file('error.pdf', 'error')]); });
    var before = controls['source-code'].value;
    pending.complete();
    assert(controls['source-code'].value === before, 'Callback invalidado sobrescribe editor');
    assert(controls['upload-message'].attributes['data-state'] === 'error', 'Callback invalidado borra error');
});
test('Drop fuera de zona impide navegación sin leer', function() {
    var count = readers.length;
    var event = dragEvent([file('fuera.rb', 'NO CARGAR')]);
    document.handlers.dragover(event);
    document.handlers.drop(event);
    assert(event.prevented && readers.length === count, 'Drop externo procesado');
});
test('Nombre con marcado se muestra como texto', function() {
    var source = file('<img src=x onerror=alert(1)>.rb', '# texto');
    select([source]); latestReader().complete(); loaded(source);
    assert(controls['file-name'].innerHTML === undefined, 'Nombre insertado como HTML');
});
['baja.rb', 'media.rb', 'alta.rb'].forEach(function(name) {
    test('Archivo real ' + name + ' íntegro por selector y drop', function() {
        var source = file(name, readUtf8(root + '/archivos-prueba/' + name));
        select([source]); latestReader().complete(); loaded(source);
        drop([source]); latestReader().complete(); loaded(source);
    });
});
test('Asignación conserva espacios, indentación y saltos de línea', function() {
    var source = file('lineas.txt', '  uno\r\n\tdos\n\n tres  \r\n');
    select([source]); latestReader().complete(); loaded(source);
});
print('TOTAL: ' + passed + ' pruebas de comportamiento aprobadas (DOM y FileReader simulados).');
