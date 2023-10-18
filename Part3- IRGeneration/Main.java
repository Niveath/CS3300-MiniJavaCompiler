import syntaxtree.*;
import visitor.*;

public class Main {
   public static void main(String [] args) {
      try {
         Node root = new MiniJavaParser(System.in).Goal();
         IRGenerator gjdf = new IRGenerator();
         root.accept(gjdf, null);
      }
      catch (ParseException e) {
         System.out.println("Exception occurred");
         System.out.println(e.toString());
      }
   }
} 