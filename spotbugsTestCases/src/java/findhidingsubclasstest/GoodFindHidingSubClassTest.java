package findhidingsubclasstest;

class GrantAccess {
    public void displayAccountStatus() {
        System.out.print("displayAccountStatus (GrantAccess)");
    }
}

/**
 * This test case is the compliant test case for the bug.
 * As the displayAccountStatus() is declared as non-static and non-private (public)
 */
class GrantUserAccess extends GrantAccess {
    @Override
    public void displayAccountStatus() {
        System.out.print("displayAccountStatus (GrantUserAccess)");
    }
}

public class GoodFindHidingSubClassTest {
}
