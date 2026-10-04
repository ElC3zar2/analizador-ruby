package com.umg.analizador.ast;

import com.umg.analizador.model.SourcePosition;
import java.util.Objects;

/** Agrupacion explicita entre parentesis. */
public final class GroupingNode extends ExpressionNode {
    private final ExpressionNode expression;

    public GroupingNode(SourcePosition position, ExpressionNode expression) {
        super(position);
        this.expression = Objects.requireNonNull(expression, "expression no debe ser null");
    }

    public ExpressionNode getExpression() {
        return expression;
    }
}

