package plc.project;

import javax.management.BadAttributeValueExpException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
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
        Ast.Expression left_result = parseComparisonExpression();

        if (peek(Token.Type.OPERATOR)){
            switch (tokens.get(0).getLiteral()) {
                case "&&":
                case "||":
                    String operator = tokens.get(0).getLiteral();
                    match(operator);
                    return new Ast.Expression.Binary(
                            operator,
                            left_result,
                            parseLogicalExpression()
                    );
            }
        }
        return left_result;
    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression left_result = parseAdditiveExpression();

        if (peek(Token.Type.OPERATOR)){
            switch (tokens.get(0).getLiteral()) {
                case "<":
                case ">":
                case "==":
                case "!=":
                    String operator = tokens.get(0).getLiteral();
                    match(operator);
                    return new Ast.Expression.Binary(
                            operator,
                            left_result,
                            parseComparisonExpression()
                    );
            }
        }
        return left_result;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression left_result = parseMultiplicativeExpression();

        if (peek(Token.Type.OPERATOR)){
            switch (tokens.get(0).getLiteral()) {
                case "+":
                case "-":
                    String operator = tokens.get(0).getLiteral();
                    match(operator);
                    return new Ast.Expression.Binary(
                            operator,
                            left_result,
                            parseAdditiveExpression()
                    );
            }
        }
        return left_result;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression left_result = parsePrimaryExpression();

        if (peek(Token.Type.OPERATOR)) {
            switch (tokens.get(0).getLiteral()) {
                case "*":
                case "/":
                case "^":
                    String operator = tokens.get(0).getLiteral();
                    match(operator);
                    return new Ast.Expression.Binary(
                            operator,
                            left_result,
                            parseMultiplicativeExpression()
                    );
            }
        }
        return left_result;
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
                Ast.Expression.Literal string = new Ast.Expression.Literal(tokens.get(0).getLiteral());
                match(Token.Type.STRING);
                return string;
            } else if (peek(Token.Type.CHARACTER)) {
                Ast.Expression.Literal character = new Ast.Expression.Literal(tokens.get(0).getLiteral());
                match(Token.Type.CHARACTER);
                return character;
            }else if (peek(Token.Type.IDENTIFIER)) {
                if (tokens.get(0).getLiteral() == null) {
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
                            return new Ast.Expression.Function(identifierLiteral, parameters);

                        } else { //Check for Identifier if not an empty parameter list
                            parameters.add(parseExpression());
                            //While we find comma indicating more parameters
                            while(peek(Token.Type.OPERATOR) && peek(",")){
                                tokens.advance();
                                parameters.add(parseExpression());
                                //Add parameters to list
                            }
                            if(!match(')')){ //No matching right parentheses
                                throw new ParseException("No right parentheses found", tokens.index);
                            } else{ //Otherwise return the function and the parameters added
                                return new Ast.Expression.Function(identifierLiteral, parameters);
                            }
                        }
                    } else if(peek("[")){

                    } else{
                        return new Ast.Expression.Literal(identifierLiteral);
                    }
                }
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
