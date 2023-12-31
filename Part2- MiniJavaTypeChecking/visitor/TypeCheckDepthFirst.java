//
// Generated by JTB 1.3.2
//

package visitor;
import syntaxtree.*;
import java.util.*;

/**
 * Provides default methods which visit each node in the tree in depth-first
 * order.  Your visitors may extend this class.
 */
public class TypeCheckDepthFirst<A> implements GJVisitor<String, A> {
   boolean debug = false;
   boolean isSecondPass = false; // flag to check if we are in the second pass
   boolean isPrimaryExpression = false; // flag to check if we are in a primary expression


   class _variable {
      String type;
      String name;

      _variable() {
         type = null;
         name = null;
      }
   }


   class _method {
      String returnType;
      String name;
      String enclosingClass;
      Vector<_variable> parameters;
      HashMap<String, _variable> variables;

      _method() {
         returnType = null;
         name = null;
         enclosingClass = null;
         parameters = new Vector<_variable>();
         variables = new HashMap<String, _variable>();
      }
   }


   class _class {
      String name;
      boolean isChild;
      String parentClass;
      HashMap<String, _variable> fields;
      HashMap<String, _method> methods;

      _class() {
         name = null;
         isChild = false;
         parentClass = null;
         fields = new HashMap<String, _variable>();
         methods = new HashMap<String, _method>();
      }
   }

   // global hash map to store all classes
   HashMap<String, _class> classes = new HashMap<String, _class>();


   void checkParameterVariableClash() {
      for(String className : classes.keySet()) {
         _class currentClass = classes.get(className);
         for(String methodName : currentClass.methods.keySet()) {
            _method currentMethod = currentClass.methods.get(methodName);
            for(_variable parameter : currentMethod.parameters) {
               if(currentMethod.variables.containsKey(parameter.name)) {
                  if(debug) System.out.println("parameter clashes with method variable error");
                  error(0);
               }
            }  
         }
      }
   }


   void checkTypesExists() {
      for(String className : classes.keySet()) {
         _class currentClass = classes.get(className);
         for(String fieldName : currentClass.fields.keySet()) {
            _variable currentField = currentClass.fields.get(fieldName);
            if(!currentField.type.equals("int") && !currentField.type.equals("int[]") && !currentField.type.equals("boolean")) {
               if(!classes.containsKey(currentField.type)) {
                  if(debug) System.out.println("field type not found");
                  error(1);
               }
            }
         }
         for(String methodName : currentClass.methods.keySet()) {
            _method currentMethod = currentClass.methods.get(methodName);
            for(String variableName : currentMethod.variables.keySet()) {
               _variable currentVariable = currentMethod.variables.get(variableName);
               if(!currentVariable.type.equals("int") && !currentVariable.type.equals("int[]") && !currentVariable.type.equals("boolean")) {
                  if(!classes.containsKey(currentVariable.type)) {
                     if(debug) System.out.println("variable type not found");
                     error(1);
                  }
               }
            }

            for(_variable parameter : currentMethod.parameters) {
               if(!parameter.type.equals("int") && !parameter.type.equals("int[]") && !parameter.type.equals("boolean")) {
                  if(parameter.type.equals("String[]") && currentMethod.name.equals("main")) continue;
                  if(!classes.containsKey(parameter.type)) {
                     if(debug) System.out.println("parameter type not found");
                     error(1);
                  }
               }
            }

            String returnType = currentMethod.returnType;
            if(!returnType.equals("int") && !returnType.equals("int[]") && !returnType.equals("boolean")) {
               if(returnType.equals("void") && currentMethod.name.equals("main")) continue;
               if(!classes.containsKey(returnType)) {
                  if(debug) System.out.println("return type not found");
                  error(1);
               }
            }
         }
      }
   }


   void checkIfParentsExist() {
      for(String className : classes.keySet()) {
         _class currentClass = classes.get(className);
         if(currentClass.isChild) {
            if(!classes.containsKey(currentClass.parentClass)) {
               if(debug) System.out.println("parent class not found");
               error(1);
            }
         }
      }
   }

   void checkExtendsCycles() {
      for(String className : classes.keySet()) {
         _class currentClass = classes.get(className);
         HashMap<String, Boolean> visited = new HashMap<String, Boolean>();
         if(currentClass.isChild) {
            visited.put(className, true);
            String parentClass = currentClass.parentClass;
            while(parentClass != null) {
               if(visited.containsKey(parentClass)) {
                  if(debug) System.out.println(currentClass.name + ": extends cycle error");
                  error(0);
               }
               visited.put(parentClass, true);
               parentClass = classes.get(parentClass).parentClass;
            }
         }
      }
   }


   void checkOverloading() {
      for(String className : classes.keySet()) {
         _class currentClass = classes.get(className);
         if(currentClass.isChild) {
            _class ancestorClass = classes.get(currentClass.parentClass);
            while(true) {
               for(String currentMethodName : currentClass.methods.keySet()) {
                  if(ancestorClass.methods.containsKey(currentMethodName)) {
                     _method currentMethod = currentClass.methods.get(currentMethodName);
                     _method parentMethod = ancestorClass.methods.get(currentMethodName);

                     if(currentMethod.parameters.size() != parentMethod.parameters.size()) {
                        if(debug) System.out.println(className + ": " + currentMethodName + ": overriding error");
                        error(0);
                     }

                     int numParameters = currentMethod.parameters.size();
                     for(int i=0; i<numParameters; i++) {
                        if(!currentMethod.parameters.get(i).type.equals(parentMethod.parameters.get(i).type)) {
                           if(debug) System.out.println(className + ": " + currentMethodName + ": overloading error");
                           error(0);
                        }
                     }
                     
                     if(!isMatchingTypes(parentMethod.returnType, currentMethod.returnType)) {
                        if(debug) System.out.println(className + ": " + currentMethodName + ": overriding error");
                        error(0);
                     }
                  }
               }
               if(!ancestorClass.isChild) break;
               ancestorClass = classes.get(ancestorClass.parentClass);
            }
         }
      }
   }

   
   String findVariableType(String variableName, A argu) {
      _method currentMethod = (_method) argu;
      if(currentMethod.variables.containsKey(variableName)) {
         return currentMethod.variables.get(variableName).type;
      }

      for(_variable parameter : currentMethod.parameters) {
         if(parameter.name.equals(variableName)) return parameter.type;
      }

      _class currentClass = classes.get(currentMethod.enclosingClass);

      if(currentClass.fields.containsKey(variableName)) {
         return currentClass.fields.get(variableName).type;
      }

      while(currentClass.isChild) {
         currentClass = classes.get(currentClass.parentClass);
         if(currentClass.fields.containsKey(variableName)) {
            return currentClass.fields.get(variableName).type;
         }
      }
      
      if(debug) System.out.println(variableName + ":variable not found");
      error(1);

      return null;
   }


   _method findMethod(String methodName, _class currentClass) {
      if(currentClass.methods.containsKey(methodName)) {
         return currentClass.methods.get(methodName);
      }
      else {
         while(currentClass.isChild) {
            currentClass = classes.get(currentClass.parentClass);
            if(currentClass.methods.containsKey(methodName)) {
               return currentClass.methods.get(methodName);
            }
         }
      }

      if(debug) System.out.println(methodName + "method not found");
      error(1);

      return null;
   }


   void findArgumentTypes(String exprList, Vector<String> arguments) {
      String[] args = exprList.split("\\$");
      for(String s : args) {
         arguments.add(s);
      }
   }


   void checkMatchingCall(_method method, Vector<String> argumentTypes) {
      if(method.parameters.size() != argumentTypes.size()) {
         if(debug) System.out.println(method.name + "method call error");
         if(debug) System.out.println(method.parameters.size() + " " + argumentTypes.size());
         error(0);
      }

      int numParameters = method.parameters.size();
      for(int i=0; i<numParameters; i++) {
         if(!isMatchingTypes(method.parameters.get(i).type, argumentTypes.get(i))) {
            if(debug) System.out.println(method.name + ":method call error");
            if(debug) System.out.println("numParameters: " + numParameters);
            if(debug) System.out.println("Found: " + argumentTypes.get(i) + " Expected: " + method.parameters.get(i).type + " at index: " + i);
            error(0);
         }
      }
   }

   boolean isMatchingTypes (String type1, String type2) {
      if(type1.equals(type2)) return true;
      return inheritedFrom(type1, type2);
   }

   boolean inheritedFrom(String parent, String child) {
      if(!classes.containsKey(child)) return false;
      if(!classes.containsKey(parent)) return false;

      if(!classes.get(child).isChild) return false;
      if(classes.get(child).parentClass.equals(parent)) return true;
      return inheritedFrom(parent, classes.get(child).parentClass);
   }

   void printAllComponents() {
      for(String className : classes.keySet()) {
         _class currClass = classes.get(className);
         System.out.println("Class: " + currClass.name);

         for(String fieldName : currClass.fields.keySet()) {
            _variable currField = currClass.fields.get(fieldName);
            System.out.println("    Field: " + currField.name + " " + currField.type);
         }

         for(String methodName : currClass.methods.keySet()) {
            _method currMethod = currClass.methods.get(methodName);
            System.out.println("    Method: " + currMethod.name + " " + currMethod.returnType);

            for(_variable parameter : currMethod.parameters) {
               System.out.println("        Parameter: " + parameter.name + " " + parameter.type);
            }

            for(String variableName : currMethod.variables.keySet()) {
               _variable currVariable = currMethod.variables.get(variableName);
               System.out.println("        Variable: " + currVariable.name + " " + currVariable.type);
            }
         }
      }
   }


   void error(int errorCode) {
      if(errorCode == 0) System.out.println("Type error");
      else System.out.println("Symbol not found");
      System.exit(1);
   }

   //
   // Auto class visitors--probably don't need to be overridden.
   //
   public String visit(NodeList n, A argu) {
      String _ret=null;
      int _count=0;
      for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
         e.nextElement().accept(this,argu);
         _count++;
      }
      return _ret;
   }

   public String visit(NodeListOptional n, A argu) {
      if ( n.present() ) {
         String _ret=null;
         _ret = "";
         int _count=0;
         for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
            _ret += "$";
            _ret += e.nextElement().accept(this,argu);
            _count++;
         }

         return _ret;
      }
      else
         return null;
   }

   public String visit(NodeOptional n, A argu) {
      if ( n.present() )
         return n.node.accept(this,argu);
      else
         return null;
   }

   public String visit(NodeSequence n, A argu) {
      String _ret=null;
      int _count=0;
      for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
         e.nextElement().accept(this,argu);
         _count++;
      }
      return _ret;
   }

   public String visit(NodeToken n, A argu) { return n.toString(); }

   //
   // User-generated visitor methods below
   //

   /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
   public String visit(Goal n, A argu) {
      String _ret=null;

      if(debug) System.out.println("Starting first pass");

      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      
      if(debug) System.out.println("First pass completed successfully");

      if(debug) {
         System.out.println("Summary: Printing all components");
         printAllComponents();
      }

      checkParameterVariableClash();
      checkTypesExists();
      checkIfParentsExist();
      checkExtendsCycles();
      checkOverloading();

      if(debug) System.out.println("Intermediate checks completed successfully");

      isSecondPass = true;

      n.f0.accept(this, argu);
      n.f1.accept(this, argu);
      n.f2.accept(this, null);

      if(debug) System.out.println("Second pass completed successfully");

      return _ret;
   }

   /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> PrintStatement()
    * f15 -> "}"
    * f16 -> "}"
    */
   public String visit(MainClass n, A argu) {
      String _ret = null;

      n.f0.accept(this, null);
      String className = n.f1.accept(this, argu);

      if(!isSecondPass) {
         _class mainClass = new _class();
         
         mainClass.name = className;

         n.f2.accept(this, null);
         n.f3.accept(this, null);
         n.f4.accept(this, null);
         n.f5.accept(this, null);
         n.f6.accept(this, null);
         n.f7.accept(this, null);
         n.f8.accept(this, null);

         _method mainMethod = new _method();
         mainMethod.returnType = "void";
         mainMethod.name = "main";
         mainMethod.enclosingClass = mainClass.name;

         _variable mainParameter = new _variable();
         mainParameter.type = "String[]";
         
         n.f9.accept(this, null);
         n.f10.accept(this, null);

         mainParameter.name = n.f11.accept(this, argu);

         mainMethod.parameters.add(mainParameter);
         mainClass.methods.put(mainMethod.name, mainMethod);
         classes.put(className, mainClass);

         n.f12.accept(this, null);
         n.f13.accept(this, null);
         // n.f14.accept(this, null);
         n.f15.accept(this, null);
         n.f16.accept(this, null);
      }
      else {
         n.f14.accept(this, (A) classes.get(className).methods.get("main"));
      }

      return _ret;
   }

   /**
    * f0 -> ClassDeclaration()
    *       | ClassExtendsDeclaration()
    */
   public String visit(TypeDeclaration n, A argu) {
      String _ret=null;
      n.f0.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
   public String visit(ClassDeclaration n, A argu) {
      String _ret=null;

      n.f0.accept(this, null);
      String className = n.f1.accept(this, argu);

      if(!isSecondPass) {
         _class newClass = new _class();
         newClass.name = className;

         if(classes.containsKey(newClass.name)) {
            if(debug) System.out.println("class redeclaration error");
            error(0);
         }
         
         n.f2.accept(this, null);
         n.f3.accept(this, (A) newClass.fields);
         n.f4.accept(this, (A) newClass);
         n.f5.accept(this, null);

         classes.put(newClass.name, newClass);
      }
      else {
         n.f4.accept(this, (A) classes.get(className));
      }

      return _ret;
   }

   /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
   public String visit(ClassExtendsDeclaration n, A argu) {
      String _ret=null;

      n.f0.accept(this, null);
      String className = n.f1.accept(this, argu);

      if(!isSecondPass) {
         _class newClass = new _class();
         newClass.name = className;

         if(classes.containsKey(newClass.name)) {
            if(debug) System.out.println("class redeclaration error");
            error(0);
         }

         n.f2.accept(this, null);

         newClass.isChild = true;
         newClass.parentClass = n.f3.accept(this, argu);

         n.f4.accept(this, null);
         n.f5.accept(this, (A) newClass.fields);
         n.f6.accept(this, (A) newClass);
         n.f7.accept(this, null);

         classes.put(newClass.name, newClass);
      }
      else {
         n.f6.accept(this, (A) classes.get(className));
      }

      return _ret;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
   public String visit(VarDeclaration n, A argu) {
      String _ret=null;

      if(!isSecondPass) {
         HashMap<String, _variable> variableList = (HashMap<String, _variable>) argu;
         _variable newVariable = new _variable();

         newVariable.type = n.f0.accept(this, argu);
         newVariable.name = n.f1.accept(this, argu);
         
         if(variableList.containsKey(newVariable.name)) {
            if(debug) System.out.println("variable redeclaration error");
            error(0);
         }
         variableList.put(newVariable.name, newVariable);

         n.f2.accept(this, null);
      }

      return _ret;
   }

   /**
    * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
   public String visit(MethodDeclaration n, A argu) {
      String _ret=null;
      
      _class currentClass = (_class) argu;

      n.f0.accept(this, null);
      String returnType = n.f1.accept(this, argu);
      String methodName = n.f2.accept(this, argu);

      if(!isSecondPass) {
         _method newMethod = new _method();

         newMethod.returnType = returnType;
         newMethod.name = methodName;
         newMethod.enclosingClass = currentClass.name;

         if(currentClass.methods.containsKey(newMethod.name)) {
            if(debug) System.out.println("method redeclaration error");
            error(0);
         }

         n.f3.accept(this, null);
         n.f4.accept(this, (A) newMethod);
         n.f5.accept(this, null);
         n.f6.accept(this, null);
         n.f7.accept(this, (A) newMethod.variables);
         n.f8.accept(this, (A) newMethod);
         n.f9.accept(this, null);
         n.f10.accept(this, (A) newMethod);
         n.f11.accept(this, null);
         n.f12.accept(this, null);

         currentClass.methods.put(newMethod.name, newMethod);
      }
      else {
         _method currentMethod = (_method) currentClass.methods.get(methodName);
         n.f8.accept(this, (A) currentMethod);
         String generatedReturnType = n.f10.accept(this, (A) currentMethod);
         if(!isMatchingTypes(generatedReturnType, currentMethod.returnType)) {
            if(debug) System.out.println("return type mismatch");
            error(0);
         }
      }

      return _ret;
   }

   /**
    * f0 -> FormalParameter()
    * f1 -> ( FormalParameterRest() )*
    */
   public String visit(FormalParameterList n, A argu) {
      String _ret=null;
      
      if(!isSecondPass) {
         _method currentMethod = (_method) argu;
         _variable newParameter = new _variable();

         n.f0.accept(this, (A) newParameter);

         for(_variable v : currentMethod.parameters) {
            if(v.name.equals(newParameter.name)) {
               if(debug) System.out.println("parameter redeclaration error");  
               error(0);
            }
         }
         currentMethod.parameters.add(newParameter);

         n.f1.accept(this, argu);
      }

      return _ret;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
   public String visit(FormalParameter n, A argu) {
      String _ret=null;
      
      if(!isSecondPass) {
         _variable currentParameter = (_variable) argu;

         currentParameter.type = n.f0.accept(this, argu);
         currentParameter.name = n.f1.accept(this, argu);
      }

      return _ret;
   }

   /**
    * f0 -> ","
    * f1 -> FormalParameter()
    */
   public String visit(FormalParameterRest n, A argu) {
      String _ret=null;
      
      if(!isSecondPass) {
         _method currentMethod = (_method) argu;
         _variable newParameter = new _variable();

         n.f0.accept(this, null);

         n.f1.accept(this, (A) newParameter);

         for(_variable v : currentMethod.parameters) {
            if(v.name.equals(newParameter.name)) {
               if(debug) System.out.println("parameter redeclaration error");
               error(0);
            }
         }
         currentMethod.parameters.add(newParameter);
      }    

      return _ret;
   }

   /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
   public String visit(Type n, A argu) {
      String _ret=null;
      if(!isSecondPass) {
         _ret = n.f0.accept(this, argu);
      }
      return _ret;
   }

   /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
   public String visit(ArrayType n, A argu) {
      String _ret=null;
      n.f0.accept(this, null);
      n.f1.accept(this, null);
      n.f2.accept(this, null);

      _ret = "int[]";
      return _ret;
   }

   /**
    * f0 -> "boolean"
    */
   public String visit(BooleanType n, A argu) {
      String _ret=null;
      n.f0.accept(this, null);

      _ret = "boolean";
      return _ret;
   }

   /**
    * f0 -> "int"
    */
   public String visit(IntegerType n, A argu) {
      String _ret=null;
      n.f0.accept(this, null);

      _ret = "int";
      return _ret;
   }

   /**
    * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | IfStatement()
    *       | WhileStatement()
    *       | DoStatement()
    *       | PrintStatement()
    */
   public String visit(Statement n, A argu) {
      String _ret=null;

      if(!isSecondPass) return _ret;

      n.f0.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
   public String visit(Block n, A argu) {
      String _ret=null;
      n.f0.accept(this, null);
      n.f1.accept(this, argu);
      n.f2.accept(this, null);
      return _ret;
   }

   /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
   public String visit(AssignmentStatement n, A argu) {
      String _ret=null;

      String variable = n.f0.accept(this, argu);
      String lType = findVariableType(variable, argu);

      n.f1.accept(this, argu);

      String rType = n.f2.accept(this, argu);

      if(!isMatchingTypes(lType, rType)) {
         if(debug) System.out.println("lType:" + lType + " rType:" + rType + " assignment error");
         error(0);
      }

      n.f3.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
   public String visit(ArrayAssignmentStatement n, A argu) {
      String _ret=null;
      String variable = n.f0.accept(this, argu);
      String lType = findVariableType(variable, argu);

      if(!lType.equals("int[]")) {
         if(debug) System.out.println("array assignment error");
         error(0);
      }

      n.f1.accept(this, argu);

      String expressionType = n.f2.accept(this, argu);
      if(!expressionType.equals("int")) {
         if(debug) System.out.println("array assignment error");
         error(0);
      }

      n.f3.accept(this, argu);
      n.f4.accept(this, argu);

      String rType = n.f5.accept(this, argu);
      if(!rType.equals("int")) {
         if(debug) System.out.println("array assignment error");
         error(0);
      }

      n.f6.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> IfthenElseStatement()
    *       | IfthenStatement()
    */
   public String visit(IfStatement n, A argu) {
      String _ret=null;
      n.f0.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
   public String visit(IfthenStatement n, A argu) {
      String _ret=null;
      n.f0.accept(this, null);
      n.f1.accept(this, null);

      String expressionType = n.f2.accept(this, argu);
      if(!expressionType.equals("boolean")) {
         if(debug) System.out.println("if statement expects a boolean");
         error(0);
      }

      n.f3.accept(this, null);
      n.f4.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
   public String visit(IfthenElseStatement n, A argu) {
      String _ret=null;
      n.f0.accept(this, null);
      n.f1.accept(this, null);

      String expressionType = n.f2.accept(this, argu);
      if(!expressionType.equals("boolean")) {
         if(debug) System.out.println("if else statement expects a boolean");
         error(0);
      }

      n.f3.accept(this, null);
      n.f4.accept(this, argu);
      n.f5.accept(this, null);
      n.f6.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
   public String visit(WhileStatement n, A argu) {
      String _ret=null;
      n.f0.accept(this, null);
      n.f1.accept(this, null);

      String expressionType = n.f2.accept(this, argu);
      if(!expressionType.equals("boolean")) {
         if(debug) System.out.println("while statement expects a boolean condition");
         error(0);
      }

      n.f3.accept(this, null);
      n.f4.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> "do"
    * f1 -> Statement()
    * f2 -> "while"
    * f3 -> "("
    * f4 -> Expression()
    * f5 -> ")"
    * f6 -> ";"
    */
   public String visit(DoStatement n, A argu) {
      String _ret=null;
      n.f0.accept(this, null);
      n.f1.accept(this, argu);
      n.f2.accept(this, null);
      n.f3.accept(this, null);

      String expressionType = n.f4.accept(this, argu);
      if(!expressionType.equals("boolean")) {
         if(debug) System.out.println("do while statement expects a boolean condition");
         error(0);
      }

      n.f5.accept(this, null);
      n.f6.accept(this, null);
      return _ret;
   }

   /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
   public String visit(PrintStatement n, A argu) {
      String _ret=null;
      n.f0.accept(this, null);
      n.f1.accept(this, null);

      String expressionType = n.f2.accept(this, argu);
      if(!expressionType.equals("int")) {
         if(debug) System.out.println("print statement expects an int");
         error(0);
      }

      n.f3.accept(this, null);
      n.f4.accept(this, null);
      return _ret;
   }

   /**
    * f0 -> OrExpression()
    *       | AndExpression()
    *       | CompareExpression()
    *       | neqExpression()
    *       | AddExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | DivExpression()
    *       | ArrayLookup()
    *       | ArrayLength()
    *       | MessageSend()
    *       | PrimaryExpression()
    */
   public String visit(Expression n, A argu) {
      String _ret=null;

      if(!isSecondPass) return _ret;
      
      _ret = n.f0.accept(this, argu);

      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "&&"
    * f2 -> PrimaryExpression()
    */
   public String visit(AndExpression n, A argu) {
      String _ret=null;
      String lType = n.f0.accept(this, argu);
      n.f1.accept(this, null);
      String rType = n.f2.accept(this, argu);

      if(!lType.equals("boolean") || !rType.equals("boolean")) {
         if(debug) System.out.println("and expression expects two booleans");
         error(0);
      }

      _ret = "boolean";
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "||"
    * f2 -> PrimaryExpression()
    */
   public String visit(OrExpression n, A argu) {
      String _ret=null;
      String lType = n.f0.accept(this, argu);
      n.f1.accept(this, null);
      String rType = n.f2.accept(this, argu);

      if(!lType.equals("boolean") || !rType.equals("boolean")) {
         if(debug) System.out.println("or expression expects two booleans");
         error(0);
      }

      _ret = "boolean";
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "<="
    * f2 -> PrimaryExpression()
    */
   public String visit(CompareExpression n, A argu) {
      String _ret=null;
      String lType = n.f0.accept(this, argu);
      n.f1.accept(this, null);
      String rType = n.f2.accept(this, argu);

      if(!lType.equals("int") || !rType.equals("int")) {
         if(debug) System.out.println("compare expression expects two ints");
         error(0);
      }

      _ret = "boolean";
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "!="
    * f2 -> PrimaryExpression()
    */
   public String visit(neqExpression n, A argu) {
      String _ret=null;
      String lType = n.f0.accept(this, argu);
      n.f1.accept(this, null);
      String rType = n.f2.accept(this, argu);

      if(!(isMatchingTypes(lType, rType) || isMatchingTypes(rType, lType))) {
         if(debug) System.out.println("not equals expression expects two comparable types");
         error(0);
      }

      _ret = "boolean";
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
   public String visit(AddExpression n, A argu) {
      String _ret=null;
      String lType = n.f0.accept(this, argu);
      n.f1.accept(this, null);
      String rType = n.f2.accept(this, argu);

      if(!lType.equals("int") || !rType.equals("int")) {
         if(debug) System.out.println("add expression expects two ints");
         error(0);
      }

      _ret = "int";
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
   public String visit(MinusExpression n, A argu) {
      String _ret=null;
      String lType = n.f0.accept(this, argu);
      n.f1.accept(this, null);
      String rType = n.f2.accept(this, argu);

      if(!lType.equals("int") || !rType.equals("int")) {
         if(debug) System.out.println("subtract expression expects two ints");
         error(0);
      }

      _ret = "int";
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
   public String visit(TimesExpression n, A argu) {
      String _ret=null;
      String lType = n.f0.accept(this, argu);
      n.f1.accept(this, null);
      String rType = n.f2.accept(this, argu);

      if(!lType.equals("int") || !rType.equals("int")) {
         if(debug) System.out.println("multiply expression expects two ints");
         error(0);
      }

      _ret = "int";
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "/"
    * f2 -> PrimaryExpression()
    */
   public String visit(DivExpression n, A argu) {
      String _ret=null;
      String lType = n.f0.accept(this, argu);
      n.f1.accept(this, null);
      String rType = n.f2.accept(this, argu);

      if(!lType.equals("int") || !rType.equals("int")) {
         if(debug) System.out.println("divide expression expects two ints");
         error(0);
      }

      _ret = "int";
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
   public String visit(ArrayLookup n, A argu) {
      String _ret=null;
      String lType = n.f0.accept(this, argu);
      n.f1.accept(this, null);
      String rType = n.f2.accept(this, argu);

      if(!lType.equals("int[]") || !rType.equals("int")) {
         if(debug) System.out.println("subtract expression expects int[] and int");
         error(0);
      }

      n.f3.accept(this, null);

      _ret = "int";
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
   public String visit(ArrayLength n, A argu) {
      String _ret=null;
      String lType = n.f0.accept(this, argu);

      if(!lType.equals("int[]")) {
         if(debug) System.out.println(".length can only be called on arrays");
         error(0);
      }

      n.f1.accept(this, null);
      n.f2.accept(this, null);

      _ret = "int";
      return _ret;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
   public String visit(MessageSend n, A argu) {
      String _ret=null;

      String className = n.f0.accept(this, argu);

      n.f1.accept(this, null);

      String methodName = n.f2.accept(this, argu);
      _method method = findMethod(methodName, classes.get(className));

      n.f3.accept(this, null);
      String expressionList = n.f4.accept(this, argu);
      Vector<String> argumentTypes = new Vector<String>();
      if(expressionList != null) {
         findArgumentTypes(expressionList, argumentTypes);
      }

      checkMatchingCall(method, argumentTypes);

      _ret = method.returnType;

      n.f5.accept(this, null);
      return _ret;
   }

   /**
    * f0 -> Expression()
    * f1 -> ( ExpressionRest() )*
    */
   public String visit(ExpressionList n, A argu) {
      String _ret=null;
      _ret = n.f0.accept(this, argu);
      String ret = n.f1.accept(this, argu);
      if(ret != null) _ret += ret;
      return _ret;
   }

   /**
    * f0 -> ","
    * f1 -> Expression()
    */
   public String visit(ExpressionRest n, A argu) {
      String _ret=null;
      n.f0.accept(this, null);
      _ret = n.f1.accept(this, argu);

      return _ret;
   }

   /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | NotExpression()
    *       | BracketExpression()
    */
   public String visit(PrimaryExpression n, A argu) {
      String _ret=null;

      isPrimaryExpression = true;
      _ret = n.f0.accept(this, argu);
      isPrimaryExpression = false;

      return _ret;
   }

   /**
    * f0 -> <INTEGER_LITERAL>
    */
   public String visit(IntegerLiteral n, A argu) {
      String _ret=null;
      n.f0.accept(this, argu);

      _ret = "int";
      return _ret;
   }

   /**
    * f0 -> "true"
    */
   public String visit(TrueLiteral n, A argu) {
      String _ret=null;
      n.f0.accept(this, argu);

      _ret = "boolean";
      return _ret;
   }

   /**
    * f0 -> "false"
    */
   public String visit(FalseLiteral n, A argu) {
      String _ret=null;
      n.f0.accept(this, argu);

      _ret = "boolean";
      return _ret;
   }

   /**
    * f0 -> <IDENTIFIER>
    */
   public String visit(Identifier n, A argu) {
      String _ret=null;
      _ret = n.f0.accept(this, argu);

      if(isPrimaryExpression) {
         _ret = findVariableType(_ret, argu);
      }

      return _ret;
   }

   /**
    * f0 -> "this"
    */
   public String visit(ThisExpression n, A argu) {
      String _ret=null;
      n.f0.accept(this, argu);

      _ret = ((_method) argu).enclosingClass;

      return _ret;
   }

   /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public String visit(ArrayAllocationExpression n, A argu) {
      String _ret=null;
      n.f0.accept(this, null);
      n.f1.accept(this, null);
      n.f2.accept(this, null);
      _ret = n.f3.accept(this, argu);

      if(!_ret.equals("int")) {
         if(debug) System.out.println("size of array must be integer");
         error(0);
      }

      n.f4.accept(this, null);

      _ret = "int[]";
      return _ret;
   }

   /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
   public String visit(AllocationExpression n, A argu) {
      String _ret=null;
      n.f0.accept(this, null);

      isPrimaryExpression = false;
      _ret = n.f1.accept(this, argu);

      if(!classes.containsKey(_ret)) {
         if(debug) System.out.println(_ret + ": class not found");
         error(1);
      }

      n.f2.accept(this, null);
      n.f3.accept(this, null);
      return _ret;
   }

   /**
    * f0 -> "!"
    * f1 -> Expression()
    */
   public String visit(NotExpression n, A argu) {
      String _ret=null;
      n.f0.accept(this, null);
      _ret = n.f1.accept(this, argu);

      if(!_ret.equals("boolean")) {
         if(debug) System.out.println("! expression expects boolean");
         error(0);
      }

      return _ret;
   }

   /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
   public String visit(BracketExpression n, A argu) {
      String _ret=null;
      n.f0.accept(this, null);
      _ret = n.f1.accept(this, argu);
      n.f2.accept(this, null);
      return _ret;
   }

   /**
    * f0 -> Identifier()
    * f1 -> ( IdentifierRest() )*
    */
   public String visit(IdentifierList n, A argu) {
      String _ret=null;
      _ret = n.f0.accept(this, argu);
      _ret += n.f1.accept(this, argu);
      return _ret;
   }

   /**
    * f0 -> ","
    * f1 -> Identifier()
    */
   public String visit(IdentifierRest n, A argu) {
      String _ret=null;
      n.f0.accept(this, argu);
      _ret = n.f1.accept(this, argu);
      return _ret;
   }

}
