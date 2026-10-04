package com.umg.analizador.ast;

import com.umg.analizador.model.SourcePosition;

/** Base de las expresiones del subconjunto Ruby. */
public abstract class ExpressionNode extends Node {
    protected ExpressionNode(SourcePosition position) {
        super(position);
    }
}

