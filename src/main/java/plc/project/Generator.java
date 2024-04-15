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
    private void printList(List<Ast.Expression> expressionList){
        if (expressionList.isEmpty())
            return;
        print(expressionList.getFirst());
        for (int i = 1; i < expressionList.size(); i++){
            print(", ");
            print(expressionList.get(i).toString());
        }
        return;
    }

    private void printStatements(List<Ast.Statement> statementsList){
        statementsList.forEach(statement -> {
                newline(indent);
                print(statement);
            }
        );
    }

    // Structurally the same as printStatements but due to type
    // differences must be done in a separate function
    private void printCases(List<Ast.Statement.Case> cases) {
        cases.forEach(aCase -> {
                newline(indent);
                print(aCase);
            }
        );
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        print(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver());
        print(" = ");
        print(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch (");
        print(ast.getCondition());
        print(") {");

        indent++;
        printCases(ast.getCases());
        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        indent++;
        if (ast.getValue().isPresent()) {
            print("case ");
            print(ast.getValue().get());
            print(":");
            printStatements(ast.getStatements());
            newline(indent);
            print("break;");
        } else{
            print("default:");
            printStatements(ast.getStatements());
        }

        indent--;
        return null;
    }

    // Nothing actually tests this function
    // Check lectures to see how professor implemented this
    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (");
        print(ast.getCondition());
        print(") {");

        // Print inner statements on new scope
        indent++;
        printStatements(ast.getStatements());
        indent--;

        // print closing bracket
        if (ast.getStatements().isEmpty())
            newline(indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ");
        print(ast.getValue());
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
            case null:
                print("null");
                break;
            default: // Should print boolean or int or BigDecimal
                print(ast.getLiteral());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        print(ast.getExpression());
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
            print(ast.getLeft());
            print(" ");
            print(ast.getOperator());
            print(" ");
            print(ast.getRight());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        print(ast.getVariable().getJvmName());

        if (ast.getOffset().isPresent()){
            print("[");
            print(ast.getOffset().get());
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
