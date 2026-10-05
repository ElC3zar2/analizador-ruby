package com.umg.analizador.web;

import com.umg.analizador.service.AnalysisResult;
import com.umg.analizador.service.AnalysisService;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import static org.junit.Assert.*;

/** Servicio real, JSON.parse de Java 11 y proxies Servlet sin librerías de mocks. */
public class AnalysisServletTest {
    private final AnalysisService service = new AnalysisService();

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String json) throws Exception {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        assertNotNull("Se requiere el JDK 11 del proyecto", engine);
        engine.put("jsonInput", json);
        return (Map<String, Object>) engine.eval("Java.asJSONCompatible(JSON.parse(jsonInput))");
    }

    private Map<String, Object> analyze(String source) throws Exception {
        AnalysisResult result = service.analyze(source);
        Map<String, Object> json = parse(AnalysisJson.serialize(result));
        assertEquals(result.isValid(), json.get("valid"));
        assertEquals(result.getLexicalErrorCount(), number(json, "lexicalErrorCount"));
        assertEquals(result.getSyntaxErrorCount(), number(json, "syntaxErrorCount"));
        assertEquals(result.getSemanticErrorCount(), number(json, "semanticErrorCount"));
        assertEquals(result.getTotalErrorCount(), number(json, "totalErrorCount"));
        assertEquals(number(json, "totalErrorCount"), number(json, "lexicalErrorCount")
                + number(json, "syntaxErrorCount") + number(json, "semanticErrorCount"));
        assertEquals(number(json, "lexicalErrorCount"), ((List<?>) json.get("lexicalErrors")).size());
        assertEquals(number(json, "syntaxErrorCount"), ((List<?>) json.get("syntaxErrors")).size());
        assertEquals(number(json, "semanticErrorCount"), ((List<?>) json.get("semanticErrors")).size());
        return json;
    }

    private int number(Map<String, Object> json, String key) { return ((Number) json.get(key)).intValue(); }

    @Test public void validRuby() throws Exception { assertEquals(true, analyze("x = 1\nputs x").get("valid")); }
    @Test public void lexicalError() throws Exception {
        Map<String, Object> json = analyze("x = 10 ?");
        assertEquals(false, json.get("valid")); assertEquals(1, number(json, "lexicalErrorCount"));
    }
    @Test public void syntaxError() throws Exception {
        Map<String, Object> json = analyze("x =");
        assertEquals(false, json.get("valid")); assertEquals(1, number(json, "syntaxErrorCount"));
    }
    @Test public void semanticError() throws Exception {
        Map<String, Object> json = analyze("puts variable_no_definida");
        assertEquals(false, json.get("valid")); assertEquals(1, number(json, "semanticErrorCount"));
    }
    @Test public void combinedErrorCountsAreNotDuplicated() throws Exception {
        Map<String, Object> json = analyze("?"); assertEquals(2, number(json, "totalErrorCount"));
    }
    @Test public void emptyCodeUsesServiceBehavior() throws Exception {
        Map<String, Object> json = analyze(""); assertEquals(true, json.get("valid")); assertEquals(0, number(json, "totalErrorCount"));
    }
    @Test public void multipleSemanticErrors() throws Exception {
        assertEquals(3, number(analyze("puts falta\nbreak\nreturn 10"), "semanticErrorCount"));
    }
    private void roundTrip(String source) throws Exception { assertEquals(source, parse("{\"value\":" + AnalysisJson.quote(source) + "}").get("value")); }
    @Test public void escapesQuotes() throws Exception { roundTrip("\"Ruby\""); assertEquals("\"\\\"Ruby\\\"\"", AnalysisJson.quote("\"Ruby\"")); }
    @Test public void escapesBackslash() throws Exception { roundTrip("C:\\Ruby\\archivo.rb"); }
    @Test public void escapesNewline() throws Exception { roundTrip("uno\ndos"); assertFalse(AnalysisJson.quote("\n").contains("\n")); }
    @Test public void escapesCarriageReturnAndTab() throws Exception { roundTrip("uno\r\n\tdos"); }
    @Test public void escapesAllControlCharacters() throws Exception {
        StringBuilder value = new StringBuilder(); for (char c = 0; c < 32; c++) { value.append(c); }
        roundTrip(value.toString());
    }
    @Test public void preservesUnicodeAndSurrogates() throws Exception { roundTrip("Español áéíóú ñ \uD83D\uDC8E \uD800"); }
    @Test public void serializesActualTokenLexemes() throws Exception {
        String code = "puts \"Ruby\"\n";
        Map<String, Object> json = analyze(code);
        List<?> tokens = (List<?>) json.get("tokens");
        assertEquals(service.analyze(code).getTokenCount(), tokens.size());
        assertEquals("puts", ((Map<?, ?>) tokens.get(0)).get("lexeme"));
        assertTrue(((List<?>) json.get("alphabet")).contains("\n"));
        assertEquals(code.length(), ((List<?>) json.get("lexicalSymbols")).size());
    }
    private void official(String name, int tokens, int symbols) throws Exception {
        Map<String, Object> json = analyze(Files.readString(Paths.get("archivos-prueba", name), StandardCharsets.UTF_8));
        assertEquals(true, json.get("valid")); assertEquals(0, number(json, "totalErrorCount"));
        assertEquals(tokens, ((List<?>) json.get("tokens")).size());
        assertEquals(symbols, ((List<?>) json.get("semanticSymbols")).size());
    }
    @Test public void officialBaja() throws Exception { official("baja.rb", 38, 3); }
    @Test public void officialMedia() throws Exception { official("media.rb", 73, 7); }
    @Test public void officialAlta() throws Exception { official("alta.rb", 174, 10); }

    private static final class Exchange {
        String method = "POST", contentType = "application/x-www-form-urlencoded;charset=UTF-8", code = "x = 1";
        String requestEncoding, responseEncoding, responseContentType;
        int status;
        boolean parameterRead;
        Map<String, String> headers = new HashMap<>();
        StringWriter body = new StringWriter();
        HttpServletRequest request() {
            return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getMethod": return this.method;
                    case "setCharacterEncoding": assertFalse(parameterRead); requestEncoding = (String) args[0]; return null;
                    case "getContentType": return contentType;
                    case "getParameter": assertEquals("UTF-8", requestEncoding); assertEquals("codigoRuby", args[0]); parameterRead = true; return code;
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });
        }
        HttpServletResponse response() {
            return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpServletResponse.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "setCharacterEncoding": responseEncoding = (String) args[0]; return null;
                    case "setContentType": responseContentType = (String) args[0]; return null;
                    case "setHeader": headers.put((String) args[0], (String) args[1]); return null;
                    case "setStatus": status = (Integer) args[0]; return null;
                    case "getWriter": return new PrintWriter(body);
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });
        }
    }
    private Map<String, Object> call(AnalysisServlet servlet, Exchange exchange) throws Exception {
        servlet.service(exchange.request(), exchange.response());
        assertEquals("UTF-8", exchange.responseEncoding);
        assertEquals("application/json", exchange.responseContentType);
        assertEquals("no-store", exchange.headers.get("Cache-Control"));
        return parse(exchange.body.toString());
    }
    @Test public void servletMapping() {
        WebServlet mapping = AnalysisServlet.class.getAnnotation(WebServlet.class);
        assertArrayEquals(new String[]{"/api/analyze"}, mapping.urlPatterns());
    }
    @Test public void postUsesRealService() throws Exception {
        Exchange exchange = new Exchange(); assertEquals(true, call(new AnalysisServlet(), exchange).get("valid")); assertEquals(200, exchange.status);
    }
    @Test public void invalidRubyIsSuccessfulHttpWithDiagnostics() throws Exception {
        Exchange exchange = new Exchange(); exchange.code = "puts falta";
        assertEquals(false, call(new AnalysisServlet(), exchange).get("valid")); assertEquals(200, exchange.status);
    }
    @Test public void missingParameterIs400() throws Exception {
        Exchange exchange = new Exchange(); exchange.code = null;
        assertNotNull(call(new AnalysisServlet(), exchange).get("error")); assertEquals(400, exchange.status);
    }
    @Test public void emptyParameterIsAnalyzed() throws Exception {
        Exchange exchange = new Exchange(); exchange.code = "";
        assertEquals(true, call(new AnalysisServlet(), exchange).get("valid")); assertEquals(200, exchange.status);
    }
    @Test public void unsupportedContentTypeIs415() throws Exception {
        for (String type : new String[]{null, "application/json", "multipart/form-data", "text/plain"}) {
            Exchange exchange = new Exchange(); exchange.contentType = type;
            assertNotNull(call(new AnalysisServlet(), exchange).get("error")); assertEquals(415, exchange.status); assertFalse(exchange.parameterRead);
        }
    }
    @Test public void unsupportedMethodsAre405() throws Exception {
        for (String method : new String[]{"GET", "HEAD", "PUT", "DELETE", "OPTIONS", "PATCH"}) {
            Exchange exchange = new Exchange(); exchange.method = method;
            assertNotNull(call(new AnalysisServlet(), exchange).get("error")); assertEquals(405, exchange.status);
            assertEquals("POST", exchange.headers.get("Allow")); assertFalse(exchange.parameterRead);
        }
    }
    @Test public void utf8AndExactCodeReachService() throws Exception {
        Exchange exchange = new Exchange(); exchange.code = "# á ñ\r\n  puts \"Ruby + & =\"\n";
        AnalysisServlet capture = new AnalysisServlet() {
            @Override protected AnalysisResult analyze(String code) { assertEquals(exchange.code, code); return service.analyze(code); }
        };
        assertEquals(true, call(capture, exchange).get("valid")); assertEquals(200, exchange.status);
    }
    @Test public void internalFailureIsControlledAndDoesNotLeak() throws Exception {
        Exchange exchange = new Exchange();
        AnalysisServlet broken = new AnalysisServlet() {
            @Override protected AnalysisResult analyze(String code) { throw new IllegalStateException("SECRET_INTERNAL_PATH"); }
        };
        Map<String, Object> json = call(broken, exchange);
        assertEquals(500, exchange.status); assertEquals("No fue posible completar el análisis.", json.get("error"));
        assertFalse(exchange.body.toString().contains("SECRET_INTERNAL_PATH")); assertFalse(exchange.body.toString().contains("Exception"));
    }
}
