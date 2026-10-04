package com.umg.analizador.ast;

import com.umg.analizador.lexer.Token;
import java.util.Objects;

/** Literal que conserva su token y lexema original. */
public final class LiteralNode extends ExpressionNode {
    private final Token token;

    public LiteralNode(Token token) {
        super(token.getPosition());
        this.token = Objects.requireNonNull(token, "token no debe ser null");
    }

    public Token getToken() {
        return token;
    }
}

