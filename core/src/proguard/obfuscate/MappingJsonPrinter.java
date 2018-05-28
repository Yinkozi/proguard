/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2018 GuardSquare NV
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.obfuscate;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.classfile.ProgramField;
import proguard.classfile.ProgramMethod;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.CodeAttribute;
import proguard.classfile.attribute.LineNumberTableAttribute;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.util.SimplifiedVisitor;
import proguard.classfile.visitor.ClassVisitor;
import proguard.classfile.visitor.MemberVisitor;


/**
 * This ClassVisitor prints out the renamed classes and class members with
 * their old names and new names.
 *
 * @see ClassRenamer
 */
public class MappingJsonPrinter
extends      SimplifiedVisitor
implements   ClassVisitor,
             MemberVisitor,
             AttributeVisitor
{
    private final PrintStream ps;

    // keep track of visited members so we don't print
    // them multiple times (e.g. for overloads)
    private Set<String> visitedMembers;

    private boolean atLeastOneClass = false;


    /**
     * Creates a new MappingPrinter that prints to <code>System.out</code>.
     */
    public MappingJsonPrinter() {
        this(System.out);
    }


    /**
     * Creates a new MappingPrinter that prints to the given stream.
     * @param printStream the stream to which to print
     */
    public MappingJsonPrinter(PrintStream printStream) {
        this.ps = printStream;
    }


    // Implementations for ClassVisitor.

    public void visitProgramClass(ProgramClass programClass) {
        if (this.atLeastOneClass) {
            ps.println(",");
        }

        String name    = programClass.getName();
        String newName = ClassObfuscator.newClassName(programClass);

        // Print out the class mapping.
        ps.println("  \"" + ClassUtil.externalClassName(name) + "\": {");
        ps.println("    \"name\": \"" + ClassUtil.externalClassName(newName) + "\",");
        ps.println("    \"members\": {");

        visitedMembers = new HashSet<String>();

        // Print out the class members.
        programClass.fieldsAccept(this);
        programClass.methodsAccept(this);

        if (visitedMembers.size() > 0) {
            ps.println();
        }
        ps.println("    }"); // end of members
        ps.print("  }"); // end of class entry

        this.atLeastOneClass = true;
    }


    // Implementations for MemberVisitor.

    public void visitProgramField(ProgramClass programClass, ProgramField programField) {
        String fieldName           = programField.getName(programClass);
        String obfuscatedFieldName = MemberObfuscator.newMemberName(programField);
        printMemberMapping(fieldName, obfuscatedFieldName);
    }


    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod) {
        String methodName           = programMethod.getName(programClass);
        String obfuscatedMethodName = MemberObfuscator.newMemberName(programMethod);
        printMemberMapping(methodName, obfuscatedMethodName);
    }

    private void printMemberMapping(String name, String newName) {
        if (newName == null) {
            newName = name;
        }

        if (visitedMembers.contains(name)) {
            return;
        }

        if (visitedMembers.size() > 0) {
            ps.println(",");
        }

        ps.print("      \"" + name + "\": \"" + newName + "\"");
        visitedMembers.add(name);
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) { }


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute) { }


    public void visitLineNumberTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LineNumberTableAttribute lineNumberTableAttribute) { }
}
