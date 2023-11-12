/*
 * SpotBugs - Find bugs in Java programs
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.ClassContext;
import org.apache.bcel.generic.BasicType;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.Const;
import org.apache.bcel.generic.Type;

import java.util.Arrays;

/**
 * This detector finds all the methods of a subclass which are hiding the static methods of the superclass and
 * tries to invoke that method using the instance variable.
 * Please see @see <a href="https://wiki.sei.cmu.edu/confluence/display/java/MET07-J.+Never+declare+a+class+method+that+hides+a+method+declared+in+a+superclass+or+superinterface">SEI CERT MET07-J</a>
 * @author Nazir, Muhammad Zafar Iqbal
 */
public class FindHidingSubClass implements Detector {
    private final BugReporter bugReporter;

    public FindHidingSubClass(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    /*
     * Only the super classes are analyzed, not the super interfaces
     * Reason: when I looked in detail of the super interfaces,
     * I found there is no point to analyze as whenever we have method hiding due to super interfaces,
     * We can call the method by fully qualified name only (as Interface can't be instantiated).
     * Calling a hidden method by fully qualified name is a compliant test.
     * See @see <a href="https://wiki.sei.cmu.edu/confluence/display/java/MET07-J.+Never+declare+a+class+method+that+hides+a+method+declared+in+a+superclass+or+superinterface">SEI CERT MET07-J</a>
     * Thereof, it is useless to analyze super interfaces in the detector.
     */
    @Override
    public void visitClassContext(ClassContext classContext) {
        //First, I get the subclass and superclasses in variables.
        //This is the current class. Named it subClass to better depict the idea of sub and super class.
        JavaClass subClass = classContext.getJavaClass();
        //No need for null check as every class has at least `Object` as its super class.
        JavaClass[] superClasses = null;
        try {
            superClasses = subClass.getSuperClasses();
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
        //I store all the methods of subclass in an array
        Method[] methods = subClass.getMethods();
        //For each super class, I go through each method of the subclass
        //and filter the non-private and static methods, as private methods can't be overridden.
        for (JavaClass superClass : superClasses) {
            for (Method method : methods) {
                // Careful!!! regarding the order of the conditions applied here
                // Taking advantage of short circuit evaluation here by placing !isMainMethod(method)
                if (method.isStatic() && !method.isPrivate() && !isMainMethod(method)) {
                    //I check for the exceptional cases of inner class using three auxiliary private methods.
                    if (isConstructor(method) || isHidingInnerClass(method) || isAutoGeneratedMethod(method)) {
                        continue;
                    }
                    //I check either the subclass method is hiding the superclass method. If yes I report the bug.
                    Method[] superMethods = superClass.getMethods();
                    if (Arrays.asList(superMethods).contains(method)) {
                        bugReporter.reportBug(new BugInstance(this, "HSBC_HIDING_SUB_CLASS", NORMAL_PRIORITY)
                                .addClass(subClass.getClassName())
                                .addMethod(subClass, method)
                                .addClass(superClass.getClassName())
                                .addSourceLine(SourceLineAnnotation.fromVisitedInstruction(subClass, method, 0)));
                    }
                }
            }
        }
    }

    @Override
    public void report() {
    }

    /**
     *This method checks for the inner class exceptional cases.
     *As whenever there an inner class, '.access$' methods are created hiddenly to access the outer class attributes.
     */
    private boolean isHidingInnerClass(Method method) {
        return method.getName().contains("access$");
    }

    /**
     *This method checks for the autoGenerated methods for an inner class - exceptional cases.
     *As whenever there an inner class, '.class$' methods are created hiddenly.
     */
    private boolean isAutoGeneratedMethod(Method method) {
        return method.getName().contains("class$");
    }

    /**
     * This method is here to check the exceptional case of Constructors
     */
    private boolean isConstructor(Method method) {
        return Const.STATIC_INITIALIZER_NAME.equals(method.getName()) || Const.CONSTRUCTOR_NAME.equals(method.getName());
    }

    /**
     * This method checks for the exceptional case of main method.
     * As we know main method always have the signature "public static void main(String[] args)".
     * It is static but usually public class have its own main method as its entry point.
     * Therefore, it is not an error caused by Programming but a utility to provide UI to user.
     *
     * This condition is to abide with the latest main method criteria.  @see <a href="https://openjdk.org/jeps/445">JEP 445</a>
     */
    private boolean isMainMethod(Method method) {
        return !method.isPrivate() && method.getReturnType().equals(BasicType.VOID) && "main".equals(method.getName())
                && (isStringArray(method.getArgumentTypes()) || method.getArgumentTypes().length==0);
    }

    /**
     * This method checks either the argument is String array
     */
    private boolean isStringArray(Type[] methodArguments) {
        return methodArguments.length == 1 &&
                "java.lang.String[]".equals(methodArguments[0].toString());
    }
}
