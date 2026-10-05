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
['drop-zone', 'source-file', 'select-file', 'source-code', 'file-name', 'file-info', 'upload-message', 'analyze-button', 'analysis-message', 'results-placeholder', 'analysis-summary', 'result-state', 'result-valid', 'result-lexical', 'result-syntax', 'result-semantic', 'result-total'].forEach(function(id) {
    controls[id] = mockElement();
});
var radios = ['lexico', 'sintactico', 'semantico', 'todo'].map(function(type) {
    var option = mockElement(); option.value = type; option.checked = type === 'todo'; return option;
});
var document = {
    querySelectorAll: function(selector) {
        assert(selector === 'input[name="analysis-type"]', 'Selector inesperado');
        return radios;
    },
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
// Promesas controladas: permiten completar/rechazar fetch sin Node ni librerías.
function Deferred() { this.state = 'pending'; this.callbacks = []; }
Deferred.prototype.settle = function(state, value) {
    if (this.state !== 'pending') return;
    this.state = state; this.value = value;
    var callbacks = this.callbacks.splice(0);
    for (var i = 0; i < callbacks.length; i++) callbacks[i]();
};
Deferred.prototype.resolve = function(value) {
    var self = this;
    if (value && typeof value.then === 'function') {
        value.then(function(result) { self.resolve(result); }, function(error) { self.reject(error); });
    } else { this.settle('resolved', value); }
};
Deferred.prototype.reject = function(error) { this.settle('rejected', error); };
Deferred.prototype.then = function(success, failure) {
    var parent = this; var next = new Deferred();
    function complete() {
        var callback = parent.state === 'resolved' ? success : failure;
        if (!callback) {
            if (parent.state === 'resolved') next.resolve(parent.value); else next.reject(parent.value);
            return;
        }
        try { next.resolve(callback(parent.value)); } catch (error) { next.reject(error); }
    }
    if (parent.state === 'pending') parent.callbacks.push(complete); else complete();
    return next;
};
Deferred.prototype.catch = function(failure) { return this.then(null, failure); };
var requests = [];
var synchronousFetchFailure = false;
function fetch(url, options) {
    if (synchronousFetchFailure) throw new Error('fetch unavailable');
    var promise = new Deferred(); requests.push({ url: url, options: options, promise: promise }); return promise;
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


function edit(code) { controls['source-code'].value = code; controls['source-code'].handlers.input(); }
function analyzeClick() { controls['analyze-button'].handlers.click(); }
function lastRequest() { return requests[requests.length - 1]; }
function summary(valid, lexical, syntax, semantic) {
    return { valid: valid, lexicalErrorCount: lexical, syntaxErrorCount: syntax, semanticErrorCount: semantic, totalErrorCount: lexical + syntax + semantic, tokens: [{ type: 'EOF', lexeme: '' }] };
}
function respond(result) {
    lastRequest().promise.resolve({ ok: true, json: function() { var value = new Deferred(); value.resolve(result); return value; } });
}
function assertAnalysisError() {
    assert(controls['analysis-message'].attributes['data-state'] === 'error', 'Mensaje HTTP/red ausente');
    assert(controls['analysis-message'].textContent.indexOf('No fue posible completar') !== -1, 'Mensaje no amigable');
    assert(controls['analysis-summary'].hidden, 'Se muestra resumen ficticio o anterior');
    assert(analysisState.result === null, 'Error conserva respuesta como resultado válido');
    assert(!controls['analyze-button'].disabled, 'Botón no restaurado');
}
test('Botón según contenido manual, vacío o espacios', function() {
    edit(''); assert(controls['analyze-button'].disabled, 'Vacío habilita botón');
    edit(' \n\t'); assert(controls['analyze-button'].disabled, 'Espacios habilitan botón');
    var count = requests.length; analyzeClick(); assert(requests.length === count, 'Vacío genera petición');
    edit('puts 1'); assert(!controls['analyze-button'].disabled, 'Código manual no habilita botón');
});
test('Archivo cargado actualiza el estado del botón', function() {
    select([file('activo.rb', 'puts 1')]); latestReader().complete();
    assert(!controls['analyze-button'].disabled, 'Archivo no habilita botón');
    select([file('vacio.txt', '')]); latestReader().complete();
    assert(controls['analyze-button'].disabled, 'Archivo vacío habilita botón');
});
test('Las cuatro selecciones se conservan y no cambian la petición al servicio', function() {
    radios.forEach(function(option) {
        radios.forEach(function(other) { other.checked = other === option; });
        option.handlers.change(); edit('puts 1'); analyzeClick(); respond(summary(true, 0, 0, 0));
        assert(option.checked && analysisState.type === option.value, 'Tipo no conservado');
        assert(lastRequest().options.body === 'codigoRuby=puts%201', 'Tipo altera petición');
    });
});
test('Click envía texto exacto UTF-8 sin archivo binario a ruta relativa', function() {
    var code = '# ñ á\r\n  puts "Ruby + & = %"\n\tputs 2';
    edit(code); analyzeClick(); var request = lastRequest();
    assert(request.url === 'api/analyze' && !/^(https?:|\/)/.test(request.url), 'URL no relativa');
    assert(request.options.method === 'POST', 'Método no POST');
    assert(request.options.headers['Content-Type'] === 'application/x-www-form-urlencoded;charset=UTF-8', 'Content-Type incorrecto');
    assert(typeof request.options.body === 'string', 'Se envió objeto/archivo binario');
    assert(decodeURIComponent(request.options.body.substring('codigoRuby='.length)) === code, 'Código enviado alterado');
    respond(summary(true, 0, 0, 0));
});
test('Respuesta válida muestra estado real y conserva JSON completo', function() {
    edit('x = 1'); analyzeClick(); var result = summary(true, 0, 0, 0); respond(result);
    assert(!controls['analysis-summary'].hidden, 'Resumen oculto');
    assert(controls['result-valid'].textContent === 'Válido', 'valid=true incorrecto');
    assert(controls['analysis-message'].textContent === 'Análisis completado', 'Éxito ausente');
    assert(analysisState.result === result && analysisState.code === 'x = 1', 'Resultado/código no retenidos');
    assert(analysisState.result.tokens.length === 1, 'Detalles JSON descartados');
});
test('valid=false y conteos mostrados coinciden con respuesta', function() {
    edit('codigo con errores'); analyzeClick(); respond(summary(false, 2, 3, 4));
    assert(controls['result-valid'].textContent === 'Con errores', 'valid=false incorrecto');
    assert(controls['result-lexical'].textContent === '2' && controls['result-syntax'].textContent === '3', 'Conteos léxico/sintáctico incorrectos');
    assert(controls['result-semantic'].textContent === '4' && controls['result-total'].textContent === '9', 'Conteos semántico/total incorrectos');
});
test('No aparece un resumen antes de recibir respuesta', function() {
    edit('puts 1'); analyzeClick();
    assert(controls['analysis-summary'].hidden && analysisState.result === null, 'Resumen ficticio en carga');
    respond(summary(true, 0, 0, 0));
});
test('Error HTTP limpia resumen anterior y permite reintento', function() {
    analyzeClick(); lastRequest().promise.resolve({ ok: false, json: function() { throw Error('No debe leer JSON de error'); } }); assertAnalysisError();
});
test('Error de red manejado', function() {
    analyzeClick(); lastRequest().promise.reject(new Error('Network failure')); assertAnalysisError();
});
test('JSON inválido manejado', function() {
    analyzeClick(); lastRequest().promise.resolve({ ok: true, json: function() { var result = new Deferred(); result.reject(new SyntaxError('Invalid JSON')); return result; } }); assertAnalysisError();
});
test('Esquema JSON incompleto rechazado', function() { analyzeClick(); respond({ valid: true }); assertAnalysisError(); });
test('Conteos negativos o inconsistentes rechazados', function() {
    var invalid = [summary(false, -1, 0, 0), summary(true, 1, 0, 0), summary(false, 0, 0, 0), summary(false, 0.5, 0, 0)];
    invalid.push({ valid: false, lexicalErrorCount: 1, syntaxErrorCount: 0, semanticErrorCount: 0, totalErrorCount: 5 });
    invalid.forEach(function(result) { analyzeClick(); respond(result); assertAnalysisError(); });
});
test('Doble click bloquea duplicados y restaura botón', function() {
    var before = requests.length; analyzeClick(); analyzeClick(); analyzeClick();
    assert(requests.length === before + 1, 'Peticiones duplicadas');
    assert(controls['analyze-button'].disabled && controls['analyze-button'].textContent === 'Analizando…', 'Estado ocupado incorrecto');
    respond(summary(true, 0, 0, 0));
    assert(!controls['analyze-button'].disabled && controls['analyze-button'].textContent === 'Analizar', 'Botón no restaurado');
});
test('Fallo síncrono de fetch manejado', function() {
    synchronousFetchFailure = true; try { analyzeClick(); assertAnalysisError(); } finally { synchronousFetchFailure = false; }
});
test('Cambios durante petición no presentan resultado como código nuevo', function() {
    edit('puts 1'); analyzeClick(); edit('puts 2'); respond(summary(true, 0, 0, 0));
    assert(controls['analysis-summary'].hidden && analysisState.code === 'puts 1', 'Resumen/código antiguo mal manejado');
    assert(controls['result-state'].textContent === 'Código modificado', 'Aviso ausente');
    assert(!controls['analyze-button'].disabled, 'No permite analizar código nuevo');
});
test('Vaciar editor durante petición mantiene botón deshabilitado al terminar', function() {
    analyzeClick(); edit(''); respond(summary(true, 0, 0, 0)); assert(controls['analyze-button'].disabled, 'Botón habilitado para vacío');
});
test('Editar o cargar otro archivo invalida resumen anterior', function() {
    edit('puts 1'); analyzeClick(); respond(summary(true, 0, 0, 0));
    edit('puts 2'); assert(controls['analysis-summary'].hidden && analysisState.result === null, 'Edición conserva resumen viejo');
    analyzeClick(); respond(summary(true, 0, 0, 0));
    select([file('nuevo.rb', 'puts 3')]); latestReader().complete();
    assert(controls['analysis-summary'].hidden && analysisState.result === null, 'Carga conserva resumen viejo');
});
print('TOTAL: ' + passed + ' pruebas frontend aprobadas (DOM, FileReader y fetch simulados).');