package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false),

                Arguments.of("Special Symbols", "[aaaaa", false),
                Arguments.of("Multiple @", "@ad@m", false),
                Arguments.of("@ at Location other than 0", "testing@", false),
                Arguments.of("Special characters", "hello/World", false),
                Arguments.of("Leading Underscore", "_wow", false),
                Arguments.of("Invalid Character", "ron;ld_mcd0nald", false),
                Arguments.of("One Character False", "+", false),
                Arguments.of("Last Character False", "variable;", false),

                Arguments.of("Leading @", "@commandName", true),
                Arguments.of("Mixing Underscores", "Hello_World_", true),
                Arguments.of("Mixing Hyphens", "Hello-World-", true),
                Arguments.of("Mixing Hyphens and Underscores", "Hello_-World-_", true),
                Arguments.of("Single Character @", "@", true),
                Arguments.of("Single Character", "a", true)

        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Large integer with trailing zeroes", "100000000000", true),
                Arguments.of("Leading Zero", "01", false),
                Arguments.of("Negative Zero Integer", "-0", false),
                Arguments.of("Decimal", "1.2", false),
                Arguments.of("Decimal", "00", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("0.123", "0.123", true),
                Arguments.of("Decimal with Identifier", "0.123a", false),
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Trailing Zeroes", "1.00000", true),
                Arguments.of("Negative Zero Decimal", "-0.0", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Single Digit", "1", false),
                Arguments.of("Double Decimal", "1..1", false),
                Arguments.of("Multiple Decimal Points", "1.11.1", false),
                Arguments.of("Comma Decimal (European Style)", "1,112", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("@ Symbol", "\'@\'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Unterminated Character", "\'", false),
                Arguments.of("Multiple", "\'abc\'", false),

                // New Tests
                Arguments.of("Terminated Only", "a\'", false),
                Arguments.of("Tabline Escape", "\'\\t\'", true),
                Arguments.of("Escape Literal", "\'\n\'", false),
                Arguments.of("Escape Character", "\'\\\'", false),
                Arguments.of("Single Quotes Surrounding Single Quote", "\'\'\'", false),
                Arguments.of("Single Quotes Surrounding Escaped Single Quote", "\'\\\'\'", true),
                Arguments.of("Multiple Escape Valid Sequences", "\'\\\'\\n\\r\'", false),
                Arguments.of("Invalid Escape", "\'\\g\'", false),
                Arguments.of("Number", "\'4\'", true),
                Arguments.of("^ Symbol", "\'^\'", true),
                Arguments.of("White space", "\' \'", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("AND", "&&", true),
                Arguments.of("OR", "||", true),
                Arguments.of("More ANDs", "&&&", false),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),
                Arguments.of("Single =", "=", true),
                Arguments.of("Single !", "!", true),
                Arguments.of("Incomplete Compound OR", "|", true),
                Arguments.of("Incomplete Compound AND", "&", true),
                Arguments.of("Character Alphabet", "a", false),
                Arguments.of("Identifier @", "@", false),
                Arguments.of("Character", "[", true),
                Arguments.of("Multiple Operators", "-)", false),
                Arguments.of("Incorrect Compound", "!!", false),
                Arguments.of("Incorrect Compound 2", "||&", false),
                Arguments.of("Multiple Operators Separated by Whitespace", "! ==", false),
                Arguments.of("Multiple Operators Separated by Whitespace", "( ==", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Example Multiple Decimal Points", "1.11.1;", Arrays.asList(
                        new Token(Token.Type.DECIMAL, "1.11", 0),
                        new Token(Token.Type.OPERATOR, ".", 4),
                        new Token(Token.Type.INTEGER, "1", 5),
                        new Token(Token.Type.OPERATOR, ";", 6)
                ))
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}
