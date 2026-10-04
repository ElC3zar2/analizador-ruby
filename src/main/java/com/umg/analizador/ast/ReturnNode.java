package com.umg.analizador.ast;

import com.umg.analizador.model.SourcePosition;
import java.util.Objects;

/** Retorno con expresion opcional; null representa return sin valor. */
public final class ReturnNode extends StatementNode {
    private final ExpressionNode expression;

    public ReturnNode(SourcePosition position, ExpressionNode expression) {
        super(position);
        this.expression = expression;
    }

    public ExpressionNode getExpression() {
        return expression;
    }
}

