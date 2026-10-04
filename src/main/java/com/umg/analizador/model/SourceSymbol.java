package com.umg.analizador.model;

import java.util.Objects;

/**
 * Un simbolo es una unidad elemental perteneciente al alfabeto observado en el
 * codigo fuente. Cada ocurrencia conserva su posicion; normalmente es un caracter.
 */
public final class SourceSymbol {
    private final String value;
    private final SourcePosition position;

    public SourceSymbol(String value, SourcePosition position) {
        this.value = Objects.requireNonNull(value, "value no debe ser null");
        this.position = Objects.requireNonNull(position, "position no debe ser null");
    }

    public String getValue() {
        return value;
    }

    public SourcePosition getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SourceSymbol)) {
            return false;
        }
        SourceSymbol other = (SourceSymbol) object;
        return value.equals(other.value) && position.equals(other.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, position);
    }

    @Override
    public String toString() {
        return "SourceSymbol{value='" + value + "', position=" + position + '}';
    }
}
