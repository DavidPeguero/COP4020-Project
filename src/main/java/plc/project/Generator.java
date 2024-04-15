package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    // Print items in list
    private Void printList(List<Ast.Expression> expressionList){
        if (expressionList.isEmpty())
            return null;
        print(expressionList.getFirst());
        for (int i = 1; i < expressionList.size(); i++){
            print(", ");
            print(expressionList.get(i).toString());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        if(ast.getMutable()){
            if(ast.getValue().isPresent() && ast.getValue().get() instanceof Ast.Expression.PlcList) {
                print(ast.getVariable().getJvmName() + "[] " + ast.getName() + " = {");
                visit(ast.getValue().get());
                print("};");
            }
            print(ast.getVariable().getJvmName() + " " + ast.getName());
            if(ast.getValue().isPresent()) {
                print(" = ");
                visit(ast.getValue().get());
            }
            print(";");
        } else{
            print("final" + ast.getVariable().getJvmName() + " " + ast.getName() + " = ");
            visit(ast.getValue().get());
            print(";");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        print(ast.getFunction().getReturnType().getJvmName() + " " + ast.getName() + "(");
        List<Environment.Type> pTypes = ast.getFunction().getParameterTypes();
        List<String> parameters = ast.getParameters();
        for(int i = 0; i < parameters.size() - 1; i++){
            print(pTypes.get(i) + " " + parameters.get(i) + " ");
        }
        print(pTypes.getLast()+ " " + parameters.getLast() + " {");
        print();
        indent++;
        newline(indent);
        ast.getStatements().forEach(this::visit);
        indent--;
        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        indent++;
        print("switch (");
        visit(ast.getCondition());
        print(") {");

        ast.getCases().forEach(aCase -> {
            newline(indent);
            visit(aCase);
            }
        );
        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        indent++;
        if (ast.getValue().isPresent()) {
            print("case ");
            visit(ast.getValue().get());
            print(":");
            ast.getStatements().forEach(statement -> {
                newline(indent);
                visit(statement);
                }
            );

            newline(indent);
            print("break;");
        } else{
            print("default:");
            ast.getStatements().forEach(statement -> {
                newline(indent);
                visit(statement);
                }
            );
        }

        indent--;
        return null;
    }

    // Nothing actually tests this function
    // Check lectures to see how professor implemented this
    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (");
        visit(ast.getCondition());
        print(") {");
        indent++;
        ast.getStatements().forEach(statement -> {newline(indent);
            visit(statement);
            }
        );
        print("}");
        indent--;
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        switch(ast.getLiteral()){
            case String s :
                print("\"");
                print(s);
                print("\"");
                break;
            case Character c:
                print("'");
                print(c);
                print("'");
                break;
            default: // Should print boolean or int or BigDecimal
                print(ast.getLiteral());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if (ast.getOperator().equals("^")){
            print("Math.pow(");
            printList(Arrays.asList(ast.getLeft(), ast.getRight()));
            print(")");
        }
        else{
            visit(ast.getLeft());
            print(" ");
            print(ast.getOperator());
            print(" ");
            visit(ast.getRight());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        print(ast.getVariable().getJvmName());

        if (ast.getOffset().isPresent()){
            print("[");
            visit(ast.getOffset().get());
            print("]");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getFunction().getJvmName());
        print("(");
        printList(ast.getArguments());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        print("{");
        printList(ast.getValues());
        print("}");
        return null;
    }

}
