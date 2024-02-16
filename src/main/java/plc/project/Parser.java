package plc.project;

import javax.management.BadAttributeValueExpException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicStampedReference;

/*
ParserTests (31/36):
    Expr (20/23):
        // FIXED: Literal (6/7): Nil Literal: Incorrect result, received Ast.Expression.Access{offset=Optional.empty, name='NIL'}
        Priority (1/3):
            // FIXED: And Or: Incorrect result,
                received Ast.Expression.Binary{operator='&&', left=Ast.Expression.Access{offset=Optional.empty, name='expr1'},
                        right=Ast.Expression.Binary{operator='||', left=Ast.Expression.Access{offset=Optional.empty, name='expr2'},
                        right=Ast.Expression.Access{offset=Optional.empty, name='expr3'}}}
            //FIXED: Equals Not Equals: Incorrect result,
                received Ast.Expression.Binary{operator='==', left=Ast.Expression.Access{offset=Optional.empty, name='expr1'},
                        right=Ast.Expression.Binary{operator='!=', left=Ast.Expression.Access{offset=Optional.empty, name='expr2'},
                        right=Ast.Expression.Access{offset=Optional.empty, name='expr3'}}}
        Error (1/3):
            // FIXED: Missing Closing Parenthesis: Incorrect index, received 2.
            // FIXED: Invalid Closing Parenthesis: Incorrect index, received 2.

 */

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {

        // Technically the last check needed in parsing statements
        if (tokens.has(0)){

            // expression (= expression)? ;
            String receiver = tokens.get(0).getLiteral();
            Ast.Expression expr = parseExpression();

            Ast.Statement statement;
            if (peek("=")) { // Go into an assignment expression
                String op = tokens.get(0).getLiteral();
                match("=");
                String value = tokens.get(0).getLiteral();

                statement = new Ast.Statement.Assignment(
                        new Ast.Expression.Access(Optional.empty(), receiver),
                        new Ast.Expression.Access(Optional.empty(), value));

                tokens.advance();

                if (!match(";"))
                    throw new ParseException("Missing Semicolon", tokens.index);
                else
                    return statement;
            }

            if (match(";")) { // Only the single expression
                return new Ast.Statement.Expression(expr);
            } else
                throw new ParseException("Missing Semicolon", tokens.index);
        }

        throw new ParseException("Invalid Statement", tokens.index);
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        // 'Let'
        // 'SWITCH'
        // 'IF'
        // 'WHILE

        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression expression = parseComparisonExpression();

        while (peek(Token.Type.OPERATOR)){
            switch (tokens.get(0).getLiteral()) {
                case "&&":
                case "||":
                    String operator = tokens.get(0).getLiteral();
                    match(operator);
                    Ast.Expression right = parseComparisonExpression();
                    expression = new Ast.Expression.Binary(
                        operator,
                        expression,
                        right
                );
                    continue;
                default:
            }
            break;
        }
        return expression;
    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression additiveExpression = parseAdditiveExpression();

        while (peek(Token.Type.OPERATOR)){
            switch (tokens.get(0).getLiteral()) {
                case "<":
                case ">":
                case "==":
                case "!=":
                    String operator = tokens.get(0).getLiteral();
                    match(operator);
                    Ast.Expression rightAdditiveExpression = parseAdditiveExpression();
                    additiveExpression = new Ast.Expression.Binary(
                            operator,
                            additiveExpression,
                            rightAdditiveExpression
                    );
                    continue;
                default:
            }
            break;
        }
        return additiveExpression;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression multiplicativeExpression = parseMultiplicativeExpression();

        while (peek(Token.Type.OPERATOR)){
            switch (tokens.get(0).getLiteral()) {
                case "+":
                case "-":
                    String operator = tokens.get(0).getLiteral();
                    match(operator);
                    Ast.Expression rightMultiplicativeExpression = parseMultiplicativeExpression();
                    multiplicativeExpression = new Ast.Expression.Binary(
                            operator,
                            multiplicativeExpression,
                            rightMultiplicativeExpression
                    );
                    continue;
                default:
            }
            break;
        }
        return multiplicativeExpression;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression leftPrimaryExpression = parsePrimaryExpression();

        while (peek(Token.Type.OPERATOR)) {
            switch (tokens.get(0).getLiteral()) {
                case "*":
                case "/":
                case "^":
                    String operator = tokens.get(0).getLiteral();
                    match(operator);
                    Ast.Expression rightPrimaryExpression = parsePrimaryExpression();
                    leftPrimaryExpression = new Ast.Expression.Binary(
                            operator,
                            leftPrimaryExpression,
                            rightPrimaryExpression
                    );
                    continue;
                default:
            }
            break;
        }
        return leftPrimaryExpression;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (tokens.has(0)) {
            if (peek(Token.Type.DECIMAL)) {
                Ast.Expression.Literal decimal = new Ast.Expression.Literal(new BigDecimal(tokens.get(0).getLiteral()));
                match(Token.Type.DECIMAL);
                return decimal;
            } else if (peek(Token.Type.INTEGER)) {
                Ast.Expression.Literal integer = new Ast.Expression.Literal(new BigInteger(tokens.get(0).getLiteral()));
                match(Token.Type.INTEGER);
                return integer;
            } else if (peek(Token.Type.STRING)) {
                String formattedString = tokens.get(0).getLiteral();
                formattedString = formattedString.replace("\"", "");
                formattedString = formattedString.replace("\\b", "\b");
                formattedString = formattedString.replace("\\n", "\n");
                formattedString = formattedString.replace("\\r", "\r");
                formattedString = formattedString.replace("\\t", "\t");
                Ast.Expression.Literal string = new Ast.Expression.Literal(formattedString);
                match(Token.Type.STRING);
                return string;
            } else if (peek(Token.Type.CHARACTER)) {
                String formattedChar = tokens.get(0).getLiteral();
                formattedChar = formattedChar.replace("'", "");
                formattedChar = formattedChar.replace("\\b", "\b");
                formattedChar = formattedChar.replace("\\n", "\n");
                formattedChar = formattedChar.replace("\\r", "\r");
                formattedChar = formattedChar.replace("\\t", "\t");
                Ast.Expression.Literal character = new Ast.Expression.Literal(formattedChar.charAt(0));
                match(Token.Type.CHARACTER);
                return character;
            } else if (peek(Token.Type.IDENTIFIER)) {
                if (tokens.get(0).getLiteral().equals("NIL") || tokens.get(0).getLiteral() == null) {
                    tokens.advance();
                    return new Ast.Expression.Literal(null);
                } else if (tokens.get(0).getLiteral().equals("FALSE")){
                    tokens.advance();
                    return new Ast.Expression.Literal(Boolean.FALSE);
                } else if (tokens.get(0).getLiteral().equals("TRUE")){
                    tokens.advance();
                    return new Ast.Expression.Literal(Boolean.TRUE);
                } else{
                    String identifierLiteral = tokens.get(0).getLiteral();
                    tokens.advance();
                    if(tokens.has(0) && peek(Token.Type.OPERATOR) && peek("(")){
                        List<Ast.Expression> parameters = new ArrayList<Ast.Expression>();
                        tokens.advance();
                        if(peek(Token.Type.OPERATOR) && peek(")")){
                            tokens.advance();
                            return new Ast.Expression.Function(identifierLiteral, parameters);

                        } else { //Check for Identifier if not an empty parameter list
                            parameters.add(parseExpression());
                            //While we find comma indicating more parameters
                            while(peek(Token.Type.OPERATOR) && peek(",")){
                                tokens.advance();
                                parameters.add(parseExpression());
                                //Add parameters to list
                            }
                            if(!match(")")){ //No matching right parentheses
                                throw new ParseException("No right parentheses found", tokens.index);
                            } else{ //Otherwise return the function and the parameters added
                                return new Ast.Expression.Function(identifierLiteral, parameters);
                            }
                        }
                    } else if(match("[")){
                        Ast.Expression tempExp = parseExpression();

                        if(match("]"))
                            return new Ast.Expression.Access(Optional.of(tempExp), identifierLiteral);
                        else
                            throw new ParseException("No matching right bracket", tokens.index);
                    } else{
                        return new Ast.Expression.Access(Optional.empty(), identifierLiteral);
                    }
                }
            } else if (peek("(")) { // '(' expression ')'
                match("(");

                if (!tokens.has(0))
                    throw new ParseException("Missing Expression", tokens.index);

                // Getting accurate token index according to ParseException Specs
                int expressionLength = tokens.get(0).getLiteral().length();
                Ast.Expression expr = parseExpression();
                if (!match(")"))
                    throw new ParseException("Expected a Right Parenthesis", tokens.index + expressionLength);

                return new Ast.Expression.Group(expr);
            }
        }
        throw new ParseException("Invalid Primary Expression", tokens.index);
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++){
            if (!tokens.has(i)){
                return false;
            } else if (patterns[i] instanceof Token.Type){
                if (patterns[i] != tokens.get(i).getType()){
                    return false;
                }
            } else if (patterns[i] instanceof String){
                if (!patterns[i].equals(tokens.get(i).getLiteral())){
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + 
                patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++){
                tokens.advance();
            }
        }

        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
