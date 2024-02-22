package plc.project;

import javax.management.BadAttributeValueExpException;
import java.io.Console;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicStampedReference;

/*
ParserTests (47/53):
    Statement (7/10):
        Assignment (4/7):
            List 1: Incorrect result,
                received Ast.Statement.Assignment
                    {receiver=Ast.Expression.Access
                        {offset=Optional.empty, name='list'},
                        value=Ast.Expression.Access{offset=Optional.empty, name='expr'}
                    }
            List 2: Incorrect result,
                received Ast.Statement.Assignment
                    {receiver=Ast.Expression.Access
                        {offset=Optional.empty, name='list'},
                        value=Ast.Expression.Access{offset=Optional.empty, name='expr1'}
                    }
    Expression (39/41):
        Literal (6/8):
            Char Escapes (0/1):
                Char Escape '\'': Incorrect result,
                    received Ast.Expression.Literal
                        {literal=\}
                Char Escape '\"': Incorrect result,
                    received Ast.Expression.Literal{literal=\}
            String Escapes (0/1):
                String Escape "\'": Incorrect result,
                    received Ast.Expression.Literal{literal=\'}
                String Escape "\"": Incorrect result,
                    received Ast.Expression.Literal{literal=\}
                String Escape "\\": Incorrect result,
                    received Ast.Expression.Literal{literal=\\}
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
 * grammar will have its own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    public void handleError(String message) throws ParseException {
        // Two cases
        // name(expr -> throw index at 9
        // name = ; -> throw index at 7

        // Missing token at end of valid stream of tokens
        if (!tokens.has(0)) {
            throw new ParseException(message, tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        } else {    // Token in the middle of stream of tokens
            throw new ParseException(message, tokens.get(0).getIndex());
        }
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Global> globals = new ArrayList<>();
        List<Ast.Function> functions = new ArrayList<>();
        while(tokens.has(0) && (peek("LIST") || peek("VAR") || peek("VAL"))){
            globals.add(parseGlobal());
        }
        while(tokens.has(0) && peek("FUN")){
            functions.add(parseFunction());
        }
        return new Ast.Source(globals, functions);
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        Ast.Global global = null;
        if(match("LIST")){
            global = parseList();
        } else if (match("VAR")){
            global = parseMutable();
        } else if (match("VAL")){
            global = parseImmutable();
        } else {
            handleError("Not a valid global Start");
        }

        if(!match(";")){
            handleError("Missing Semicolon");
        }

        return global;

    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        String id = tokens.get(0).getLiteral();
        Ast.Global newGlobal = null;
        List<Ast.Expression> expressions = new ArrayList<>();
        tokens.advance();
        if(match("=", "[")){
            //Parse Exception
            expressions.add(parseExpression());
            while(match(",")){
                expressions.add(parseExpression());
            }
            if(match("]")){
                Ast.Expression list = new Ast.Expression.PlcList(expressions);
                newGlobal = new Ast.Global(id, true, Optional.of(list));
            }
        }
        return newGlobal;
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        String id;
        Ast.Global newGlobal = null;
        if(peek(Token.Type.IDENTIFIER)){
            id = tokens.get(0).getLiteral();
            tokens.advance();
            if(match("=")){
                newGlobal = new Ast.Global(id, true, Optional.of(parseExpression()));
                return newGlobal;
            }
            else{
                newGlobal = new Ast.Global(id, true, Optional.empty());
                return newGlobal;
            }
        } else {
            handleError("Expected Identifier");
        }
        return newGlobal;
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        String id;
        Ast.Global newGlobal = null;
        if(peek(Token.Type.IDENTIFIER)){
            id = tokens.get(0).getLiteral();
            tokens.advance();
            if(match("=")){
                newGlobal = new Ast.Global(id, false, Optional.of(parseExpression()));
                return newGlobal;
            }
            else{
                handleError("Expected '=' symbol");
            }
        } else {
            handleError("Expected 'IDENTIFIER' type token ");
        }
        return newGlobal;
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        String id;
        Ast.Function newFunction = null;
        List<String> parameters = new ArrayList<>();
        if(tokens.has(0) && match("FUN")){
            if(peek(Token.Type.IDENTIFIER)){
                id = tokens.get(0).getLiteral();
                tokens.advance();
                if(tokens.has(0) && peek(Token.Type.OPERATOR) && peek("(")) {
                    match("(");
                    //Empty parameter list
                    if (peek(Token.Type.OPERATOR) && peek(")")) {
                        match(")");
                        if(match("DO")){
                            List<Ast.Statement> block = parseBlock();
                            if(match("END")){
                                newFunction = new Ast.Function(id, parameters,block);
                            }
                        }
                    } else { //Check for Identifier if not an empty parameter list
                        if(peek(Token.Type.IDENTIFIER)){
                            parameters.add(tokens.get(0).getLiteral());
                        }
                        //While we find comma indicating more parameters
                        while (peek(Token.Type.OPERATOR) && match(",")) {
                            if(peek(Token.Type.IDENTIFIER)){
                                parameters.add(tokens.get(0).getLiteral());
                            }
                            else{
                                handleError("Expected Token Type 'IDENTIFIER'");
                            }
                            //Add parameters to list
                        }
                        if (!match(")")) { //No matching right parentheses
                            handleError("Expected ')' operator");
                        } else { //Otherwise return the function and the parameters added
                            if(match("DO")){
                                List<Ast.Statement> block = parseBlock();
                                if(match("END")){
                                    newFunction = new Ast.Function(id, parameters, block);
                                }
                                else{
                                    handleError("Expected 'END' keyword");
                                }
                            }
                            else{
                                handleError("Expected 'DO' keyword");
                            }
                        }
                    }
                }
            }
        }

        return newFunction;
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<>();

        while (tokens.has(0)){
            boolean shouldBreak = false;
            switch (tokens.get(0).getLiteral()){
                case "ELSE":
                case "CASE":
                case "DEFAULT":
                case ":":
                case "END":
                    shouldBreak = true;
                default:
            }

            if (shouldBreak) {break;}

            Ast.Statement statement = parseStatement();
            statements.add(statement);
        }

        return statements;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {

        Ast.Statement statement;

        if (peek("LET")){
            statement = parseDeclarationStatement();
        } else if(peek("SWITCH")){
            statement = parseSwitchStatement();
        } else if(peek("IF")){
            statement = parseIfStatement();
        } else if(peek("WHILE")){
            statement = parseWhileStatement();
        } else if (peek("RETURN")) {
            statement = parseReturnStatement();
        } else {
            Ast.Expression expr = parseExpression();
            statement =  new Ast.Statement.Expression(expr);

            if (match("=")){
                Ast.Expression rightExpr = parseExpression();

                if (rightExpr instanceof Ast.Expression.Literal) {
                    statement = new Ast.Statement.Assignment(expr, new Ast.Expression.Access(Optional.empty(), ((Ast.Expression.Literal) rightExpr).getLiteral().toString()));
                }
                else {
                    statement = new Ast.Statement.Assignment(expr, rightExpr);
                }

            }

            if (!match(";"))
                handleError("Expected semicolon");
        }

        return statement;
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        // 'Let' identifier (= expression)? ;
        match("LET");
        if (!match(Token.Type.IDENTIFIER))
            handleError("Expected identifier in declaration");

        // Get identifier literal string name
        String name = tokens.get(-1).getLiteral();

        // Optional type that contains an Ast.Expression
        Optional<Ast.Expression> value = Optional.empty();

        // Creates an Optional<Ast.Expression> object
        if (match("=")){
            value = Optional.of(parseExpression());
        }

        if (!match(";"))
            handleError("Expected ';' in declaration");
        return new Ast.Statement.Declaration(name, value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        // 'IF' expression 'DO' block ('ELSE' block)? 'END'
        // If(Ast.Expression condition, List<Statement> thenStatements, List<Statement> elseStatements)

        match("IF");
        Ast.Expression condition = parseExpression();

        if(!match("DO"))
            handleError("Expected 'DO' keyword");

        List<Ast.Statement> thenStatements = parseBlock();
        List<Ast.Statement> elseStatements = new ArrayList<>();

        if (match("ELSE")){
            elseStatements = parseBlock();
        }

        if (!match("END"))
            handleError("Expected END keyword");

        return new Ast.Statement.If(condition, thenStatements, elseStatements);
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */

    // TODO: Develop unit tests for this
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        // 'SWITCH' expression ('CASE' expression ':' block)* 'DEFAULT' block 'END'

        Ast.Expression condition;
        List<Ast.Statement.Case> cases = new ArrayList<>();

        // case can have optional value and a required list of statements
        match("SWITCH");
        condition = parseExpression();

        while (match("CASE")){
            cases.add(parseCaseStatement());
        }

        if (!match("DEFAULT"))
            handleError("Expected 'DEFAULT' case");

        cases.add(new Ast.Statement.Case(Optional.empty(), parseBlock()));

        if (!match("END"))
            handleError("Expected 'END' keyword in switch statement");

        return new Ast.Statement.Switch(condition, cases);
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        Optional<Ast.Expression> value = Optional.of(parseExpression());

        if (!match(":"))
            handleError("Expected ':' for case condition");

        List<Ast.Statement> statements = parseBlock();
        return new Ast.Statement.Case(value, statements);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        // 'WHILE' expression 'DO' block 'END'

        match("WHILE");
        Ast.Expression condition = parseExpression();

        if (!match("DO"))
            handleError("Expected 'DO' in while statement");

        List<Ast.Statement> statements = parseBlock();

        if (!match("END"))
            handleError("Expected 'END' in while loop");

        return new Ast.Statement.While(condition, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        // 'RETURN' expression ';'
        match("RETURN");

        Ast.Expression value = parseExpression();

        if (!match(";"))
            handleError("Expected ';' at end of return statement");

        return new Ast.Statement.Return(value);
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
                Ast.Expression integer = new Ast.Expression.Literal(new BigInteger(tokens.get(0).getLiteral()));
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
                    match(Token.Type.IDENTIFIER);
                    return new Ast.Expression.Literal(null);
                } else if (tokens.get(0).getLiteral().equals("FALSE")){
                    match(Token.Type.IDENTIFIER);
                    return new Ast.Expression.Literal(Boolean.FALSE);
                } else if (tokens.get(0).getLiteral().equals("TRUE")){
                    match(Token.Type.IDENTIFIER);
                    return new Ast.Expression.Literal(Boolean.TRUE);
                } else{
                    String identifierLiteral = tokens.get(0).getLiteral();
                    match(Token.Type.IDENTIFIER);
                    if(tokens.has(0) && peek(Token.Type.OPERATOR) && peek("(")){
                        List<Ast.Expression> parameters = new ArrayList<Ast.Expression>();
                        match("(");
                        if(peek(Token.Type.OPERATOR) && peek(")")){
                            match(")");
                            return new Ast.Expression.Function(identifierLiteral, parameters);

                        } else { //Check for Identifier if not an empty parameter list
                            parameters.add(parseExpression());
                            //While we find comma indicating more parameters
                            while(peek(Token.Type.OPERATOR) && peek(",")){
                                match(",");
                                parameters.add(parseExpression());
                                //Add parameters to list
                            }
                            if(!match(")")){ //No matching right parentheses
                                handleError("No right parentheses found");
                            } else{ //Otherwise return the function and the parameters added
                                return new Ast.Expression.Function(identifierLiteral, parameters);
                            }
                        }
                    } else if(match("[")){
                        Ast.Expression tempExp = parseExpression();
                        if(match("]"))
                            return new Ast.Expression.Access(Optional.of(tempExp), identifierLiteral);
                        else
                            handleError("No matching right bracket");
                    } else{
                        return new Ast.Expression.Access(Optional.empty(), identifierLiteral);
                    }
                }
            } else if (match("(")) { // '(' expression ')'
                if (!tokens.has(0))
                    handleError("Expecting Expression after Opening Parenthesis");
                Ast.Expression expr = parseExpression();
                if (!match(")"))
                    handleError("Expected a Closing Parenthesis");

                return new Ast.Expression.Group(expr);
            }
        }

        handleError("Expected Primary Expression");
        return null;
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
