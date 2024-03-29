package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    /*
    * Visits globals followed by functions (following the left-depth-first traversal of the AST).
    * Throws a RuntimeException if:
    *   A main/0 function (name = main, arity = 0) does not exist.
    *   The main/0 function does not have an Integer return type.
    * Returns null.
    */
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    // TODO: Test this
    @Override
    public Void visit(Ast.Expression.Literal ast) {
        // https://stackoverflow.com/questions/5579309/is-it-possible-to-use-the-instanceof-operator-in-a-switch-statement
        switch (ast.getLiteral()){
            case Character c : ast.setType(Environment.Type.CHARACTER); break;
            case Boolean b : ast.setType(Environment.Type.BOOLEAN); break;
            case String s : ast.setType(Environment.Type.STRING); break;
            case null : ast.setType(Environment.Type.NIL); break;
            case BigInteger i :
                if (i.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0 ||
                        i.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0)
                    throw new RuntimeException("Integer value out of bounds");
                ast.setType(Environment.Type.INTEGER);
                break;
            case BigDecimal d :
                if (d.doubleValue() == Double.NEGATIVE_INFINITY ||
                        d.doubleValue() == Double.POSITIVE_INFINITY)
                    throw new RuntimeException("Decimal value out of bounds");
                ast.setType(Environment.Type.DECIMAL);
                break;
            default:
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (!target.equals(type))
            throw new RuntimeException("Assignable target does not match type");
    }

}
