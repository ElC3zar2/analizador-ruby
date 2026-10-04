package com.umg.analizador.ast;

import com.umg.analizador.model.SourcePosition;
import java.util.Objects;

/** Salida puts de una unica expresion. */
public final class PutsNode extends StatementNode {
    private final ExpressionNode expression;

    public PutsNode(SourcePosition position, ExpressionNode expression) {
        super(position);
        this.expression = Objects.requireNonNull(expression, "expression no debe ser null");
    }

    public ExpressionNode getExpression() {
        return expression;
    }
}

