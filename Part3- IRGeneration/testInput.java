class Factorial{
    public static void main(String[] a){
        System.out.println((new B().foo(6)).fa());
    }
}

class C
{
    A x;
    public A foo(int i)
    {
        x = new B();
        return x;
    }
}
class A extends C{
    int x;
    public int ComputeFac(int n){
        x = n+20;
        return x;
    }

    public int fa()
    {
        x = 46;
        return x;
    }
}

class B extends A
{
    int x;
    public int suu(int i)
    {
        x = i+200;
        return x;
    }
}