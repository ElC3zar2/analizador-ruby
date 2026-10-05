package com.umg.analizador.semantic;

import com.umg.analizador.model.SourcePosition;
import java.util.Objects;

/** Entrada inmutable; la reasignacion reemplaza el tipo y conserva la posicion inicial. */
public final class SemanticSymbol {
    private final String name;
    private final SymbolKind kind;
    private final SemanticType type;
    private final SourcePosition position;
    private final String scope;
    private final int arity;

    public SemanticSymbol(String name, SymbolKind kind, SemanticType type,
            SourcePosition position, String scope, int arity) {
        this.name = Objects.requireNonNull(name, "name no debe ser null");
        this.kind = Objects.requireNonNull(kind, "kind no debe ser null");
        this.type = Objects.requireNonNull(type, "type no debe ser null");
        this.position = Objects.requireNonNull(position, "position no debe ser null");
        this.scope = Objects.requireNonNull(scope, "scope no debe ser null");
        if ((kind == SymbolKind.METHOD && arity < 0) || (kind != SymbolKind.METHOD && arity != -1)) {
            throw new IllegalArgumentException("arity debe ser >= 0 para metodos y -1 para otros simbolos");
        }
        this.arity = arity;
    }

    public String getName() { return name; }
    public SymbolKind getKind() { return kind; }
    public SemanticType getType() { return type; }
    public SourcePosition getPosition() { return position; }
    public String getScope() { return scope; }
    public int getArity() { return arity; }

    public SemanticSymbol withType(SemanticType newType) {
        return new SemanticSymbol(name, kind, newType, position, scope, arity);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) { return true; }
        if (!(object instanceof SemanticSymbol)) { return false; }
        SemanticSymbol other = (SemanticSymbol) object;
        return name.equals(other.name) && kind == other.kind && type == other.type
                && position.equals(other.position) && scope.equals(other.scope) && arity == other.arity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, kind, type, position, scope, arity);
    }

    @Override
    public String toString() {
        return "SemanticSymbol{name='" + name + "', kind=" + kind + ", type=" + type
                + ", position=" + position + ", scope='" + scope + "', arity=" + arity + '}';
    }
}
