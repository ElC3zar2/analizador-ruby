package com.umg.analizador.ast;

import com.umg.analizador.model.SourcePosition;
import java.util.Objects;

/** Base del AST: conserva la posicion inicial de cada construccion. */
public abstract class Node {
    private final SourcePosition position;

    protected Node(SourcePosition position) {
        this.position = Objects.requireNonNull(position, "position no debe ser null");
    }

    public final SourcePosition getPosition() {
        return position;
    }
}

