package com.umg.analizador.ast;

import com.umg.analizador.lexer.Token;
import java.util.Objects;

/** Operacion binaria con ambos operandos. */
public final class BinaryExpressionNode extends ExpressionNode {
    private final ExpressionNode left;
    private final Token operator;
    private final ExpressionNode right;

    public BinaryExpressionNode(ExpressionNode left, Token operator, ExpressionNode right) {
        super(left.getPosition());
        this.left = Objects.requireNonNull(left, "left no debe ser null");
        this.operator = Objects.requireNonNull(operator, "operator no debe ser null");
        this.right = Objects.requireNonNull(right, "right no debe ser null");
    }

    public ExpressionNode getLeft() {
        return left;
    }

    public Token getOperator() {
        return operator;
    }

    public ExpressionNode getRight() {
        return right;
    }
}

