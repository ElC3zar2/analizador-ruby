package com.umg.analizador.ast;

import com.umg.analizador.lexer.Token;
import com.umg.analizador.model.SourcePosition;
import java.util.List;
import java.util.Objects;

/** Ciclo for sobre una expresion iterable, sin comprobacion semantica. */
public final class ForNode extends StatementNode {
    private final Token variable;
    private final ExpressionNode iterable;
    private final List<StatementNode> body;

    public ForNode(SourcePosition position, Token variable, ExpressionNode iterable, List<StatementNode> body) {
        super(position);
        this.variable = Objects.requireNonNull(variable, "variable no debe ser null");
        this.iterable = Objects.requireNonNull(iterable, "iterable no debe ser null");
        this.body = List.copyOf(body);
    }

    public Token getVariable() {
        return variable;
    }

    public ExpressionNode getIterable() {
        return iterable;
    }

    public List<StatementNode> getBody() {
        return body;
    }
}

