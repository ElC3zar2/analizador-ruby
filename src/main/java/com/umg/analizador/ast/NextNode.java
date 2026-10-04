package com.umg.analizador.ast;

import com.umg.analizador.model.SourcePosition;

/** Sentencia next; su contexto se comprobara en la fase semantica. */
public final class NextNode extends StatementNode {

    public NextNode(SourcePosition position) {
        super(position);
    }
}

