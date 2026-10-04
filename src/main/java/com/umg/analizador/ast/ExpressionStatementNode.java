package com.umg.analizador.ast;

import java.util.Objects;

/** Expresion utilizada como sentencia. */
public final class ExpressionStatementNode extends StatementNode {
    private final ExpressionNode expression;

    public ExpressionStatementNode(ExpressionNode expression) {
        super(expression.getPosition());
        this.expression = Objects.requireNonNull(expression, "expression no debe ser null");
    }

    public ExpressionNode getExpression() {
        return expression;
    }
}

