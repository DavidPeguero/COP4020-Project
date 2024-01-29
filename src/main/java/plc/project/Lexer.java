package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation a lot easier.
 **/
public final class Lexer {

    private final CharStream chars;
    private final String idStart = "(@)|[A-Za-z]";

    // Constructor
    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokenList = new ArrayList<>();
        while (chars.has(0)){
            tokenList.add(lexToken());
        }

        return tokenList;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {

        if ( peek(idStart) ){
            return lexIdentifier();
        }
        else  { // Catch all if token start is not valid
            throw new ParseException("Not a valid token", 0);
        }

    }

    // identifier ::= ( '@' | [A-Za-z] ) [A-Za-z0-9_-]*
    public Token lexIdentifier() {

        // First character is guaranteed a match
        match(idStart);

        // Check character by character
        String idBody = "[A-Za-z0-9_-]*";
        while(chars.has(0))
            if ( !match(idBody) ) // Not able to match while there are still characters in the token
                throw new ParseException("Not a valid Identifier", chars.index);

        // All characters in the string match identifier regex
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexCharacter() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexString() {
        throw new UnsupportedOperationException(); //TODO
    }

    public void lexEscape() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexOperator() {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     * Can pass in regex expression due to the String... patterns parameter
     */
    public boolean peek(String... patterns) {

        // Checks letter by letter to see if the charstream matches the pattern given
        for (int i = 0; i < patterns.length; i++) {

            // Checks letter if it matches the letter in patterns
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i]) ) {

                return false;
            }
        }

        // Charstream matches with the given pattern
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     * Parameter accepts regex expressions
     */
    public boolean match(String... patterns) {
        // Calls peek to get the validity of the charstream
        boolean peek = peek(patterns);

        // If true, advance however many characters is consumed
        if (peek) {
            for (int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }

        // Return the validity of the charstream when compared to the given pattern
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        // Purpose: goes to current index, and starts on new token, returns token identified
        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}

// Notes
/*
*
*
 */
