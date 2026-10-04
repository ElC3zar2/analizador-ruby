package com.umg.analizador.ast;

import com.umg.analizador.lexer.Token;
import com.umg.analizador.model.SourcePosition;
import java.util.List;
import java.util.Objects;

/** Definicion de metodo con parametros simples. */
public final class MethodDefinitionNode extends StatementNode {
    private final Token name;
    private final List<Token> parameters;
    private final List<StatementNode> body;

    public MethodDefinitionNode(SourcePosition position, Token name, List<Token> parameters, List<StatementNode> body) {
        super(position);
        this.name = Objects.requireNonNull(name, "name no debe ser null");
        this.parameters = List.copyOf(parameters);
        this.body = List.copyOf(body);
    }

    public Token getName() {
        return name;
    }

    public List<Token> getParameters() {
        return parameters;
    }

    public List<StatementNode> getBody() {
        return body;
    }
}

