package com.umg.analizador.ast;

import com.umg.analizador.model.SourcePosition;

/** Sentencia break; su contexto se comprobara en la fase semantica. */
public final class BreakNode extends StatementNode {

    public BreakNode(SourcePosition position) {
        super(position);
    }
}

