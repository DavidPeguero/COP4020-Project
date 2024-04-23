package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.function.Function;

final class EndToEndInterpreterTests {

    @ParameterizedTest
    @MethodSource
    void testSource(String test, String input, Object expected) {
        test(input, expected, new Scope(null), Parser::parseSource);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                // FUN main() DO RETURN 0; END
                Arguments.of("Main",
                        "FUN main() DO RETURN 0; END"
                        , BigInteger.ZERO
                ),
                // VAR x: Integer = 1; VAR y: Integer = 10; FUN main() DO x + y; END
                Arguments.of("Globals & No Return",
                        "VAR x: Integer = 1; VAR y: Integer = 10; FUN main() DO x + y; END",
                        Environment.NIL.getValue()
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGlobal(String test, String input, Object expected, String variableName) {
        Scope scope = test(input, Environment.NIL.getValue(), new Scope(null), Parser::parseGlobal);
        Assertions.assertEquals(expected, scope.lookupVariable(variableName).getValue().getValue());
    }

    private static Stream<Arguments> testGlobal() {
        return Stream.of(
                // VAR name: Integer;
                Arguments.of("Mutable",
                        "VAR name: Integer;",
                        Environment.NIL.getValue(),
                        "name"
                ),
                // VAL name: Integer = 1;
                Arguments.of("Immutable",
                        "VAL name: Integer = 1;",
                        BigInteger.ONE,
                        "name"
                )
        );
    }

    @Test
    void testList() {
        // LIST list: Integer = [1, 5, 10];
        List<Object> expected = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.TEN);
        String input = new String("LIST list: Integer = [1, 5, 10];");
        String variableName = new String("list");

        Scope scope = test(input, Environment.NIL.getValue(), new Scope(null), Parser::parseGlobal);
        Assertions.assertEquals(expected, scope.lookupVariable(variableName).getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testFunction(String test, String input, List<Environment.PlcObject> args, Object expected, String functionName) {

        Scope scope = test(input, Environment.NIL.getValue(), new Scope(null), Parser::parseFunction);
        Assertions.assertEquals(expected, scope.lookupFunction(functionName, args.size()).invoke(args).getValue());
    }

    private static Stream<Arguments> testFunction() {
        return Stream.of(
                // FUN main(): Integer DO RETURN 0; END
                Arguments.of("Main",
                        "FUN main(): Integer DO RETURN 0; END",
                        List.of(),
                        BigInteger.ZERO,
                        "main"
                ),
                // FUN square(x: Integer): Integer DO RETURN x * x; END
                Arguments.of("Arguments",
                        "FUN square(x: Integer): Integer DO RETURN x * x; END",
                        List.of(Environment.create(BigInteger.TEN)),
                        BigInteger.valueOf(100),
                        "square"
                )
        );
    }

    @Test
    void testExpressionStatement() {
        // print("Hello, World!");
        PrintStream sysout = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            test("print(\"Hello, World!\");", Environment.NIL.getValue(), new Scope(null), Parser::parseStatement);
            Assertions.assertEquals("Hello, World!" + System.lineSeparator(), out.toString());
        } finally {
            System.setOut(sysout);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testDeclarationStatement(String test, String input, Object expected, String variableName) {
        Scope scope = test(input, Environment.NIL.getValue(), new Scope(null), Parser::parseStatement);
        Assertions.assertEquals(expected, scope.lookupVariable(variableName).getValue().getValue());
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                // LET name;
                Arguments.of("Declaration",
                        "LET name;",
                        Environment.NIL.getValue(),
                        "name"
                ),
                // LET name = 1;
                Arguments.of("Initialization",
                        "LET name = 1;",
                        BigInteger.ONE,
                        "name"
                )
        );
    }

    @Test
    void testVariableAssignmentStatement() {
        // variable = 1;
        Scope scope = new Scope(null);
        scope.defineVariable("variable", true, Environment.create("variable"));
        test("variable = 1;", Environment.NIL.getValue(), scope, Parser::parseStatement);
        Assertions.assertEquals(BigInteger.ONE, scope.lookupVariable("variable").getValue().getValue());
    }

    @Test
    void testListAssignmentStatement() {
        // list[2] = 3;
        List<Object> expected = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.valueOf(3));
        List<Object> list = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.TEN);

        Scope scope = new Scope(null);
        scope.defineVariable("list", true, Environment.create(list));
        test("list[2] = 3;", Environment.NIL.getValue(), scope, Parser::parseStatement);

        Assertions.assertEquals(expected, scope.lookupVariable("list").getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testIfStatement(String test, String input, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("num", true, Environment.NIL);
        test(input, Environment.NIL.getValue(), scope, Parser::parseStatement);
        Assertions.assertEquals(expected, scope.lookupVariable("num").getValue().getValue());
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                // IF TRUE DO num = 1; END
                Arguments.of("True Condition",
                        "IF TRUE DO num = 1; END",
                        BigInteger.ONE
                ),
                // IF FALSE DO ELSE num = 10; END
                Arguments.of("False Condition",
                        "IF FALSE DO ELSE num = 10; END",
                        BigInteger.TEN
                )
        );
    }

    @Test
    void testSwitchStatement() {
        // SWITCH letter CASE 'y': print("yes"); letter = 'n'; DEFAULT print("no"); END
        Scope scope = new Scope(null);
        scope.defineVariable("letter", true, Environment.create('y'));
        String input = new String("SWITCH letter CASE 'y': print(\"yes\"); letter = 'n'; DEFAULT print(\"no\"); END");

        PrintStream sysout = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            test(input, Environment.NIL.getValue(), scope, Parser::parseStatement);
            Assertions.assertEquals("yes" + System.lineSeparator(), out.toString());
        } finally {
            System.setOut(sysout);
        }

        Assertions.assertEquals('n', scope.lookupVariable("letter").getValue().getValue());
    }

    @Test
    void testWhileStatement() {
        // WHILE num < 10 DO num = num + 1; END
        Scope scope = new Scope(null);
        scope.defineVariable("num", true, Environment.create(BigInteger.ZERO));
        test("WHILE num < 10 DO num = num + 1; END",Environment.NIL.getValue(), scope, Parser::parseStatement);
        Assertions.assertEquals(BigInteger.TEN, scope.lookupVariable("num").getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, String input, Object expected) {
        test(input, expected, new Scope(null), Parser::parseExpression);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                // NIL
                Arguments.of("Nil", "NIL", Environment.NIL.getValue()), //remember, special case
                // TRUE
                Arguments.of("Boolean", "TRUE", true),
                // 1
                Arguments.of("Integer", "1", BigInteger.ONE),
                // 1.0
                Arguments.of("Decimal", "1.0", new BigDecimal("1.0")),
                // 'c'
                Arguments.of("Character", "'c'", 'c'),
                // "string"
                Arguments.of("String", "\"string\"", "string")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, String input, Object expected) {
        test(input, expected, new Scope(null), Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                // (1)
                Arguments.of("Literal",
                        "(1)",
                        BigInteger.ONE
                ),
                // (1 + 10)
                Arguments.of("Binary",
                        "(1 + 10)",
                        BigInteger.valueOf(11)
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, String input, Object expected) {
        test(input, expected, new Scope(null), Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                // TRUE && FALSE
                Arguments.of("And",
                        "TRUE && FALSE",
                        false
                ),
                // TRUE || undefined
                Arguments.of("Or (Short Circuit)",
                        "TRUE || undefined",
                        true
                ),
                // 1 < 10
                Arguments.of("Less Than",
                        "1 < 10",
                        true
                ),
                // 1 == 10
                Arguments.of("Equal",
                        "1 == 10",
                        false
                ),
                // "a" + "b"
                Arguments.of("Concatenation",
                        "\"a\" + \"b\"",
                        "ab"
                ),
                // 1 + 10
                Arguments.of("Addition",
                        "1 + 10",
                        BigInteger.valueOf(11)
                ),
                // 1.2 / 3.4
                Arguments.of("Division",
                        "1.2 / 3.4",
                        new BigDecimal("0.4")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, String input, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("variable", true, Environment.create("variable"));
        test(input, expected, scope, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                // variable
                Arguments.of("Variable",
                        "variable",
                        "variable"
                )
        );
    }

    @Test
    void testListAccessExpression() {
        // list[1]
        List<Object> list = Arrays.asList(BigInteger.ONE, BigInteger.valueOf(5), BigInteger.TEN);

        Scope scope = new Scope(null);
        scope.defineVariable("list", true, Environment.create(list));
        test("list[1]", BigInteger.valueOf(5), scope, Parser::parseExpression);
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, String input, Object expected) {
        Scope scope = new Scope(null);
        scope.defineFunction("function", 0, args -> Environment.create("function"));
        test(input, expected, scope, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                // function()
                Arguments.of("Function",
                        "function()",
                        "function"
                ),
                // print("Hello, World!")
                Arguments.of("Print",
                        "print(\"Hello, World!\")",
                        Environment.NIL.getValue()
                )
        );
    }

    private static <T extends Ast> Scope test(String input, Object expected, Scope scope, Function<Parser, T> function) {
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer.lex());

        Ast ast = function.apply(parser);

        Interpreter interpreter = new Interpreter(scope);
        if (expected != null) {
            Assertions.assertEquals(expected, interpreter.visit(ast).getValue());
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(ast));
        }
        return interpreter.getScope();
    }



}
