package com.umg.analizador.ast;

import com.umg.analizador.lexer.Token;
import java.util.Objects;

/** Asignacion simple o compuesta a una variable. */
public final class AssignmentNode extends StatementNode {
    private final Token variable;
    private final Token operator;
    private final ExpressionNode expression;

    public AssignmentNode(Token variable, Token operator, ExpressionNode expression) {
        super(variable.getPosition());
        this.variable = Objects.requireNonNull(variable, "variable no debe ser null");
        this.operator = Objects.requireNonNull(operator, "operator no debe ser null");
        this.expression = Objects.requireNonNull(expression, "expression no debe ser null");
    }

    public Token getVariable() {
        return variable;
    }

    public Token getOperator() {
        return operator;
    }

    public ExpressionNode getExpression() {
        return expression;
    }
}

