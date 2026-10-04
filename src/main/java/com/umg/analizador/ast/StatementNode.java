package com.umg.analizador.ast;

import com.umg.analizador.model.SourcePosition;

/** Base de las sentencias del subconjunto Ruby. */
public abstract class StatementNode extends Node {
    protected StatementNode(SourcePosition position) {
        super(position);
    }
}

