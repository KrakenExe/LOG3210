package analyzer.visitors;

import analyzer.SemantiqueError;
import analyzer.ast.*;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.NullType;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created: 19-01-10
 * Last Changed: 22-01-29
 * Author: Esther Guerrier
 * Modified by: Hakim Mektoub
 * <p>
 * Description: Ce visiteur explorer l'AST est renvois des erreur lorqu'une erreur sémantique est détecté.
 */

public class SemantiqueVisitor implements ParserVisitor {

    private final PrintWriter writer;

    private HashMap<String, VarType> symbolTable = new HashMap<>(); // mapping variable -> type

    // variable pour les metrics
    private int VAR = 0;
    private int WHILE = 0;
    private int IF = 0;
    private int FOR = 0;
    private int OP = 0;
    private boolean error = false;

    public SemantiqueVisitor(PrintWriter writer) {
        this.writer = writer;
    }

    //Vous pouvez utilisez cette fonction pour imprimer vos erreurs.
    private void print(final String msg) {
        if (!error) {
            writer.print(msg);
            error = true;
        }
    }

    /*
    Le Visiteur doit lancer des erreurs lorsqu'un situation arrive.

    regardez l'énoncé ou les tests pour voir le message à afficher et dans quelle situation.
    Lorsque vous voulez afficher une erreur, utilisez la méthode print implémentée ci-dessous.
    Tous vos tests doivent passer!!

     */

    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        node.childrenAccept(this, data);
        print(String.format("{VAR:%d, WHILE:%d, IF:%d, FOR:%d, OP:%d}", VAR, WHILE, IF, FOR, OP));

        return null;
    }

    /*
    Ici se retrouve les noeuds servant à déclarer une variable.
    Certaines doivent enregistrer les variables avec leur type dans la table symbolique.
     */
    @Override
    public Object visit(ASTDeclaration node, Object data) {
        node.childrenAccept(this, data);
        //VAR++;
        return null;
    }

    @Override
    public Object visit(ASTNormalDeclaration node, Object data) {
        String varName = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        String typeName = node.getValue();

        VarType type = VarType.num;

        if (typeName.equals("bool")){
            type = VarType.bool;
        }else if(typeName.equals("real")){
            type = VarType.real;
        }

        // on vérifie si la déclaration est unique
        if(symbolTable.containsKey(varName)){
            print(String.format("Invalid declaration... variable %s already exists",varName));
        } else {
            symbolTable.put(varName, type);
            VAR++;
        }


        return null;
    }

    @Override
    public Object visit(ASTListDeclaration node, Object data) {
        String varName = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        String typeName = node.getValue();

        VarType type = VarType.listnum;

        if(typeName.equals("listbool")){
            type = VarType.listbool;
        }else if(typeName.equals("listreal")){
            type = VarType.listreal;
        }

        if(symbolTable.containsKey(varName)){
            print(String.format("Invalid declaration... variable %s already exists",varName));
        } else {
            symbolTable.put(varName, type);
            VAR++;
        }

        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }


    @Override
    public Object visit(ASTStmt node, Object data) {

        node.childrenAccept(this, data);
        return null;
    }

    /*
     * Il faut vérifier que le type déclaré à gauche soit compatible avec la liste utilisée à droite. N'oubliez pas
     * de vérifier que les variables existent.
     */

    @Override
    public Object visit(ASTForEachStmt node, Object data) {
        FOR++;
        node.jjtGetChild(0).jjtAccept(this, data);
        node.jjtGetChild(1).jjtAccept(this, data);
        node.jjtGetChild(2).jjtAccept(this, data);
        String varName = ((ASTIdentifier)node.jjtGetChild(0).jjtGetChild(0)).getValue();
        String listName = ((ASTIdentifier)node.jjtGetChild(1)).getValue();
        VarType leftType = symbolTable.get(varName);
        VarType rightType = symbolTable.get(listName);

        if(rightType!=VarType.listbool && rightType!=VarType.listnum && rightType!=VarType.listreal) {
            print("Array type is required here...");
        }
        else if(!(rightType.equals(VarType.listbool) && leftType.equals(VarType.bool)
                || rightType.equals(VarType.listnum) && leftType.equals(VarType.num)
                || rightType.equals(VarType.listreal) && leftType.equals(VarType.real))) {
            print(String.format("Array type %s is incompatible with declared variable of type %s...",symbolTable.get(listName),leftType));
        }

        return null;
    }

    /*
    Ici faites attention!! Lisez la grammaire, c'est votre meilleur ami :)
     */
    @Override
    public Object visit(ASTForStmt node, Object data) {
        FOR++;
        callChildenCond(node);
        return null;
    }

    /*
    Méthode recommandée à implémenter puisque vous remarquerez que quelques fonctions ont exactement le même code! N'oubliez
    -pas que la qualité du code est évalué :)
     */
    private void callChildenCond(SimpleNode node) {
        DataStruct childData = new DataStruct();
        node.jjtGetChild(0).jjtAccept(this, childData);
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            node.jjtGetChild(i).jjtAccept(this, childData);
        }
        if(!childData.type.equals(VarType.bool)){
            print("Invalid type in condition");
        }
    }

    /*
    les structures conditionnelle doivent vérifier que leur expression de condition est de type booléenne
    On doit aussi compter les conditions dans les variables IF et WHILE
     */
    @Override
    public Object visit(ASTIfStmt node, Object data) {
        IF++;
        callChildenCond(node);
        return null;
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        WHILE++;
        callChildenCond(node);
        return null;
    }

    /*
    On doit vérifier que le type de la variable est compatible avec celui de l'expression.
    La variable doit etre déclarée.
     */
    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        DataStruct childData = new DataStruct();
        node.childrenAccept(this, childData);
        String varName = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        VarType expectedType = symbolTable.get(varName);
        VarType actualType = childData.type;
        if(!symbolTable.containsKey(varName)){
            print(String.format("Invalid use of undefined Identifier %s",varName));
        }

        else if(!expectedType.equals(actualType)){
            print(String.format("Invalid type in assignation of Identifier %s... was expecting %s but got %s", varName, expectedType, actualType));
        }

        return null;
    }

    @Override
    public Object visit(ASTExpr node, Object data) {
        //Il est normal que tous les noeuds jusqu'à expr retourne un type.
        DataStruct childData = new DataStruct();
        node.childrenAccept(this, childData);
        if(data!=null){
            ((DataStruct)data).type = childData.type;
        }
        return null;
    }

    @Override
    public Object visit(ASTCompExpr node, Object data) {
        /*attention, ce noeud est plus complexe que les autres.
        si il n'a qu'un seul enfant, le noeud a pour type le type de son enfant.

        si il a plus d'un enfant, alors ils s'agit d'une comparaison. il a donc pour type "bool".

        de plus, il n'est pas acceptable de faire des comparaisons de booleen avec les opérateur < > <= >=.
        les opérateurs == et != peuvent être utilisé pour les nombres, les réels et les booléens, mais il faut que le type soit le même
        des deux côté de l'égalité/l'inégalité.
        */

        List<VarType> types = new ArrayList<VarType>();

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            DataStruct childData = new DataStruct();
            node.jjtGetChild(i).jjtAccept(this, childData);
            types.add(childData.type);
        }

        if(node.jjtGetNumChildren()==1){
            ((DataStruct)data).type = types.get(0);
        }

        if(node.jjtGetNumChildren()>1){
            OP++;
            ((DataStruct)data).type = VarType.bool;
            boolean containsEquals = node.getValue().equals("==");
            boolean containsNotEquals = node.getValue().equals("!=");
            boolean containsLesser = node.getValue().equals("<");
            boolean containsGreater = node.getValue().equals(">");
            boolean containsLesserOrEqual = node.getValue().equals("<=");
            boolean containsGreaterOrEqual = node.getValue().equals(">=");
            boolean leftISBool = types.get(0).equals(VarType.bool);
            boolean RightIsBool = types.get(1).equals(VarType.bool);
            boolean sameTypesOnBothSides = types.get(0).equals(types.get(1));


            if(!containsEquals && !containsNotEquals && !containsLesser && !containsGreater && !containsLesserOrEqual && !containsGreaterOrEqual ||
                    !containsEquals && !containsNotEquals && leftISBool || !containsEquals && !containsNotEquals && RightIsBool || !sameTypesOnBothSides){
                print("Invalid type in expression");
            }
        }

        return null;
    }

    private void callChildren(SimpleNode node, Object data, VarType validType) {

    }

    /*
    Opérateur binaire
    s'il n'y a qu'un enfant, aucune vérification à faire.
    Par exemple, un AddExpr peut retourner le type "Bool" à condition de n'avoir qu'un seul enfant.
    Sinon, il faut s'assurer que les types des valeurs sont les mêmes des deux cotés de l'opération
     */
    @Override
    public Object visit(ASTAddExpr node, Object data) {
        List<VarType> types = new ArrayList<VarType>();
        OP+= node.getOps().size();
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            DataStruct childData = new DataStruct();
            node.jjtGetChild(i).jjtAccept(this, childData);
            ((DataStruct)data).type = childData.type;
            if(node.getOps().size()>0 && childData.type != null){
                types.add(childData.type);
            }
        }

        if(!types.isEmpty()){
            VarType firstType = types.get(0);
            for(VarType type:types){
                if(!firstType.equals(type)){
                    print("Invalid type in expression");
                }
            }
        }

        return null;
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        List<VarType> types = new ArrayList<VarType>();
        OP+= node.getOps().size();
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            DataStruct childData = new DataStruct();
            node.jjtGetChild(i).jjtAccept(this, childData);
            ((DataStruct)data).type = childData.type;
            if(node.getOps().size()>0 && childData.type != null){
                types.add(childData.type);
            }
        }

        if(!types.isEmpty()){
            VarType firstType = types.get(0);
            for(VarType type:types){
                if(!firstType.equals(type)){
                    print("Invalid type in expression");
                }
            }
        }

        return null;
    }

    @Override
    public Object visit(ASTBoolExpr node, Object data) {
        List<VarType> types = new ArrayList<VarType>();
        OP+=node.getOps().size();
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            DataStruct childData = new DataStruct();
            node.jjtGetChild(i).jjtAccept(this, childData);
            ((DataStruct)data).type = childData.type;
            if(node.getOps().size()>0 && childData.type != null){
                types.add(childData.type);
            }
        }

        if(node.jjtGetNumChildren()==1 && !types.isEmpty()){
            ((DataStruct)data).type = types.get(0);
        }

        if(node.jjtGetNumChildren()>1 && !types.isEmpty()){
            ((DataStruct)data).type = VarType.bool;
        }

        for(VarType type:types){
            if(!type.equals(VarType.bool)){
                print("Invalid type in expression");
            }
        }

        return null;
    }

    /*
    opérateur unaire
    les opérateur unaire ont toujours un seul enfant.

    Cependant, ASTNotExpr et ASTUnaExpr ont la fonction "getOps()" qui retourne un vecteur contenant l'image (représentation str)
    de chaque token associé au noeud.

    Il est utile de vérifier la longueur de ce vecteur pour savoir si une opérande est présente.

    si il n'y a pas d'opérande, ne rien faire.
    si il y a une (ou plus) opérande, ils faut vérifier le type.

    */
    @Override
    public Object visit(ASTNotExpr node, Object data) {
        DataStruct childData = new DataStruct();
        node.jjtGetChild(0).jjtAccept(this, childData);
        OP+=node.getOps().size();
        if(node.getOps().size() > 0 && childData.type != null){
            if(node.getOps().contains("!") && !childData.type.equals(VarType.bool)){
                print("Invalid type in expression");
            }
        }

        ((DataStruct)data).type = childData.type;
        return null;
    }

    @Override
    public Object visit(ASTUnaExpr node, Object data) {
        DataStruct childData = new DataStruct();
        node.jjtGetChild(0).jjtAccept(this, childData);
        OP+=node.getOps().size();
        if(node.getOps().size() > 0 && childData.type != null){

            if(node.getOps().contains("-") && childData.type.equals(VarType.bool)){
                print("Invalid type in expression");
            }
        }

        ((DataStruct)data).type = childData.type;
        return null;
    }

    /*
    les noeud ASTIdentifier aillant comme parent "GenValue" doivent vérifier leur type et vérifier leur existence.

    Ont peut envoyé une information a un enfant avec le 2e paramètre de jjtAccept ou childrenAccept.
     */
    @Override
    public Object visit(ASTGenValue node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }


    @Override
    public Object visit(ASTBoolValue node, Object data) {
        ((DataStruct) data).type = VarType.bool;
        return null;

    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        String varName = node.getValue();
        if(node.jjtGetParent() instanceof ASTGenValue){
            VarType type = symbolTable.get(varName);
            ((DataStruct)data).type = type;
        }

        if((node.jjtGetParent() instanceof ASTGenValue || node.jjtGetParent() instanceof ASTForEachStmt) && !symbolTable.containsKey(varName)){
            print(String.format("Invalid use of undefined Identifier %s",varName));
        }
        return null;
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        ((DataStruct) data).type = VarType.num;
        return null;
    }

    @Override
    public Object visit(ASTRealValue node, Object data) {
        ((DataStruct) data).type = VarType.real;
        return null;
    }

    //des outils pour vous simplifier la vie et vous enligner dans le travail
    public enum VarType {
        bool,
        num,
        real,
        listnum,
        listbool,
        listreal
    }

    private class DataStruct {
        public VarType type;

        public DataStruct() {
        }

        public DataStruct(VarType p_type) {
            type = p_type;
        }
    }
}