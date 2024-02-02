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
            if(match("[" + whiteSpace + "]")){
                lexEscape();
            } else {
                tokenList.add(lexToken());
            }
        }

        return tokenList;
    }

    private final String alphabet = "A-Za-z";
    private final String numbers = "0-9";
    private final String whiteSpace = " \b\n\r\t";
    private final String alphanumeric = alphabet + numbers;
    private final String idStart = "@|[" + alphabet + "]";
    private final String opStart = "[!=]|&|\\||[^" + alphanumeric + whiteSpace +"]";

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        // "([!=]=)?|&&|\\|\\||[^A-Za-z0-9_@]"

        String numberStart = "0|-|[1-9]";
        if ( peek(idStart) ){
            return lexIdentifier();
        } else if ( peek("'") ) {
            return lexCharacter();
        } else if( peek("\"") ){
            return lexString();
        } else if ( peek(numberStart) ) {
            return lexNumber();
        } else if ( peek(opStart) ) {
            return lexOperator();
        } else {
            throw new ParseException("Not a valid token", chars.index);
        }
    }

    // identifier ::= ( '@' | [A-Za-z] ) [A-Za-z0-9_-]*
    public Token lexIdentifier() {

        // First character is guaranteed a match
        match(idStart);

        // Check character by character
        String idBody = "[" + alphanumeric + "_-]*";
        while(chars.has(0))
            if ( !match(idBody) ) // Not able to match while there are still characters in the token
                return chars.emit(Token.Type.IDENTIFIER);

        // All characters in the string match identifier regex
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        boolean isDecimal = false;

        if (match("-")) {
            if (match("[1-9]")) {
                while (chars.has(0)) {

                    // By default, check if there are any numbers to add to integer
                    match("[" + numbers + "]");

                    // Check if decimal point exists
                    if (match("\\.", "[" + numbers + "]") && !isDecimal){
                        isDecimal = true;
                    }
                    else {
                        // Returns decimal or integer if no more matches are available
                        if (isDecimal) {
                            return chars.emit(Token.Type.DECIMAL);
                        }
                        return chars.emit(Token.Type.INTEGER);
                    }
                }
            }
            else if (match("0")) {
                if (!match("\\.", "[" + numbers + "]")) {
                    throw new ParseException("Invalid number", chars.index);
                }

                while (chars.has(0)) {
                    if (!match("[" + numbers + "]")){
                        return chars.emit(Token.Type.DECIMAL);
                    }
                }
                return chars.emit(Token.Type.DECIMAL);
            }
        }
        else if (match("0")) {
            if (!match("\\.", "[" + numbers + "]")) {
                return chars.emit(Token.Type.INTEGER);
            }

            while (chars.has(0)) {
                if (!match("[" + numbers + "]")){
                    return chars.emit(Token.Type.DECIMAL);
                }
            }
            return chars.emit(Token.Type.DECIMAL);
        }

        // 1111.078
        else if (match("[1-9]")) {
            while (chars.has(0)) {

                // By default, check if there are any numbers to add to integer
                if (match("[" + numbers + "]")) {
                    continue;
                }

                // Check if decimal point exists
                else if (peek("\\.", "[" + numbers + "]") && !isDecimal){
                    match("\\.", "[" + numbers + "]");
                    isDecimal = true;
                }
                else {
                    // Returns decimal or integer if no more matches are available
                    if (isDecimal) {
                        return chars.emit(Token.Type.DECIMAL);
                    }
                    return chars.emit(Token.Type.INTEGER);
                }
            }
        }

        if (isDecimal)
            return chars.emit(Token.Type.DECIMAL);

        return chars.emit(Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        //Match on single quote initially
        match("'");

        while(chars.has(0)){
            if(match("\\\\")){
                if(match("[bnrt'\"\\\\]")) {
                    if(match("'")){
                        return chars.emit(Token.Type.CHARACTER);
                    }
                    else{
                        throw new ParseException("Not a valid character length", chars.index);
                    }

                }
                else{
                    throw new ParseException("Invalid Escape", chars.index);
                }
            }
            else if(match("[^\\n\\r'\\\\]")){
                if(match("'")){
                    return chars.emit(Token.Type.CHARACTER);
                }
                else{
                    throw new ParseException("Not a valid character length", chars.index);
                }
            } else if (match("'")) {
                throw new ParseException("Not valid: empty character token", chars.index);
            } else {
                throw new ParseException("Not valid: empty character token", chars.index);
            }
        }

        throw new ParseException("Unterminated Character Token", chars.index);
    }

    public Token lexString() {


        match("\"");

        while(chars.has(0)){
             if (match("\"")) {
                return chars.emit(Token.Type.STRING);
             }
             else if (match("\\\\")) {
                 if(!match("[bnrt'\"\\\\]")) {
                     throw new ParseException("Invalid String Input", chars.index);
                 }
             }
             else{
                 match("[^\"\\\\]");
             }
        }
        throw new ParseException("Unterminated String", chars.index);
    }

    public void lexEscape() {
        // Skip escape characters
        // Call chars.advance and then chars.skip
        chars.skip();
    }

    private void checkCompound(char input) {

        // If the compound is ever called after finding the second character, it is not a valid token
//        if (chars.length >= 1)
//            throw new ParseException("Not a valid Operation Token", chars.index);

        // Start of new compound
        match(opStart);

        // Check if following character matches compound requirements
        if (chars.has(0) && chars.get(0) == input)
            match("\\" + String.valueOf(input));
    }

    public Token lexOperator() {
        // "([!=]=)?|&&|\\|\\||[^A-Za-z0-9_@]"
        // Different cases where we check for compound or single operator
        switch (chars.get(0)){
            case '!':
            case '=':
                checkCompound('=');
                break;
            case '&':
                checkCompound('&');
                break;
            case '|':
                checkCompound('|');
                break;
            default:
                match("[^" + idStart + whiteSpace + "]");
        }

        // Reaches this case if we have a valid operator
        return chars.emit(Token.Type.OPERATOR);
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
        // String input
        private int index = 0;
        // Current index
        private int length = 0;
        // Possible token length

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
