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
        if(ast.getValue().isPresent()){
            scope.defineVariable(ast.getName(), ast.getMutable(), visit(ast.getValue().get()));
        } else {
            scope.defineVariable(ast.getName(), ast.getMutable(), Environment.NIL);
        }
        return Environment.NIL;
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
        if(ast.getValue().isPresent()){
            scope.defineVariable(ast.getName(), true, visit(ast.getValue().get()));
        } else {
            scope.defineVariable(ast.getName(), true , Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access))
            throw new RuntimeException("Expected Access Type");
        if (!scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName()).getMutable())
            throw new RuntimeException("Modification of Immutable Type");

        if (((Ast.Expression.Access) ast.getReceiver()).getOffset().isPresent()){
            Environment.PlcObject receiver = visit(((Ast.Expression.Access) ast.getReceiver()).getOffset().get());
            if(!(receiver.getValue() instanceof BigInteger offset))
                throw new RuntimeException("Expected BigInteger type for offset");

            Environment.Variable variable = scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName());
            // Warning here says unchecked cast, just ignore it :skull_emoji:
            @SuppressWarnings("unchecked")
            List<Object> list = requireType(List.class, variable.getValue());
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
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        Environment.PlcObject plcLHS;
        Environment.PlcObject plcRHS;
        Boolean leftHand;
        Boolean rightHand;
        /* TODO:
         *
         * Use requireType and Environment.create as needed
         * Check description for project specs
         */
        Object LHS;
        Object RHS;
        switch(ast.getOperator()){
            case "&&":
                leftHand = requireType(Boolean.class, visit(ast.getLeft()));
                try{
                    rightHand = requireType(Boolean.class, visit(ast.getRight()));
                    return Environment.create(leftHand && rightHand);
                }
                catch (RuntimeException e){
                    return Environment.create(leftHand);
                }
            case "||":
                leftHand = requireType(Boolean.class, visit(ast.getLeft()));
                try{
                    rightHand = requireType(Boolean.class, visit(ast.getRight()));
                    return Environment.create(leftHand || rightHand);
                }
                catch (RuntimeException e){
                    return Environment.create(leftHand);
                }
            case "<":
                plcLHS = visit(ast.getLeft());
                plcRHS = visit(ast.getRight());

                if(plcRHS.getValue() instanceof Comparable && plcLHS.getValue() instanceof Comparable){
                    if(plcLHS.getValue().getClass().equals(plcRHS.getValue().getClass())){
                        if(plcLHS.getValue() instanceof BigInteger){
                            int compareResult = requireType(BigInteger.class, plcLHS).compareTo(requireType(BigInteger.class, plcRHS));
                            switch (compareResult){
                                case -1:
                                    return Environment.create(Boolean.TRUE);
                                case 1:
                                    return Environment.create(Boolean.FALSE);
                            }
                        }else if(plcLHS.getValue() instanceof BigDecimal){
                            int compareResult = requireType(BigInteger.class, plcLHS).compareTo(requireType(BigInteger.class, plcRHS));
                            switch (compareResult){
                                case -1:
                                    return Environment.create(Boolean.TRUE);
                                case 0:
                                case 1:
                                    return Environment.create(Boolean.FALSE);
                            }
                        } else if(plcLHS.getValue() instanceof Boolean){
                            int compareResult = requireType(Boolean.class, plcLHS).compareTo(requireType(Boolean.class, plcRHS));
                            switch (compareResult){
                                case -1:
                                case 0:
                                    return Environment.create(Boolean.FALSE);
                                case 1:
                                    return Environment.create(Boolean.TRUE);
                            }
                        } else if(plcLHS.getValue() instanceof Character){
                            int compareResult = requireType(Character.class, plcLHS).compareTo(requireType(Character.class, plcRHS));
                            switch (compareResult){
                                case -1:
                                    return Environment.create(Boolean.TRUE);
                                case 0:
                                case 1:
                                    return Environment.create(Boolean.FALSE);
                            }
                        }
                    }
                }
                break;
            case ">":
                plcLHS = visit(ast.getLeft());
                plcRHS = visit(ast.getRight());

                if(plcRHS.getValue() instanceof Comparable && plcLHS.getValue() instanceof Comparable){
                    if(plcLHS.getValue().getClass().equals(plcRHS.getValue().getClass())){
                        if(plcLHS.getValue() instanceof BigInteger){
                            int compareResult = requireType(BigInteger.class, plcLHS).compareTo(requireType(BigInteger.class, plcRHS));
                            switch (compareResult){
                                case -1:
                                    return Environment.create(Boolean.TRUE);
                                case 0:
                                case 1:
                                    return Environment.create(Boolean.FALSE);
                            }
                        }else if(plcLHS.getValue() instanceof BigDecimal){
                            int compareResult = requireType(BigDecimal.class, plcLHS).compareTo(requireType(BigDecimal.class, plcRHS));
                            switch (compareResult){
                                case -1:
                                    return Environment.create(Boolean.FALSE);
                                case 0:
                                case 1:
                                    return Environment.create(Boolean.TRUE);
                            }
                        }else if(plcLHS.getValue() instanceof Boolean){
                            int compareResult = requireType(Boolean.class, plcLHS).compareTo(requireType(Boolean.class, plcRHS));
                            switch (compareResult){
                                case -1:
                                case 0:
                                    return Environment.create(Boolean.FALSE);
                                case 1:
                                    return Environment.create(Boolean.TRUE);
                            }
                        }else if(plcLHS.getValue() instanceof Character){
                            int compareResult = requireType(Character.class, plcLHS).compareTo(requireType(Character.class, plcRHS));
                            switch (compareResult){
                                case -1:
                                    return Environment.create(Boolean.TRUE);
                                case 0:
                                case 1:
                                    return Environment.create(Boolean.FALSE);
                            }
                        }
                    }
                }
                break;
            case "==":
                plcLHS = visit(ast.getLeft());
                plcRHS = visit(ast.getRight());
                if(plcLHS.getValue().getClass().equals(plcRHS.getValue().getClass())) {
                    return Environment.create(plcLHS.getValue().equals(plcRHS.getValue()));
                }
                break;
            case "!=":
                plcLHS = visit(ast.getLeft());
                plcRHS = visit(ast.getRight());
                if(plcLHS.getValue().getClass().equals(plcRHS.getValue().getClass())) {
                    return Environment.create(!plcLHS.getValue().equals(plcRHS.getValue()));
                }
                break;
            case "+":
                // If either is a string; concatenate
                // if LHS is number/decimal then RHS must match
                LHS = visit(ast.getLeft()).getValue();
                RHS = visit(ast.getRight()).getValue();
                if (LHS instanceof String || RHS instanceof String){
                    return Environment.create(LHS.toString() + RHS.toString());
                }
                if (LHS instanceof BigDecimal && RHS instanceof BigDecimal){
                    return Environment.create(((BigDecimal) LHS).add((BigDecimal) RHS));
                }
                if (LHS instanceof BigInteger && RHS instanceof BigInteger){
                    return Environment.create(((BigInteger) LHS).add((BigInteger) RHS));
                }

                throw new RuntimeException("LHS and RHS must match type or one must be a string");
            case "-":
                LHS = visit(ast.getLeft()).getValue();
                RHS = visit(ast.getRight()).getValue();
                if (LHS instanceof BigDecimal && RHS instanceof BigDecimal){
                    return Environment.create(((BigDecimal) LHS).subtract((BigDecimal) RHS));
                }
                if (LHS instanceof BigInteger && RHS instanceof BigInteger){
                    return Environment.create(((BigInteger) LHS).subtract((BigInteger) RHS));
                }
                throw new RuntimeException("LHS and RHS must match type");
            case "*":
                LHS = visit(ast.getLeft()).getValue();
                RHS = visit(ast.getRight()).getValue();
                if (LHS instanceof BigDecimal && RHS instanceof BigDecimal){
                    return Environment.create(((BigDecimal) LHS).multiply((BigDecimal) RHS));
                }
                if (LHS instanceof BigInteger && RHS instanceof BigInteger){
                    return Environment.create(((BigInteger) LHS).multiply((BigInteger) RHS));
                }
                throw new RuntimeException("LHS and RHS must match type");
            case "/":
                LHS = visit(ast.getLeft()).getValue();
                RHS = visit(ast.getRight()).getValue();

                if (LHS == null || RHS == null)
                    throw new RuntimeException("Division with null value");

                if (RHS.toString().equals("0") || RHS.toString().equals("0.0"))
                    throw new RuntimeException("Divide by Zero");

                if (LHS instanceof BigDecimal && RHS instanceof BigDecimal){
                    return Environment.create((((BigDecimal) LHS).divide((BigDecimal) RHS, RoundingMode.HALF_EVEN)));
                }
                if (LHS instanceof BigInteger && RHS instanceof BigInteger){
                    return Environment.create(((BigInteger) LHS).divide((BigInteger) RHS));
                }

                throw new RuntimeException("LHS and RHS must match type");
            case "^":
                break;
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        // Case of direct access to variable
        if (!ast.getOffset().isPresent())
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
        //Create an array of generic objects, since we don't know what they will evaluate to
        List<Object> plcList = new ArrayList<>();
        List<Ast.Expression> expressions = ast.getValues();
        //Visit each expression and add it to the plcList
        expressions.forEach(exp -> plcList.add(visit(exp).getValue()));
        return Environment.create(plcList);
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
