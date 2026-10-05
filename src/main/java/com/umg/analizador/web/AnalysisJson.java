package com.umg.analizador.web;

import com.umg.analizador.lexer.Token;
import com.umg.analizador.model.SourcePosition;
import com.umg.analizador.service.AnalysisResult;
import java.util.StringJoiner;
import java.util.function.Function;

/** Serializa DTOs reales; no invoca analizadores ni usa toString como JSON. */
public final class AnalysisJson {
    private AnalysisJson() { }

    public static String serialize(AnalysisResult result) {
        return "{\"valid\":" + result.isValid()
                + ",\"lexicalErrorCount\":" + result.getLexicalErrorCount()
                + ",\"syntaxErrorCount\":" + result.getSyntaxErrorCount()
                + ",\"semanticErrorCount\":" + result.getSemanticErrorCount()
                + ",\"totalErrorCount\":" + result.getTotalErrorCount()
                + ",\"tokens\":" + array(result.getLexicalResult().getTokens(), AnalysisJson::token)
                + ",\"lexicalErrors\":" + array(result.getLexicalResult().getErrors(), error ->
                        "{\"message\":" + quote(error.getMessage()) + ",\"lexeme\":" + quote(error.getLexeme())
                        + ",\"position\":" + position(error.getPosition()) + "}")
                + ",\"syntaxErrors\":" + array(result.getSyntaxResult().getErrors(), error ->
                        "{\"message\":" + quote(error.getMessage()) + ",\"token\":" + token(error.getToken()) + "}")
                + ",\"semanticErrors\":" + array(result.getSemanticResult().getErrors(), error ->
                        "{\"message\":" + quote(error.getMessage())
                        + ",\"position\":" + position(error.getPosition()) + "}")
                + ",\"semanticSymbols\":" + array(result.getSemanticResult().getSymbols(), symbol ->
                        "{\"name\":" + quote(symbol.getName()) + ",\"kind\":" + quote(symbol.getKind().name())
                        + ",\"type\":" + quote(symbol.getType().name()) + ",\"scope\":" + quote(symbol.getScope())
                        + ",\"arity\":" + symbol.getArity() + ",\"position\":" + position(symbol.getPosition()) + "}")
                + ",\"alphabet\":" + array(result.getLexicalResult().getAlphabet(), AnalysisJson::quote)
                + ",\"lexicalSymbols\":" + array(result.getLexicalResult().getSymbols(), symbol ->
                        "{\"value\":" + quote(symbol.getValue()) + ",\"position\":" + position(symbol.getPosition()) + "}")
                + "}";
    }

    public static String error(String message) {
        return "{\"error\":" + quote(message) + "}";
    }

    public static String quote(String value) {
        StringBuilder json = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"': json.append("\\\""); break;
                case '\\': json.append("\\\\"); break;
                case '\n': json.append("\\n"); break;
                case '\r': json.append("\\r"); break;
                case '\t': json.append("\\t"); break;
                case '\b': json.append("\\b"); break;
                case '\f': json.append("\\f"); break;
                default:
                    if (character < 0x20 || Character.isSurrogate(character)) {
                        json.append("\\u");
                        String hex = Integer.toHexString(character);
                        for (int padding = hex.length(); padding < 4; padding++) { json.append('0'); }
                        json.append(hex);
                    } else {
                        json.append(character);
                    }
            }
        }
        return json.append('"').toString();
    }

    private static String position(SourcePosition position) {
        return "{\"line\":" + position.getLine() + ",\"column\":" + position.getColumn() + "}";
    }

    private static String token(Token token) {
        return "{\"type\":" + quote(token.getType().name()) + ",\"lexeme\":" + quote(token.getLexeme())
                + ",\"position\":" + position(token.getPosition()) + "}";
    }

    private static <T> String array(Iterable<T> items, Function<T, String> serialize) {
        StringJoiner json = new StringJoiner(",", "[", "]");
        for (T item : items) { json.add(serialize.apply(item)); }
        return json.toString();
    }
}
