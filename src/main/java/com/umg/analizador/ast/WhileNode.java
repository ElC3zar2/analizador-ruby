package com.umg.analizador.ast;

import com.umg.analizador.model.SourcePosition;
import java.util.List;
import java.util.Objects;

/** Ciclo while y su cuerpo. */
public final class WhileNode extends StatementNode {
    private final ExpressionNode condition;
    private final List<StatementNode> body;

    public WhileNode(SourcePosition position, ExpressionNode condition, List<StatementNode> body) {
        super(position);
        this.condition = Objects.requireNonNull(condition, "condition no debe ser null");
        this.body = List.copyOf(body);
    }

    public ExpressionNode getCondition() {
        return condition;
    }

    public List<StatementNode> getBody() {
        return body;
    }
}

