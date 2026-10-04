package com.umg.analizador.ast;

import com.umg.analizador.lexer.Token;
import java.util.List;
import java.util.Objects;

/** Llamada simple con parentesis y argumentos ordenados. */
public final class CallExpressionNode extends ExpressionNode {
    private final Token callee;
    private final List<ExpressionNode> arguments;

    public CallExpressionNode(Token callee, List<ExpressionNode> arguments) {
        super(callee.getPosition());
        this.callee = Objects.requireNonNull(callee, "callee no debe ser null");
        this.arguments = List.copyOf(arguments);
    }

    public Token getCallee() {
        return callee;
    }

    public List<ExpressionNode> getArguments() {
        return arguments;
    }
}

