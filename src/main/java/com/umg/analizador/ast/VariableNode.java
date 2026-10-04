package com.umg.analizador.ast;

import com.umg.analizador.lexer.Token;
import java.util.Objects;

/** Referencia a identificador o variable Ruby. */
public final class VariableNode extends ExpressionNode {
    private final Token token;

    public VariableNode(Token token) {
        super(token.getPosition());
        this.token = Objects.requireNonNull(token, "token no debe ser null");
    }

    public Token getToken() {
        return token;
    }
}

