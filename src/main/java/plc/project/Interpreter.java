package plc.project;

import java.io.Console;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Formatter;
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

        // From lecture
        scope.defineFunction("logarithm", 1, args -> {

            // Can shorten by using requireType here (see bigDecimal2)
            if (!(args.get(0).getValue() instanceof BigDecimal))
                throw new RuntimeException("expected type BigDecimal. received,  " +
                        args.get(0).getValue().getClass().getName());
            BigDecimal bigDecimal1 = (BigDecimal) args.get(0).getValue();


            BigDecimal bigDecimal2 = requireType(BigDecimal.class, Environment.create(args.get(0).getValue()));

            BigDecimal res = BigDecimal.valueOf(Math.log(bigDecimal2.doubleValue()));
            return Environment.create(res);
        });

        // From Lecture
//        scope.defineFunction("converter", 2, args -> {
//            BigInteger base10Decimal = requireType(BigInteger.class, Environment.create(args.get(0).getValue()));
//            BigInteger base = requireType(BigInteger.class, Environment.create(args.get(1).getValue()));
//        });
    }

    public Scope getScope() {
        return scope;
    }   

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        ast.getGlobals().forEach(this::visit);
        ast.getFunctions().forEach(this::visit);

        return scope.lookupFunction("main", 0).invoke(new ArrayList<>());
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
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            try{
                List<String> parameter = ast.getParameters();
                for(int i = 0; i <ast.getParameters().size(); i++){
                    scope.defineVariable(parameter.get(i), true, Environment.create(args.get(i).getValue()));
                    System.out.println(scope.lookupVariable(parameter.get(i)));
                }
                ast.getStatements().forEach(this::visit);
            }
            catch (Return e){
                return e.value;
            }
            return Environment.NIL;
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {

        if (ast.getExpression() instanceof Ast.Expression.Function)
            visit(ast.getExpression());

        return Environment.NIL;
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

        try {
            scope = new Scope(scope);
            if (requireType(Boolean.class, visit(ast.getCondition()))) {
                ast.getThenStatements().forEach(this::visit);
            } else {
                ast.getElseStatements().forEach(this::visit);
            }
        } finally {
            scope = scope.getParent();
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        /* TODO:
         *  Inside of a new scope, if the condition is equivalent to a CASE value,
         *  evaluate the statements for that case, otherwise evaluate the statements of the DEFAULT.
         *  Returns NIL.
         */
        try {
            scope = new Scope(scope);
            Environment.PlcObject condition = visit(ast.getCondition());

            int size = 0;
            for (Ast.Statement.Case _case : ast.getCases()){
                // Match on first case value
                if (_case.getValue().isPresent() && visit(_case.getValue().get()).getValue().equals(condition.getValue())) {
                    visit(_case);
                    break;
                }
                else if (size == ast.getCases().size() - 1) // DEFAULT CASE
                    visit(_case);
                size++;
            }
        } finally {
            scope = scope.getParent();
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {

        try{
            scope = new Scope(scope);
            ast.getStatements().forEach(this::visit);
        }
        finally {
            scope = scope.getParent();
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {

        while(requireType(Boolean.class, visit(ast.getCondition()))){
            try {
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
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
        Environment.PlcObject LHS = visit(ast.getLeft());
        Environment.PlcObject RHS = visit(ast.getRight());
        Boolean leftHand;
        Boolean rightHand;

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
                if(RHS.getValue() instanceof Comparable && LHS.getValue() instanceof Comparable){
                    if(LHS.getValue().getClass().equals(RHS.getValue().getClass())){
                        if(LHS.getValue() instanceof BigInteger){
                            int compareResult = requireType(BigInteger.class, LHS).compareTo(requireType(BigInteger.class, RHS));
                            switch (compareResult){
                                case -1:
                                    return Environment.create(Boolean.TRUE);
                                case 0:
                                case 1:
                                    return Environment.create(Boolean.FALSE);
                            }
                        }else if(LHS.getValue() instanceof BigDecimal){
                            int compareResult = requireType(BigInteger.class, LHS).compareTo(requireType(BigInteger.class, RHS));
                            switch (compareResult){
                                case -1:
                                    return Environment.create(Boolean.TRUE);
                                case 0:
                                case 1:
                                    return Environment.create(Boolean.FALSE);
                            }
                        } else if(LHS.getValue() instanceof Boolean){
                            int compareResult = requireType(Boolean.class, LHS).compareTo(requireType(Boolean.class, RHS));
                            switch (compareResult){
                                case -1:
                                case 0:
                                    return Environment.create(Boolean.FALSE);
                                case 1:
                                    return Environment.create(Boolean.TRUE);
                            }
                        } else if(LHS.getValue() instanceof Character){
                            int compareResult = requireType(Character.class, LHS).compareTo(requireType(Character.class, RHS));
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
                if(RHS.getValue() instanceof Comparable && LHS.getValue() instanceof Comparable){
                    if(LHS.getValue().getClass().equals(RHS.getValue().getClass())){
                        if(LHS.getValue() instanceof BigInteger){
                            int compareResult = requireType(BigInteger.class, LHS).compareTo(requireType(BigInteger.class, RHS));
                            switch (compareResult){
                                case -1:
                                    return Environment.create(Boolean.TRUE);
                                case 0:
                                case 1:
                                    return Environment.create(Boolean.FALSE);
                            }
                        }else if(LHS.getValue() instanceof BigDecimal){
                            int compareResult = requireType(BigDecimal.class, LHS).compareTo(requireType(BigDecimal.class, RHS));
                            switch (compareResult){
                                case -1:
                                    return Environment.create(Boolean.FALSE);
                                case 0:
                                case 1:
                                    return Environment.create(Boolean.TRUE);
                            }
                        }else if(LHS.getValue() instanceof Boolean){
                            int compareResult = requireType(Boolean.class, LHS).compareTo(requireType(Boolean.class, RHS));
                            switch (compareResult){
                                case -1:
                                case 0:
                                    return Environment.create(Boolean.FALSE);
                                case 1:
                                    return Environment.create(Boolean.TRUE);
                            }
                        }else if(LHS.getValue() instanceof Character){
                            int compareResult = requireType(Character.class, LHS).compareTo(requireType(Character.class, RHS));
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
                if(LHS.getValue().getClass().equals(RHS.getValue().getClass())) {
                    return Environment.create(LHS.getValue().equals(RHS.getValue()));
                }
                break;
            case "!=":
                if(LHS.getValue().getClass().equals(RHS.getValue().getClass())) {
                    return Environment.create(!LHS.getValue().equals(RHS.getValue()));
                }
                break;
            case "+":
                if (LHS.getValue() instanceof BigDecimal && RHS.getValue() instanceof BigDecimal){
                    return Environment.create(((BigDecimal) LHS.getValue()).add((BigDecimal) RHS.getValue()));
                }
                if (LHS.getValue() instanceof BigInteger && RHS.getValue() instanceof BigInteger){
                    return Environment.create(((BigInteger) LHS.getValue()).add((BigInteger) RHS.getValue()));
                }
                if (LHS.getValue() instanceof String || RHS.getValue() instanceof String){
                    return Environment.create(LHS.getValue().toString() + RHS.getValue().toString());
                }
                if (LHS.getValue().toString().equals("nil") || RHS.getValue().toString().equals("nil"))
                    return Environment.create(LHS.toString().concat(RHS.toString()));

                throw new RuntimeException("LHS and RHS must match type or one must be a string");
            case "-":
                if (LHS.getValue() instanceof BigDecimal && RHS.getValue() instanceof BigDecimal){
                    return Environment.create(((BigDecimal) LHS.getValue()).subtract((BigDecimal) RHS.getValue()));
                }
                if (LHS.getValue() instanceof BigInteger && RHS.getValue() instanceof BigInteger){
                    return Environment.create(((BigInteger) LHS.getValue()).subtract((BigInteger) RHS.getValue()));
                }
                throw new RuntimeException("LHS and RHS must match type");
            case "*":
                if (LHS.getValue() instanceof BigDecimal && RHS.getValue() instanceof BigDecimal){
                    return Environment.create(((BigDecimal) LHS.getValue()).multiply((BigDecimal) RHS.getValue()));
                }
                if (LHS.getValue() instanceof BigInteger && RHS.getValue() instanceof BigInteger){
                    return Environment.create(((BigInteger) LHS.getValue()).multiply((BigInteger) RHS.getValue()));
                }
                throw new RuntimeException("LHS and RHS must match type");
            case "/":
                if (LHS == null || RHS == null)
                    throw new RuntimeException("Division with null value");

                if (RHS.toString().equals("0") || RHS.toString().equals("0.0"))
                    throw new RuntimeException("Divide by Zero");

                if (LHS.getValue() instanceof BigDecimal && RHS.getValue() instanceof BigDecimal)
                    return Environment.create((((BigDecimal) LHS.getValue()).divide((BigDecimal) RHS.getValue(), RoundingMode.HALF_EVEN)));
                if (LHS.getValue() instanceof BigInteger && RHS.getValue() instanceof BigInteger)
                    return Environment.create(((BigInteger) LHS.getValue()).divide((BigInteger) RHS.getValue()));

                throw new RuntimeException("Division on unmatched or invalid type");
            case "^":
                if (!(LHS.getValue() instanceof BigInteger) || !(RHS.getValue() instanceof BigInteger))
                    throw new RuntimeException("Exponentiation on invalid type");

                BigInteger res = ((BigInteger) LHS.getValue()).pow(((BigInteger) RHS.getValue()).intValue());

                return  Environment.create(res);
        }

        return Environment.NIL;
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
        Environment.PlcObject returnVal;
        try{
            scope = new Scope(scope);
            Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
            if (function.getArity() == 0){
                returnVal = function.invoke(new ArrayList<>());
            }
            else {
                List<Environment.PlcObject> arguments = new ArrayList<>();
                ast.getArguments().forEach( (arg) -> {

                    arguments.add(visit(arg));
                });
                returnVal = function.invoke(arguments);
            }
        }
        finally {
            scope = scope.getParent();
        }
        return returnVal;
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
