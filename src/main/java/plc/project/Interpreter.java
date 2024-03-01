package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        // Assignments to immutable should result in failure
        // Check that receiver is of Ast.Expression.Access else fail
        // Assign to current scope
        // Return NIL

        if (!(ast.getReceiver() instanceof Ast.Expression.Access))
            throw new RuntimeException("Expected Access Type");
        if (!scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName()).getMutable())
            throw new RuntimeException("Modification of Immutable Type");

        if (((Ast.Expression.Access) ast.getReceiver()).getOffset().isPresent()){
            Environment.PlcObject receiver = visit(((Ast.Expression.Access) ast.getReceiver()).getOffset().get());
            if(!(receiver.getValue() instanceof BigInteger offset))
                throw new RuntimeException("Expected BigInteger type for offset");

            Environment.Variable variable = scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName());
            if(!(variable.getValue().getValue() instanceof List))
                throw new RuntimeException("Expected list");

            // Warning here says unchecked cast, just ignore it :skull_emoji:
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) variable.getValue().getValue();
            list.set(offset.intValue(), visit(ast.getValue()).getValue());
        } else{
            scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName()).setValue(visit(ast.getValue()));
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() == null)
            return Environment.NIL;
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        switch(ast.getOperator()){
            case "&&":
                break;
            case "||":
                break;
            case "<":
                break;
            case ">":
                break;
            case "==":
                break;
            case "!=":
                break;
            case "+":
                break;
            case "-":
                break;
            case "*":
                break;
            case "/":
                break;
            case "^":
                break;
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        // Case of direct access to variable
        if (ast.getOffset().isEmpty())
            return scope.lookupVariable(ast.getName()).getValue();

        // Offset is of incorrect type
        if (!(visit(ast.getOffset().get()).getValue() instanceof BigInteger offset))
            throw new RuntimeException("Expected BigDecimal type for offset access");

        // Search for list variable in scope or parent scopes
        Environment.Variable variable = scope.lookupVariable(ast.getName());

        @SuppressWarnings("unchecked")
        List<Ast.Expression> list = requireType(List.class, variable.getValue());

        // Offset value is out of bounds
        if (offset.intValue() < 0 || offset.intValue() >= list.size())
            throw new RuntimeException("Offset is out of list bounds");

        // return environment object with literal value in offset
        return Environment.create(list.get(offset.intValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {

        return Environment.create(ast.getValues());
        // throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
