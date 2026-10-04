package com.umg.analizador.ast;

import com.umg.analizador.model.SourcePosition;
import java.util.List;
import java.util.Objects;

/** Condicional con ramas elsif ordenadas y cuerpo else opcional. */
public final class IfNode extends StatementNode {
    private final ExpressionNode condition;
    private final List<StatementNode> thenStatements;
    private final List<Branch> elsifBranches;
    private final List<StatementNode> elseStatements;

    public IfNode(SourcePosition position, ExpressionNode condition,
            List<StatementNode> thenStatements, List<Branch> elsifBranches,
            List<StatementNode> elseStatements) {
        super(position);
        this.condition = Objects.requireNonNull(condition, "condition no debe ser null");
        this.thenStatements = List.copyOf(thenStatements);
        this.elsifBranches = List.copyOf(elsifBranches);
        this.elseStatements = List.copyOf(elseStatements);
    }

    public ExpressionNode getCondition() {
        return condition;
    }

    public List<StatementNode> getThenStatements() {
        return thenStatements;
    }

    public List<Branch> getElsifBranches() {
        return elsifBranches;
    }

    public List<StatementNode> getElseStatements() {
        return elseStatements;
    }

    /** Una rama elsif con condicion y cuerpo inmutables. */
    public static final class Branch extends Node {
        private final ExpressionNode condition;
        private final List<StatementNode> statements;

        public Branch(SourcePosition position, ExpressionNode condition, List<StatementNode> statements) {
            super(position);
            this.condition = Objects.requireNonNull(condition, "condition no debe ser null");
            this.statements = List.copyOf(statements);
        }

        public ExpressionNode getCondition() {
            return condition;
        }

        public List<StatementNode> getStatements() {
            return statements;
        }
    }
}

