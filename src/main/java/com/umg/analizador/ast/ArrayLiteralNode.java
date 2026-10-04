package com.umg.analizador.ast;

import com.umg.analizador.model.SourcePosition;
import java.util.List;

/** Array literal, incluido el array vacio. */
public final class ArrayLiteralNode extends ExpressionNode {
    private final List<ExpressionNode> elements;

    public ArrayLiteralNode(SourcePosition position, List<ExpressionNode> elements) {
        super(position);
        this.elements = List.copyOf(elements);
    }

    public List<ExpressionNode> getElements() {
        return elements;
    }
}

