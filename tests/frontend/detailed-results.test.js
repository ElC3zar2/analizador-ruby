// Se ejecuta después de las pruebas de carga y HTTP; usa las mismas simulaciones.
var RealService = Java.type('com.umg.analizador.service.AnalysisService');
var RealJson = Java.type('com.umg.analizador.web.AnalysisJson');
function realResult(source) { return JSON.parse(String(RealJson.serialize(new RealService().analyze(source)))); }
function nodes(parent, tag) {
    var found = [];
    parent.children.forEach(function(child) {
        if (child.tagName === tag.toUpperCase()) found.push(child);
        found = found.concat(nodes(child, tag));
    });
    return found;
}
function table(section, title) {
    var tables = nodes(section, 'table');
    for (var i = 0; i < tables.length; i++) {
        if (nodes(tables[i], 'caption')[0].textContent === title) return tables[i];
    }
    throw new Error('Tabla ausente: ' + title);
}
function rows(section, title) {
    return nodes(table(section, title), 'tbody')[0].children.map(function(row) {
        return row.children.map(function(cell) { return cell.textContent; });
    });
}
function mode(type) {
    radios.forEach(function(option) { option.checked = option.value === type; if (option.checked) option.handlers.change(); });
}
function showResult(result, code) {
    edit(code || 'codigo'); analyzeClick(); respond(result);
    assert(!controls['analysis-summary'].hidden && !controls['result-details'].hidden, 'Resumen/detalle oculto');
}
function emptyResult() {
    return { valid: true, lexicalErrorCount: 0, syntaxErrorCount: 0, semanticErrorCount: 0, totalErrorCount: 0,
        tokens: [], lexicalErrors: [], syntaxErrors: [], semanticErrors: [], semanticSymbols: [], alphabet: [], lexicalSymbols: [] };
}
['lexico', 'sintactico', 'semantico', 'todo'].forEach(function(type) {
    test('Modo ' + type + ' muestra únicamente las secciones correspondientes', function() {
        mode(type); showResult(realResult('x = 1\nputs x'), 'x = 1\nputs x');
        ['lexico', 'sintactico', 'semantico'].forEach(function(key) {
            assert(detailSections[key].hidden === (type !== 'todo' && type !== key), 'Visibilidad incorrecta: ' + key);
        });
    });
});
test('Cambiar modo reutiliza nodos y resultado sin fetch adicional', function() {
    var count = requests.length; var stored = analysisState.result; var lexical = detailSections.lexico;
    ['lexico', 'sintactico', 'semantico', 'todo'].forEach(mode);
    assert(requests.length === count && analysisState.result === stored && detailSections.lexico === lexical, 'Modo repite petición/render o reemplaza resultado');
});
test('Tokens conservan orden, lexema, enum, posición y EOF', function() {
    var code = 'x = 1\nputs x'; var result = realResult(code); showResult(result, code);
    var rendered = rows(detailSections.lexico, 'Tokens');
    assert(rendered.length === result.tokens.length, 'Tokens eliminados');
    result.tokens.forEach(function(token, index) {
        assert(JSON.stringify(rendered[index]) === JSON.stringify([visibleText(token.lexeme), token.type, String(token.position.line), String(token.position.column)]), 'Token mal mapeado');
    });
    assert(rendered[rendered.length - 1][1] === 'EOF', 'EOF eliminado');
});
test('Error léxico real muestra mensaje, lexema y posición', function() {
    var result = realResult('x = 10 ?'); showResult(result, 'x = 10 ?');
    var error = result.lexicalErrors[0]; var row = rows(detailSections.lexico, 'Errores léxicos')[0];
    assert(JSON.stringify(row) === JSON.stringify([error.message, visibleText(error.lexeme), String(error.position.line), String(error.position.column)]), 'Error léxico mal mapeado');
});
test('Sin errores léxicos muestra mensaje positivo', function() {
    showResult(realResult('x = 1'), 'x = 1');
    assert(detailSections.lexico.textContent.indexOf('Sin errores léxicos.') !== -1, 'Mensaje positivo ausente');
});
test('Error sintáctico real muestra token, lexema y posición', function() {
    var result = realResult('x ='); showResult(result, 'x ='); var error = result.syntaxErrors[0];
    assert(JSON.stringify(rows(detailSections.sintactico, 'Errores sintácticos')[0]) === JSON.stringify([error.message, error.token.type, visibleText(error.token.lexeme), String(error.token.position.line), String(error.token.position.column)]), 'Error sintáctico mal mapeado');
    assert(detailSections.sintactico.textContent.indexOf('Estado sintáctico: Con errores') !== -1, 'Estado sintáctico incorrecto');
});
test('Estado sintáctico correcto y sin errores', function() {
    showResult(realResult('puts 1'), 'puts 1');
    assert(detailSections.sintactico.textContent.indexOf('Estado sintáctico: Correcto') !== -1, 'Estado correcto ausente');
    assert(detailSections.sintactico.textContent.indexOf('Sin errores sintácticos.') !== -1, 'Mensaje positivo ausente');
});
test('Símbolos semánticos reales muestran nombre, kind, type, scope y posición', function() {
    var result = realResult('x = 1\nputs x'); showResult(result, 'x = 1\nputs x');
    result.semanticSymbols.forEach(function(symbol, index) {
        assert(JSON.stringify(rows(detailSections.semantico, 'Símbolos semánticos')[index]) === JSON.stringify([symbol.name, symbol.kind, symbol.type, symbol.scope, String(symbol.position.line), String(symbol.position.column), String(symbol.arity)]), 'Símbolo mal mapeado');
    });
});
test('Aridad de métodos y valor -1 de otros símbolos conservados', function() {
    var source = 'def suma(a, b)\nreturn a + b\nend'; var result = realResult(source); showResult(result, source);
    var arities = rows(detailSections.semantico, 'Símbolos semánticos').map(function(row) { return row[6]; });
    assert(arities.indexOf('2') !== -1 && arities.indexOf('-1') !== -1, 'Aridad modificada');
});
test('Error semántico real muestra mensaje y posición', function() {
    var result = realResult('puts nombre'); showResult(result, 'puts nombre'); var error = result.semanticErrors[0];
    assert(JSON.stringify(rows(detailSections.semantico, 'Errores semánticos')[0]) === JSON.stringify([error.message, String(error.position.line), String(error.position.column)]), 'Error semántico mal mapeado');
});
test('Alfabeto representa caracteres normales e invisibles sin alterar datos', function() {
    var result = emptyResult(); result.alphabet = ['a', ' ', '\t', '\n', '\r', '\u0001'];
    var before = JSON.stringify(result); showResult(result);
    assert(JSON.stringify(nodes(detailSections.lexico, 'li').map(function(node) { return node.textContent; })) === JSON.stringify(['a', '[espacio]', '\\t', '\\n', '\\r', '\\u0001']), 'Alfabeto invisible/confuso');
    assert(JSON.stringify(result) === before, 'Alfabeto original alterado');
});
test('Símbolos léxicos muestran valor y posición de cada ocurrencia', function() {
    var result = realResult('x = 1\n'); showResult(result, 'x = 1\n');
    var rendered = rows(detailSections.lexico, 'Símbolos léxicos');
    assert(rendered.length === result.lexicalSymbols.length, 'Símbolos descartados');
    result.lexicalSymbols.forEach(function(symbol, index) {
        assert(JSON.stringify(rendered[index]) === JSON.stringify([visibleText(symbol.value), String(symbol.position.line), String(symbol.position.column)]), 'Símbolo léxico mal mapeado');
    });
});
test('Arrays vacíos muestran explicaciones y no tablas vacías', function() {
    showResult(emptyResult());
    assert(nodes(controls['result-details'], 'table').length === 0, 'Tablas vacías sin explicación');
    var text = controls['result-details'].textContent;
    ['No se generaron tokens.', 'Sin errores léxicos.', 'Sin errores sintácticos.', 'Sin errores semánticos.', 'No se generaron símbolos semánticos.', 'No se generaron símbolos léxicos.', 'No se generaron elementos del alfabeto.'].forEach(function(message) { assert(text.indexOf(message) !== -1, 'Estado vacío ausente: ' + message); });
});
test('Lexemas largos se conservan completos', function() {
    var longText = new Array(12001).join('x'); var result = emptyResult();
    result.tokens = [{ type: 'IDENTIFIER', lexeme: longText, position: { line: 1, column: 1 } }]; showResult(result);
    assert(rows(detailSections.lexico, 'Tokens')[0][0] === longText, 'Lexema largo truncado');
});
test('HTML/script en lexemas, diagnósticos y nombres solo se muestra como texto', function() {
    var payload = '<script>alert("x")</script><img onerror="x">'; var result = emptyResult();
    result.valid = false; result.lexicalErrorCount = 1; result.totalErrorCount = 1;
    result.tokens = [{ type: 'STRING', lexeme: payload, position: { line: 1, column: 1 } }];
    result.lexicalErrors = [{ message: payload, lexeme: payload, position: { line: 1, column: 1 } }];
    result.semanticSymbols = [{ name: payload, kind: 'VARIABLE', type: 'STRING', scope: payload, arity: -1, position: { line: 1, column: 1 } }];
    showResult(result);
    assert(rows(detailSections.lexico, 'Errores léxicos')[0][0] === payload, 'Mensaje interpretado/alterado');
    assert(rows(detailSections.semantico, 'Símbolos semánticos')[0][0] === payload, 'Nombre interpretado/alterado');
    assert(nodes(controls['result-details'], 'script').length === 0 && nodes(controls['result-details'], 'img').length === 0, 'HTML se convirtió en elementos');
});
test('Editar el código invalida y elimina las tres vistas', function() {
    edit('otro'); assert(analysisState.result === null && controls['result-details'].hidden && controls['result-details'].children.length === 0, 'Detalle anterior permanece');
    var count = requests.length; mode('lexico'); assert(requests.length === count && controls['result-details'].hidden, 'Modo sin resultado hace fetch/muestra detalle');
});
test('Código con errores se representa en las tres vistas', function() {
    mode('todo'); var result = realResult('?'); showResult(result, '?');
    assert(!result.valid && !detailSections.lexico.hidden && !detailSections.sintactico.hidden && !detailSections.semantico.hidden, 'Resultado inválido rompe vistas');
});
test('Todo usa el mismo JSON y respeta orden léxico, sintáctico, semántico', function() {
    var result = realResult('x = 1'); var original = JSON.stringify(result); showResult(result, 'x = 1'); mode('todo');
    assert(analysisState.result === result && JSON.stringify(result) === original, 'Resultado modificado o fabricado');
    assert(controls['result-details'].children[0] === detailSections.lexico && controls['result-details'].children[1] === detailSections.sintactico && controls['result-details'].children[2] === detailSections.semantico, 'Orden incorrecto');
});
test('Campos opcionales ausentes o filas incompletas no rompen interfaz', function() {
    var result = emptyResult(); result.tokens = [null, {}]; result.lexicalSymbols = null; delete result.alphabet;
    showResult(result); assert(rows(detailSections.lexico, 'Tokens').length === 2, 'Fila incompleta causa fallo');
    assert(detailSections.lexico.textContent.indexOf('Detalle no disponible') !== -1, 'Datos ausentes simulan ausencia de errores');
});
test('Nueva petición y error HTTP eliminan detalle anterior', function() {
    showResult(realResult('x = 1'), 'x = 1'); analyzeClick();
    assert(controls['result-details'].hidden && controls['result-details'].children.length === 0, 'Petición conserva datos anteriores');
    lastRequest().promise.reject(new Error('Network')); assert(controls['result-details'].hidden, 'Error muestra detalle anterior');
});
test('Contenedores de tablas son enfocables y tienen encabezados accesibles', function() {
    showResult(realResult('x = 1'), 'x = 1');
    nodes(controls['result-details'], 'table').forEach(function(node) {
        assert(node.parentNode.tabIndex === 0 && node.parentNode.attributes.role === 'region', 'Scroll no accesible');
        nodes(node, 'th').forEach(function(cell) { assert(cell.attributes.scope === 'col', 'Cabecera sin scope'); });
    });
});
[['baja.rb', 38, 3], ['media.rb', 73, 7], ['alta.rb', 174, 10]].forEach(function(fixture) {
    test('Respuesta REAL ' + fixture[0] + ' se representa íntegramente', function() {
        var code = readUtf8(root + '/archivos-prueba/' + fixture[0]); var result = realResult(code); showResult(result, code); mode('todo');
        assert(result.valid && result.totalErrorCount === 0, 'Caso oficial no válido');
        assert(rows(detailSections.lexico, 'Tokens').length === fixture[1], 'Cantidad real de tokens incorrecta');
        assert(rows(detailSections.semantico, 'Símbolos semánticos').length === fixture[2], 'Cantidad real de símbolos incorrecta');
        assert(rows(detailSections.lexico, 'Símbolos léxicos').length === result.lexicalSymbols.length, 'Símbolos léxicos perdidos');
    });
});
