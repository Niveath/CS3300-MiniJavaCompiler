class Factorial{
    public static void main(String[] a){
        System.out.println(1);
    }
}

class Add{
    public int Add(int num1, int num2){
        return num1 + num2 ;
    }
}

class Sub extends Add{
    public boolean Add(int num1, int num2){
        return num1 <= num2 ;
    }
}