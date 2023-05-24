package findhidingsubclasstest;

class GoodGrandClassPrivate {
    private static void display(String s) {
        System.out.println("Grand Information: " + s);
    }
}

class GoodParentClassPrivate extends GoodGrandClassPrivate {
}

/**
 * This test case is a complaint test case with multiple level inheritance.
 * As the overridden methods are static but declared private.
 * So actually none of the child classes inherit any method from its super class(es).
 */
class GoodChildClassPrivate extends GoodParentClassPrivate {
    private static void display(String s) {
        System.out.println("Child information" + s);
    }
}

public class GoodMultiLevelInheritancePrivate {
}
