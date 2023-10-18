import syntaxtree.*;
import visitor.*;

public class Main {
   public static void main(String [] args) {
      try {
         Node root = new MiniJavaParser(System.in).Goal();
         TypeCheckDepthFirst gjdf = new TypeCheckDepthFirst();
         root.accept(gjdf, null); 
         System.out.println("Program type checked successfully");
      }
      catch (ParseException e) {
         System.out.println("Exception occurred");
         System.out.println(e.toString());
      }
   }
} 