package com.umg.analizador.semantic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Tabla ordenada por registro; metodos y variables tienen espacios de nombres separados. */
public final class SymbolTable {
    public static final String GLOBAL = "GLOBAL";
    private final Map<String, SemanticSymbol> symbols = new LinkedHashMap<>();

    public static String methodScope(String name) {
        return "METHOD:" + Objects.requireNonNull(name, "name no debe ser null");
    }

    public void put(SemanticSymbol symbol) {
        Objects.requireNonNull(symbol, "symbol no debe ser null");
        symbols.put(key(symbol.getName(), symbol.getScope(), symbol.getKind() == SymbolKind.METHOD), symbol);
    }

    public SemanticSymbol findVariable(String name, String scope) {
        return symbols.get(key(name, scope, false));
    }

    public SemanticSymbol findMethod(String name) {
        return symbols.get(key(name, GLOBAL, true));
    }

    public List<SemanticSymbol> getSymbols() {
        return List.copyOf(symbols.values());
    }

    private static String key(String name, String scope, boolean method) {
        Objects.requireNonNull(name, "name no debe ser null");
        Objects.requireNonNull(scope, "scope no debe ser null");
        return scope + '\0' + (method ? "METHOD" : "VARIABLE") + '\0' + name;
    }
}
