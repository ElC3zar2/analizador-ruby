function clearApp() { controls['clear-button'].handlers.click(); }
function initialState() {
    assert(controls['source-code'].value === '' && controls['source-file'].value === '', 'Editor/input no limpios');
    assert(controls['file-name'].textContent === 'Ningún archivo seleccionado', 'Nombre no limpio');
    assert(!controls['file-info'].classList.contains('is-loaded'), 'Estado de archivo no limpio');
    assert(controls['upload-message'].textContent === '' && controls['analysis-message'].textContent === '', 'Mensajes no limpios');
    assert(analysisState.result === null && analysisState.code === null && analysisState.type === 'todo', 'Estado interno no reiniciado');
    assert(controls['analysis-summary'].hidden && controls['result-details'].hidden && controls['result-details'].children.length === 0, 'Resultados no ocultos');
    assert(!controls['results-placeholder'].hidden && controls['result-state'].textContent === 'Sin análisis', 'Estado inicial de resultados incorrecto');
    assert(controls['analyze-button'].disabled && controls['analyze-button'].textContent === 'Analizar', 'Botón no reiniciado');
    assert(controls['results-panel'].attributes['aria-busy'] === 'false' && controls['drop-zone'].attributes['aria-busy'] === 'false', 'Estado ocupado residual');
    radios.forEach(function(option) { assert(option.checked === (option.value === 'todo'), 'Radio no reiniciado'); });
    assert(controls['source-code'].focused, 'Foco no vuelve al editor');
}
test('Limpiar devuelve al estado inicial sin llamar al backend', function() {
    mode('semantico'); var count = requests.length; clearApp(); initialState(); assert(requests.length === count, 'Limpiar genera fetch');
    clearApp(); initialState();
});
test('Limpiar durante lectura impide callbacks tardíos de archivo', function() {
    select([file('pendiente.rb', 'NO RESTAURAR')]); var pending = latestReader();
    controls['drop-zone'].handlers.dragenter(dragEvent([])); clearApp();
    assert(pending.aborted && !controls['drop-zone'].classList.contains('is-dragging'), 'Lectura/drag no cancelados');
    pending.complete(); pending.fail(); initialState();
});
test('Limpiar durante HTTP descarta respuesta tardía y su error', function() {
    edit('puts 1'); analyzeClick(); var old = lastRequest(); clearApp();
    old.promise.resolve({ ok: true, json: function() { throw Error('Respuesta vieja no debe leerse'); } }); initialState();
    edit('puts 1'); analyzeClick(); old = lastRequest(); clearApp(); old.promise.reject(new Error('Network old')); initialState();
});
test('Respuesta antigua no desbloquea ni altera un nuevo análisis', function() {
    edit('puts 1'); analyzeClick(); var old = lastRequest(); clearApp();
    edit('puts 2'); analyzeClick();
    old.promise.resolve({ ok: true, json: function() { return realResult('puts 1'); } });
    assert(controls['analyze-button'].disabled && controls['results-panel'].attributes['aria-busy'] === 'true', 'Petición vieja desbloquea nueva');
    respond(realResult('puts 2')); assert(analysisState.code === 'puts 2' && !controls['analyze-button'].disabled, 'Respuesta actual no visible');
});
test('Limpiar también invalida una lectura JSON todavía pendiente', function() {
    edit('puts 1'); analyzeClick(); var json = new Deferred();
    lastRequest().promise.resolve({ ok: true, json: function() { return json; } });
    clearApp(); json.resolve(realResult('puts 1')); initialState();
});
test('Después de limpiar puede seleccionarse otra vez el mismo archivo', function() {
    var source = file('mismo.rb', 'puts 1'); select([source]); latestReader().complete(); clearApp();
    select([source]); latestReader().complete(); loaded(source);
});
[['baja.rb', 38, 3], ['media.rb', 73, 7], ['alta.rb', 174, 10]].forEach(function(fixture) {
    test('Flujo completo ' + fixture[0] + ': carga, Todo, modo, edición y nuevo análisis', function() {
        clearApp(); var code = readUtf8(root + '/archivos-prueba/' + fixture[0]);
        var source = file(fixture[0], code); select([source]); latestReader().complete(); loaded(source);
        mode('todo'); analyzeClick();
        assert(decodeURIComponent(lastRequest().options.body.substring(11)) === code, 'Texto del archivo no enviado íntegro');
        respond(realResult(code));
        assert(!controls['analysis-summary'].hidden && !detailSections.lexico.hidden && !detailSections.sintactico.hidden && !detailSections.semantico.hidden, 'Vista Todo incompleta');
        assert(rows(detailSections.lexico, 'Tokens').length === fixture[1] && rows(detailSections.semantico, 'Símbolos semánticos').length === fixture[2], 'Métricas oficiales incorrectas');
        var count = requests.length; mode('lexico');
        assert(requests.length === count && !detailSections.lexico.hidden && detailSections.sintactico.hidden && detailSections.semantico.hidden, 'Cambio de modo genera petición');
        edit('puts nombre'); assert(analysisState.result === null && controls['result-details'].hidden, 'Edición no invalida');
        analyzeClick(); respond(realResult('puts nombre')); mode('semantico');
        assert(controls['result-valid'].textContent === 'Con errores' && rows(detailSections.semantico, 'Errores semánticos').length === 1, 'Nuevo diagnóstico no visible');
    });
});
test('Flujo de drag & drop .txt con errores combinados reales', function() {
    clearApp(); var source = file('combinado.txt', '?'); drop([source]); latestReader().complete();
    mode('todo'); analyzeClick(); respond(realResult(source.content));
    assert(analysisState.result.lexicalErrorCount > 0 && analysisState.result.syntaxErrorCount > 0, 'Caso no combinado');
    assert(rows(detailSections.lexico, 'Errores léxicos').length > 0 && rows(detailSections.sintactico, 'Errores sintácticos').length > 0, 'Diagnósticos combinados ausentes');
});
test('Limpiar conserva datos originales y elimina texto de resumen anterior', function() {
    var source = file('original.rb', 'x = 1'); select([source]); latestReader().complete(); analyzeClick(); respond(realResult(source.content));
    var original = analysisState.result; var snapshot = JSON.stringify(original); clearApp(); initialState();
    assert(source.content === 'x = 1' && JSON.stringify(original) === snapshot, 'Limpiar alteró archivo/datos originales');
    ['result-valid', 'result-lexical', 'result-syntax', 'result-semantic', 'result-total'].forEach(function(id) { assert(controls[id].textContent === '', 'Resumen viejo almacenado en DOM'); });
});
