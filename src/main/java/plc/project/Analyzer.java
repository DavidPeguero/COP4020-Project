package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
        for (Ast.Global global : ast.getGlobals())
            visit(global);

        for (Ast.Function function : ast.getFunctions())
            visit(function);

        if(!scope.lookupFunction("main", 0).getReturnType().equals(Environment.Type.INTEGER))
            throw new RuntimeException("Main does not have an Integer return type");

        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if(ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            if(ast.getValue().get() instanceof  Ast.Expression.PlcList){
                ((Ast.Expression.PlcList) ast.getValue().get()).getValues().forEach(expression -> {
                   requireAssignable(Environment.getType(ast.getTypeName()), expression.getType());
                });
            }
            else
                requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
        }
        scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), ast.getMutable(), Environment.NIL);
        ast.setVariable(scope.lookupVariable(ast.getName()));

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        List<Environment.Type> parameterTypes = new ArrayList<>();
        ast.getParameterTypeNames().forEach(typeName -> {
            parameterTypes.add(Environment.getType(typeName));
        });
        Environment.Type retType = Environment.Type.NIL;
        if(ast.getReturnTypeName().isPresent())
            retType = Environment.getType(ast.getReturnTypeName().get());
        scope.defineFunction(ast.getName(), ast.getName(), parameterTypes, retType, args -> Environment.NIL);
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));
        scope = new Scope(scope);
        scope.defineVariable("retType", true, Environment.create(retType));
        ast.getParameters().forEach(parameter -> {
            scope.defineVariable(parameter, true, Environment.NIL);
        });
        ast.getStatements().forEach(this::visit);
        scope = scope.getParent();

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Function))
            throw new RuntimeException("Expected function in statement");
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        if(ast.getValue().isPresent())
            visit(ast.getValue().get());
        if(ast.getTypeName().isPresent()) {
            switch (ast.getTypeName().get()) {
                case "Integer":
                    if(ast.getValue().isPresent())
                        requireAssignable(Environment.Type.INTEGER, ast.getValue().get().getType());
                    ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, true, Environment.NIL));
                    break;
                case "String":
                    if(ast.getValue().isPresent())
                        requireAssignable(Environment.Type.STRING, ast.getValue().get().getType());
                    ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.STRING, true, Environment.NIL));
                    break;
                case "Decimal":
                    if(ast.getValue().isPresent())
                        requireAssignable(Environment.Type.DECIMAL, ast.getValue().get().getType());
                    ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.DECIMAL, true, Environment.NIL));
                    break;
                case "Boolean":
                    if(ast.getValue().isPresent())
                        requireAssignable(Environment.Type.BOOLEAN, ast.getValue().get().getType());
                    ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.BOOLEAN, true, Environment.NIL));
                    break;
                case "Character":
                    if(ast.getValue().isPresent())
                        requireAssignable(Environment.Type.CHARACTER, ast.getValue().get().getType());
                    ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.CHARACTER, true, Environment.NIL));
                    break;
                case "Any":
                    ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.ANY, true, Environment.NIL));
                    break;
                case "Comparable":
                    if(ast.getValue().isPresent()){
                        requireAssignable(Environment.Type.COMPARABLE, ast.getValue().get().getType());
                    }
                    ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.COMPARABLE, true, Environment.NIL));
                    break;
                default:
                    throw new RuntimeException("Unknown type for type name");
            }
        } else if(ast.getValue().isPresent()){
                ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), ast.getValue().get().getType(), true, Environment.NIL));
        } else {
            throw new RuntimeException("Missing type in both typename or inferred type from value");
        }


        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        // First check if receiver is an access otherwise return
        // visit access to make sure that is valid
        // check if value is assignable to ast
        // Check the type of receiver and make sure there is a valid value being assigned to it.
        if(!(ast.getReceiver() instanceof  Ast.Expression.Access)){
            throw new RuntimeException("Not an access expression");
        }
        visit(ast.getReceiver());
        visit(ast.getValue());
        Environment.Variable variable = scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName());

        requireAssignable(variable.getType(),ast.getValue().getType());

        return null;
//        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        if (ast.getThenStatements().isEmpty())
            throw new RuntimeException("Empty if-body block");

        visit(ast.getThenStatements());
        visit(ast.getElseStatements());

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        visit(ast.getCondition());
        Environment.Type conditionType = ast.getCondition().getType();
        for (Ast.Statement.Case currCase : ast.getCases()){
            visit(currCase);
            if (currCase.equals(ast.getCases().getLast()) && currCase.getValue().isPresent())   // Default with condition
                throw new RuntimeException("Unexpected condition in Default case");
            if (currCase.equals(ast.getCases().getLast()))                                      // Successful default
                continue;
            if (currCase.getValue().isEmpty())                                                  // Invalid Case
                throw new RuntimeException("Expected condition in case expression");
            visit(currCase.getValue().get());
            requireAssignable(conditionType, currCase.getValue().get().getType());              // Require matching type in Case
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        visit(ast.getStatements());
        return null;
    }

    // Done in class
    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try{
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getStatements()){
                visit((stmt));
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        requireAssignable((Environment.Type)(scope.lookupVariable("retType").getValue().getValue()), ast.getValue().getType());
        return null;
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
        if (!(ast.getExpression() instanceof Ast.Expression.Binary))
            throw new RuntimeException("Group Expression must contain a binary expression");

        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());
        Environment.Type LHS = ast.getLeft().getType();
        Environment.Type RHS = ast.getRight().getType();
        switch (ast.getOperator()){
            case "||":
            case "&&":
                requireAssignable(LHS, Environment.Type.BOOLEAN);
                requireAssignable(RHS, Environment.Type.BOOLEAN);
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "<":
            case ">":
            case "==":
            case "!=":
                requireAssignable(LHS, Environment.Type.COMPARABLE);
                requireAssignable(RHS, Environment.Type.COMPARABLE);
                requireAssignable(RHS, LHS);
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "+":
                if (LHS.equals(Environment.Type.STRING) || RHS.equals(Environment.Type.STRING)){
                    ast.setType(Environment.Type.STRING);
                    break;
                }
            case "-":
            case "*":
            case "/":
                if (LHS.equals(Environment.Type.DECIMAL)){
                    requireAssignable(RHS, LHS);
                    ast.setType(LHS);
                    break;
                }
            case "^":
                if (LHS.equals(Environment.Type.INTEGER)){
                    requireAssignable(RHS, LHS);
                    ast.setType(LHS);
                    break;
                }
                throw new RuntimeException("Invalid type for Binary Expression");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if(ast.getOffset().isPresent()) {
            System.out.println(ast.getOffset().get().getType());
            requireAssignable(ast.getOffset().get().getType(), Environment.Type.INTEGER);
        }

        switch (scope.lookupVariable(ast.getName()).getType().getName()){
            case "Integer":
                ast.setVariable(new Environment.Variable(ast.getName(), ast.getName(), Environment.Type.INTEGER, true, Environment.NIL));
                break;
            case "String":
                ast.setVariable(new Environment.Variable(ast.getName(), ast.getName(), Environment.Type.STRING, true, Environment.NIL));
                break;
            case "Decimal":
                ast.setVariable(new Environment.Variable(ast.getName(), ast.getName(), Environment.Type.DECIMAL, true, Environment.NIL));
                break;
            case "Boolean":
                ast.setVariable(new Environment.Variable(ast.getName(), ast.getName(), Environment.Type.BOOLEAN, true, Environment.NIL));
                break;
            case "Character":
                ast.setVariable(new Environment.Variable(ast.getName(), ast.getName(), Environment.Type.CHARACTER, true, Environment.NIL));
                break;
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        for(int i = 0; i < function.getParameterTypes().size(); i++){
            visit(ast.getArguments().get(i));
            requireAssignable(function.getParameterTypes().get(i), ast.getArguments().get(i).getType());
        }
        ast.setFunction(new Environment.Function(ast.getName(), function.getJvmName(), function.getParameterTypes(), function.getReturnType(), args->  Environment.NIL ));
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        ast.getValues().forEach(this::visit);
        return null;
    }

    public Void visit(List<Ast.Statement> statements){
        scope = new Scope(scope);
        for (Ast.Statement statement: statements)
            visit(statement);
        scope = scope.getParent();

        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if(target.equals(Environment.Type.ANY))
            return;
        else if(target.equals(Environment.Type.COMPARABLE)) {
            if (!type.equals(Environment.Type.INTEGER)
                    && !type.equals(Environment.Type.DECIMAL)
                    && !type.equals(Environment.Type.STRING)
                    && !type.equals(Environment.Type.CHARACTER)
                    && !type.equals(Environment.Type.COMPARABLE)) {
                throw new RuntimeException("Assignable target does not match type comparable");
            }
        }
        else if (!target.equals(type)) throw new RuntimeException("Assignable target does not match type");
    }

}
