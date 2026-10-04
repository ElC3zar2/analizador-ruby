package com.umg.analizador.ast;

import com.umg.analizador.model.SourcePosition;
import java.util.List;

/** Programa y sus sentencias en orden de aparicion. */
public final class ProgramNode extends Node {
    private final List<StatementNode> statements;

    public ProgramNode(SourcePosition position, List<StatementNode> statements) {
        super(position);
        this.statements = List.copyOf(statements);
    }

    public List<StatementNode> getStatements() {
        return statements;
    }
}

