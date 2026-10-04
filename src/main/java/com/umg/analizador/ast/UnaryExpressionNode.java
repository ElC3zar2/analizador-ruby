package com.umg.analizador.ast;

import com.umg.analizador.lexer.Token;
import java.util.Objects;

/** Operacion unaria con su operando. */
public final class UnaryExpressionNode extends ExpressionNode {
    private final Token operator;
    private final ExpressionNode expression;

    public UnaryExpressionNode(Token operator, ExpressionNode expression) {
        super(operator.getPosition());
        this.operator = Objects.requireNonNull(operator, "operator no debe ser null");
        this.expression = Objects.requireNonNull(expression, "expression no debe ser null");
    }

    public Token getOperator() {
        return operator;
    }

    public ExpressionNode getExpression() {
        return expression;
    }
}

