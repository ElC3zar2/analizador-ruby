package com.umg.analizador.semantic;

import com.umg.analizador.ast.*;
import com.umg.analizador.lexer.Token;
import com.umg.analizador.lexer.TokenType;
import com.umg.analizador.model.SourcePosition;
import com.umg.analizador.parser.SyntaxAnalysisResult;
import com.umg.analizador.parser.SyntaxAnalyzer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import static com.umg.analizador.semantic.SemanticType.*;

/** Analisis academico de nombres, contextos y tipos evidentes, sin ejecutar Ruby. */
public final class SemanticAnalyzer {
    public SemanticAnalysisResult analyze(String source) {
        Objects.requireNonNull(source, "source no debe ser null");
        SyntaxAnalysisResult syntax = new SyntaxAnalyzer().analyze(source);
        if (syntax.hasErrors()) {
            return new SemanticAnalysisResult(syntax, List.of(), List.of());
        }
        Analysis analysis = new Analysis();
        analysis.registerMethods(syntax.getProgram().getStatements());
        analysis.statements(syntax.getProgram().getStatements());
        return new SemanticAnalysisResult(syntax, analysis.errors, analysis.table.getSymbols());
    }

    /** Estado independiente de una sola entrada: no se comparte entre llamadas. */
    private static final class Analysis {
        private final SymbolTable table = new SymbolTable();
        private final List<SemanticError> errors = new ArrayList<>();
        private final Set<MethodDefinitionNode> registeredMethods = new HashSet<>();
        private String scope = SymbolTable.GLOBAL;
        private int loopDepth;

        private void registerMethods(List<StatementNode> statements) {
            for (StatementNode node : statements) {
                if (node instanceof MethodDefinitionNode) {
                    MethodDefinitionNode method = (MethodDefinitionNode) node;
                    Token name = method.getName();
                    if (table.findMethod(name.getLexeme()) != null) {
                        report("Metodo duplicado: " + name.getLexeme(), name.getPosition());
                        // Conservar la primera firma y evitar mezclar cuerpos en el mismo scope.
                        continue;
                    }
                    table.put(new SemanticSymbol(name.getLexeme(), SymbolKind.METHOD, UNKNOWN,
                            name.getPosition(), SymbolTable.GLOBAL, method.getParameters().size()));
                    registeredMethods.add(method);
                    registerMethods(method.getBody());
                } else if (node instanceof IfNode) {
                    IfNode conditional = (IfNode) node;
                    registerMethods(conditional.getThenStatements());
                    for (IfNode.Branch branch : conditional.getElsifBranches()) {
                        registerMethods(branch.getStatements());
                    }
                    registerMethods(conditional.getElseStatements());
                } else if (node instanceof WhileNode) {
                    registerMethods(((WhileNode) node).getBody());
                } else if (node instanceof ForNode) {
                    registerMethods(((ForNode) node).getBody());
                }
            }
        }

        private void statements(List<StatementNode> statements) {
            for (StatementNode node : statements) {
                statement(node);
            }
        }

        private void statement(StatementNode node) {
            if (node instanceof AssignmentNode) {
                assignment((AssignmentNode) node);
            } else if (node instanceof ExpressionStatementNode) {
                expression(((ExpressionStatementNode) node).getExpression());
            } else if (node instanceof PutsNode) {
                expression(((PutsNode) node).getExpression());
            } else if (node instanceof IfNode) {
                conditional((IfNode) node);
            } else if (node instanceof WhileNode) {
                WhileNode loop = (WhileNode) node;
                expression(loop.getCondition());
                loopBody(loop.getBody());
            } else if (node instanceof ForNode) {
                forLoop((ForNode) node);
            } else if (node instanceof MethodDefinitionNode) {
                method((MethodDefinitionNode) node);
            } else if (node instanceof ReturnNode) {
                ReturnNode returned = (ReturnNode) node;
                if (scope.equals(SymbolTable.GLOBAL)) {
                    report("return fuera de un metodo", node.getPosition());
                }
                if (returned.getExpression() != null) {
                    expression(returned.getExpression());
                }
            } else if (node instanceof BreakNode || node instanceof NextNode) {
                if (loopDepth == 0) {
                    report((node instanceof BreakNode ? "break" : "next") + " fuera de un ciclo", node.getPosition());
                }
            } else {
                throw new IllegalArgumentException("Sentencia AST no soportada: " + node.getClass().getName());
            }
        }

        private void assignment(AssignmentNode node) {
            Token variable = node.getVariable();
            String variableScope = variableScope(variable);
            SemanticSymbol existing = table.findVariable(variable.getLexeme(), variableScope);
            SemanticType right = expression(node.getExpression());
            if (node.getOperator().getType() != TokenType.ASSIGN) {
                if (existing == null) {
                    report("Asignacion compuesta sobre variable no definida: " + variable.getLexeme(), variable.getPosition());
                    return;
                }
                right = binaryType(compoundOperator(node.getOperator().getType()), existing.getType(),
                        right, node.getOperator());
            }
            if (existing == null) {
                table.put(new SemanticSymbol(variable.getLexeme(), SymbolKind.VARIABLE, right,
                        variable.getPosition(), variableScope, -1));
            } else {
                table.put(existing.withType(right));
            }
        }

        private void conditional(IfNode node) {
            expression(node.getCondition());
            statements(node.getThenStatements());
            for (IfNode.Branch branch : node.getElsifBranches()) {
                expression(branch.getCondition());
                statements(branch.getStatements());
            }
            statements(node.getElseStatements());
        }

        private void forLoop(ForNode node) {
            SemanticType iterable = expression(node.getIterable());
            if (iterable != ARRAY && iterable != RANGE && iterable != UNKNOWN) {
                report("for requiere ARRAY o RANGE; se encontro " + iterable, node.getIterable().getPosition());
            }
            Token variable = node.getVariable();
            SemanticSymbol existing = table.findVariable(variable.getLexeme(), scope);
            table.put(new SemanticSymbol(variable.getLexeme(), SymbolKind.LOOP_VARIABLE, UNKNOWN,
                    existing == null ? variable.getPosition() : existing.getPosition(), scope, -1));
            loopBody(node.getBody());
        }

        private void loopBody(List<StatementNode> body) {
            loopDepth++;
            try {
                statements(body);
            } finally {
                loopDepth--;
            }
        }

        private void method(MethodDefinitionNode node) {
            if (!registeredMethods.contains(node)) {
                return;
            }
            String previousScope = scope;
            int previousLoopDepth = loopDepth;
            scope = SymbolTable.methodScope(node.getName().getLexeme());
            loopDepth = 0;
            try {
                for (Token parameter : node.getParameters()) {
                    if (table.findVariable(parameter.getLexeme(), scope) != null) {
                        report("Parametro duplicado: " + parameter.getLexeme(), parameter.getPosition());
                    } else {
                        table.put(new SemanticSymbol(parameter.getLexeme(), SymbolKind.PARAMETER, UNKNOWN,
                                parameter.getPosition(), scope, -1));
                    }
                }
                statements(node.getBody());
            } finally {
                scope = previousScope;
                loopDepth = previousLoopDepth;
            }
        }

        private SemanticType expression(ExpressionNode node) {
            if (node instanceof LiteralNode) {
                return literalType(((LiteralNode) node).getToken().getType());
            }
            if (node instanceof VariableNode) {
                return variable((VariableNode) node);
            }
            if (node instanceof GroupingNode) {
                return expression(((GroupingNode) node).getExpression());
            }
            if (node instanceof ArrayLiteralNode) {
                for (ExpressionNode element : ((ArrayLiteralNode) node).getElements()) {
                    expression(element);
                }
                return ARRAY;
            }
            if (node instanceof CallExpressionNode) {
                return call((CallExpressionNode) node);
            }
            if (node instanceof UnaryExpressionNode) {
                return unary((UnaryExpressionNode) node);
            }
            if (node instanceof BinaryExpressionNode) {
                BinaryExpressionNode binary = (BinaryExpressionNode) node;
                SemanticType left = expression(binary.getLeft());
                SemanticType right = expression(binary.getRight());
                return binaryType(binary.getOperator().getType(), left, right, binary.getOperator());
            }
            throw new IllegalArgumentException("Expresion AST no soportada: " + node.getClass().getName());
        }

        /** Sin clases ni objetos, los prefijos @, @@ y $ comparten GLOBAL; locales no hacen fallback. */
        private String variableScope(Token token) {
            return token.getType() == TokenType.IDENTIFIER ? scope : SymbolTable.GLOBAL;
        }

        /** Lecturas de @ y $ sin asignacion son UNKNOWN y no declaran una variable para +=. */
        private SemanticType variable(VariableNode node) {
            Token token = node.getToken();
            SemanticSymbol symbol = table.findVariable(token.getLexeme(), variableScope(token));
            if (symbol != null) {
                return symbol.getType();
            }
            if (token.getType() != TokenType.INSTANCE_VARIABLE && token.getType() != TokenType.GLOBAL_VARIABLE) {
                report("Variable no definida: " + token.getLexeme(), token.getPosition());
            }
            return UNKNOWN;
        }

        private SemanticType call(CallExpressionNode node) {
            for (ExpressionNode argument : node.getArguments()) {
                expression(argument);
            }
            SemanticSymbol method = table.findMethod(node.getCallee().getLexeme());
            if (method == null) {
                report("Metodo no definido: " + node.getCallee().getLexeme(), node.getPosition());
            } else if (method.getArity() != node.getArguments().size()) {
                report("Aridad incorrecta para " + method.getName() + ": se esperaban " + method.getArity()
                        + " argumentos y se recibieron " + node.getArguments().size(), node.getPosition());
            }
            // No inferir retornos a traves de llamadas ni ejecutar el cuerpo.
            return UNKNOWN;
        }

        private SemanticType unary(UnaryExpressionNode node) {
            SemanticType operand = expression(node.getExpression());
            if (node.getOperator().getType() != TokenType.MINUS) {
                return BOOLEAN;
            }
            if (operand == UNKNOWN || numeric(operand)) {
                return operand;
            }
            report("Negativo unario incompatible con " + operand, node.getOperator().getPosition());
            return UNKNOWN;
        }

        /** Logicos usan truthiness; BOOLEAN es una simplificacion y DIVIDE numerico infiere FLOAT. */
        private SemanticType binaryType(TokenType operator, SemanticType left, SemanticType right, Token token) {
            switch (operator) {
                case EQUAL: case NOT_EQUAL:
                case LOGICAL_AND: case LOGICAL_OR: case AND: case OR:
                    return BOOLEAN;
                case RANGE_INCLUSIVE: case RANGE_EXCLUSIVE:
                    if (left == UNKNOWN || right == UNKNOWN || (numeric(left) && numeric(right))) {
                        return RANGE;
                    }
                    return incompatible(token, left, right);
                case LESS_THAN: case LESS_EQUAL: case GREATER_THAN: case GREATER_EQUAL:
                    if (left == UNKNOWN || right == UNKNOWN || (numeric(left) && numeric(right))
                            || (left == STRING && right == STRING)) {
                        return BOOLEAN;
                    }
                    return incompatible(token, left, right);
                default:
                    if (left == UNKNOWN || right == UNKNOWN) {
                        return UNKNOWN;
                    }
                    if (operator == TokenType.PLUS && left == STRING && right == STRING) {
                        return STRING;
                    }
                    if (numeric(left) && numeric(right)) {
                        return operator == TokenType.DIVIDE || left == FLOAT || right == FLOAT ? FLOAT : INTEGER;
                    }
                    return incompatible(token, left, right);
            }
        }

        private SemanticType incompatible(Token operator, SemanticType left, SemanticType right) {
            report("Operacion '" + operator.getLexeme() + "' incompatible entre " + left + " y " + right,
                    operator.getPosition());
            return UNKNOWN;
        }

        private static boolean numeric(SemanticType type) {
            return type == INTEGER || type == FLOAT;
        }

        private static TokenType compoundOperator(TokenType type) {
            switch (type) {
                case PLUS_ASSIGN: return TokenType.PLUS;
                case MINUS_ASSIGN: return TokenType.MINUS;
                case MULTIPLY_ASSIGN: return TokenType.MULTIPLY;
                case DIVIDE_ASSIGN: return TokenType.DIVIDE;
                case MODULO_ASSIGN: return TokenType.MODULO;
                default: throw new IllegalArgumentException("Asignacion compuesta no soportada: " + type);
            }
        }

        private static SemanticType literalType(TokenType type) {
            switch (type) {
                case INTEGER: return INTEGER;
                case FLOAT: return FLOAT;
                case STRING: return STRING;
                case TRUE: case FALSE: return BOOLEAN;
                case NIL: return NIL;
                case SYMBOL_LITERAL: return SYMBOL;
                default: throw new IllegalArgumentException("Literal no soportado: " + type);
            }
        }

        private void report(String message, SourcePosition position) {
            errors.add(new SemanticError(message, position));
        }
    }
}
