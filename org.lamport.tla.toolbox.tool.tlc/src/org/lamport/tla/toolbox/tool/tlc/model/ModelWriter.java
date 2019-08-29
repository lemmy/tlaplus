/*******************************************************************************
 * Copyright (c) 2015 Microsoft Research. All rights reserved. 
 *
 * The MIT License (MIT)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Contributors:
 *   Simon Zambrovski - initial API and implementation
 ******************************************************************************/
package org.lamport.tla.toolbox.tool.tlc.model;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.lamport.tla.toolbox.tool.ToolboxHandle;
import org.lamport.tla.toolbox.tool.tlc.util.ModelHelper;
import org.lamport.tla.toolbox.util.ResourceHelper;

import tla2sany.modanalyzer.SpecObj;
import tla2sany.semantic.OpDefNode;

/**
 * Encapsulates two buffers and provides semantic methods to add content to the _MC file and the CFG file of the model 
 */
public class ModelWriter
{
    /**
     * Counter to be able to generate unique identifiers
     */
    private static final AtomicLong COUNTER = new AtomicLong(1L);

    public static final String SPEC_SCHEME = "spec";
    public static final String INIT_SCHEME = "init";
    public static final String NEXT_SCHEME = "next";
    public static final String CONSTANT_SCHEME = "const";
    public static final String SYMMETRY_SCHEME = "symm";
    public static final String DEFOV_SCHEME = "def_ov";
    public static final String CONSTRAINT_SCHEME = "constr";
    public static final String ACTIONCONSTRAINT_SCHEME = "action_constr";
    public static final String INVARIANT_SCHEME = "inv";
    public static final String PROP_SCHEME = "prop";
    public static final String VIEW_SCHEME = "view";
    public static final String CONSTANTEXPR_SCHEME = "const_expr";
    public static final String TRACE_EXPR_VAR_SCHEME = "__trace_var";
    public static final String TRACE_EXPR_DEF_SCHEME = "trace_def";

    public static final String SPACE = " ";
    public static final String CR = "\n";
    public static final String SEP = "----";
    public static final String EQ = " = ";
    public static final String ARROW = " <- ";
    public static final String RECORD_ARROW = " |-> ";
    public static final String DEFINES = " == ";
    public static final String DEFINES_CR = " ==\n";
    public static final String COMMENT = "\\* ";
    public static final String ATTRIBUTE = "@";
    public static final String INDEX = ":";
    public static final String EMPTY_STRING = "";
    public static final String CONSTANT_EXPRESSION_EVAL_IDENTIFIER = "\"$!@$!@$!@$!@$!\"";
    public static final String COMMA = ",";
    public static final String BEGIN_TUPLE = "<<";
    public static final String END_TUPLE = ">>";
    public static final String PRIME = "'";
    public static final String QUOTE = "\"";
    public static final String VARIABLES = "VARIABLES ";
    public static final String TLA_AND = "/\\";
    public static final String TLA_OR = "\\/";
    public static final String TLA_NOT = "~";
    public static final String TLA_EVENTUALLY_ALWAYS = "<>[]";
    public static final String TLA_INF_OFTEN = "[]<>";
    public static final String TRACE_NA = "\"--\"";
    public static final String L_PAREN = "(";
    public static final String R_PAREN = ")";
    public static final String L_SQUARE_BRACKET = "[";
    public static final String R_SQUARE_BRACKET = "]";
    

    protected final StringBuffer tlaBuffer;
    protected final StringBuffer cfgBuffer;

    /**
     * Constructs new model writer
     */
    public ModelWriter()
    {
        this.tlaBuffer = new StringBuffer(1024);
        this.cfgBuffer = new StringBuffer(1024);
    }

    /**
     * Write the content to files
     * @param tlaFile
     * @param cfgFile
     * @param monitor
     * @throws CoreException
     */
    public void writeFiles(IFile tlaFile, IFile cfgFile, IProgressMonitor monitor) throws CoreException
    {
        tlaBuffer.append(ResourceHelper.getModuleClosingTag());
        cfgBuffer.append(ResourceHelper.getConfigClosingTag());
        ResourceHelper.replaceContent(tlaFile, tlaBuffer, monitor);
        ResourceHelper.replaceContent(cfgFile, cfgBuffer, monitor);
    }

    /**
     * Add file header, which consists of the module-beginning ----- MODULE ... ---- line and
     * the EXTENDS statement.
     * @param moduleFilename
     * @param extendedModuleName
     */
    public void addPrimer(String moduleFilename, String extendedModuleName)
    {
        tlaBuffer.append(ResourceHelper.getExtendingModuleContent(moduleFilename, new String[] {extendedModuleName, "TLC"}));
    }

    /**
     * Add spec definition
     * @param specDefinition
     */
    public void addSpecDefinition(String[] specDefinition, String attributeName)
    {
        cfgBuffer.append("SPECIFICATION").append(SPACE);
        cfgBuffer.append(specDefinition[0]).append(CR);

        tlaBuffer.append(COMMENT).append("Specification ").append(ATTRIBUTE).append(attributeName).append(CR);
        tlaBuffer.append(specDefinition[1]).append(CR).append(SEP).append(CR);

    }

    /**
     * Documentation by SZ: Add constants declarations. 
     * 
     * On 17 March 2012, LL split the original addConstants method
     * into the current one plus the addConstantsBis method.  As explained in Bugzilla Bug 280,
     * this was to allow the user definitions added on the Advanced Model page to appear between
     * the CONSTANT declarations for model values and the definitions of the expressions that 
     * instantiate CONSTANT parameters.  (This allows symbols defined in those user definitions to
     * appear in the expressions instantiated for CONSTANT parameters.)
     * 
     * See the use of these two methods in TLCModelLaunchDelegate.buildForLaunch for a description
     * of what these methods do.
     * 
     * @param constants
     * @param modelValues
     */
    public void addConstants(List<Assignment> constants, TypedSet modelValues, String attributeConstants, String attributeMVs)
    {
        // add declarations for model values introduced on Advanced Model page.
        addMVTypedSet(modelValues, "MV CONSTANT declarations ", attributeMVs);

        Assignment constant;
        Vector<String> symmetrySets = new Vector<String>();

        // first run for all the declarations
        for (int i = 0; i < constants.size(); i++)
        {
            constant = (Assignment) constants.get(i);
            if (constant.isModelValue())
            {
                if (constant.isSetOfModelValues())
                {
                    // set model values
                    TypedSet setOfMVs = TypedSet.parseSet(constant.getRight());
                    addMVTypedSet(setOfMVs, "MV CONSTANT declarations", attributeConstants);
                }
            }
        }

        // now all the definitions
        for (int i = 0; i < constants.size(); i++)
        {
            constant = (Assignment) constants.get(i);
            if (constant.isModelValue())
            {
                if (constant.isSetOfModelValues())
                {
                    // set model values
                    cfgBuffer.append(COMMENT).append("MV CONSTANT definitions").append(CR);
                    tlaBuffer.append(COMMENT).append("MV CONSTANT definitions " + constant.getLeft()).append(CR);

                    String id = addArrowAssignment(constant, CONSTANT_SCHEME);
                    if (constant.isSymmetricalSet())
                    {
                        symmetrySets.add(id);
                    }
                    tlaBuffer.append(SEP).append(CR).append(CR);
                } else
                {
                    cfgBuffer.append(COMMENT).append("CONSTANT declarations").append(CR);
                    // model value assignment
                    // to .cfg : foo = foo
                    // to _MC.tla : <nothing>, since the constant is already defined in one of the spec modules
                    cfgBuffer.append("CONSTANT").append(SPACE).append(constant.getLabel()).append(EQ).append(
                            constant.getRight()).append(CR);
                }
            } else
            {
//                // simple constant value assignment
//                cfgBuffer.append(COMMENT).append("CONSTANT definitions").append(CR);
//
//                tlaBuffer.append(COMMENT).append("CONSTANT definitions ").append(ATTRIBUTE).append(attributeConstants)
//                        .append(INDEX).append(i).append(constant.getLeft()).append(CR);
//                addArrowAssignment(constant, CONSTANT_SCHEME);
//                tlaBuffer.append(SEP).append(CR).append(CR);
            }
        }

        // symmetry
        if (!symmetrySets.isEmpty())
        {
            String label = ModelWriter.getValidIdentifier(SYMMETRY_SCHEME);

            tlaBuffer.append(COMMENT).append("SYMMETRY definition").append(CR);
            cfgBuffer.append(COMMENT).append("SYMMETRY definition").append(CR);

            tlaBuffer.append(label).append(DEFINES).append(CR);
            // symmetric model value sets added
            for (int i = 0; i < symmetrySets.size(); i++)
            {
                tlaBuffer.append("Permutations(").append(symmetrySets.get(i)).append(")");
                if (i != symmetrySets.size() - 1)
                {
                    tlaBuffer.append(" \\union ");
                }
            }

            tlaBuffer.append(CR).append(SEP).append(CR).append(CR);
            cfgBuffer.append("SYMMETRY").append(SPACE).append(label).append(CR);
        }

    }

    public void addConstantsBis(List<Assignment> constants, /* TypedSet modelValues, */ String attributeConstants /* , String attributeMVs */)
    {
//        // add declarations for model values introduced on Advanced Model page.
//        addMVTypedSet(modelValues, "MV CONSTANT declarations ", attributeMVs);
//
        Assignment constant;
//        Vector symmetrySets = new Vector();
//
//        // first run for all the declarations
//        for (int i = 0; i < constants.size(); i++)
//        {
//            constant = (Assignment) constants.get(i);
//            if (constant.isModelValue())
//            {
//                if (constant.isSetOfModelValues())
//                {
//                    // set model values
//                    TypedSet setOfMVs = TypedSet.parseSet(constant.getRight());
//                    addMVTypedSet(setOfMVs, "MV CONSTANT declarations", attributeConstants);
//                }
//            }
//        }

        // now all the definitions
        for (int i = 0; i < constants.size(); i++)
        {
            constant = (Assignment) constants.get(i);
            if (constant.isModelValue())
            {
//                if (constant.isSetOfModelValues())
//                {
//                    // set model values
//                    cfgBuffer.append(COMMENT).append("MV CONSTANT definitions").append(CR);
//                    tlaBuffer.append(COMMENT).append("MV CONSTANT definitions " + constant.getLeft()).append(CR);
//
//                    String id = addArrowAssignment(constant, CONSTANT_SCHEME);
//                    if (constant.isSymmetricalSet())
//                    {
//                        symmetrySets.add(id);
//                    }
//                    tlaBuffer.append(SEP).append(CR).append(CR);
//                } else
//                {
//                    cfgBuffer.append(COMMENT).append("CONSTANT declarations").append(CR);
//                    // model value assignment
//                    // to .cfg : foo = foo
//                    // to _MC.tla : <nothing>, since the constant is already defined in one of the spec modules
//                    cfgBuffer.append("CONSTANT").append(SPACE).append(constant.getLabel()).append(EQ).append(
//                            constant.getRight()).append(CR);
//                }
            } else
            {
                // simple constant value assignment
                cfgBuffer.append(COMMENT).append("CONSTANT definitions").append(CR);

                tlaBuffer.append(COMMENT).append("CONSTANT definitions ").append(ATTRIBUTE).append(attributeConstants)
                        .append(INDEX).append(i).append(constant.getLeft()).append(CR);
                addArrowAssignment(constant, CONSTANT_SCHEME);
                tlaBuffer.append(SEP).append(CR).append(CR);
            }
        }

        // symmetry
//        if (!symmetrySets.isEmpty())
//        {
//            String label = ModelWriter.getValidIdentifier(SYMMETRY_SCHEME);
//
//            tlaBuffer.append(COMMENT).append("SYMMETRY definition").append(CR);
//            cfgBuffer.append(COMMENT).append("SYMMETRY definition").append(CR);
//
//            tlaBuffer.append(label).append(DEFINES).append(CR);
//            // symmetric model value sets added
//            for (int i = 0; i < symmetrySets.size(); i++)
//            {
//                tlaBuffer.append("Permutations(").append((String) symmetrySets.get(i)).append(")");
//                if (i != symmetrySets.size() - 1)
//                {
//                    tlaBuffer.append(" \\union ");
//                }
//            }
//
//            tlaBuffer.append(CR).append(SEP).append(CR).append(CR);
//            cfgBuffer.append("SYMMETRY").append(SPACE).append(label).append(CR);
//        }

    }


    /**
     * Add the view definition
     * @param viewString the string that the user enters into the view field
     * @param attributeName the attribute name of the view field
     */
    public void addView(String viewString, String attributeName)
    {
        if (!(viewString.trim().length() == 0))
        {
            cfgBuffer.append(COMMENT).append("VIEW definition").append(CR);
            String id = ModelWriter.getValidIdentifier(VIEW_SCHEME);
            cfgBuffer.append("VIEW").append(CR).append(id).append(CR);
            tlaBuffer.append(COMMENT).append("VIEW definition ").append(ATTRIBUTE).append(attributeName).append(CR);
            tlaBuffer.append(id).append(DEFINES).append(CR).append(viewString).append(CR);
            tlaBuffer.append(SEP).append(CR).append(CR);
        }
    }

    /**
     * Adds the ASSUME PrintT statement and identifier for the constant expression
     * evaluation. The MC.tla file will contain:
     * 
     * const_expr_1232141234123 ==
     * expression
     * -----
     * ASSUME PrintT(<<"$!@$!@$!@$!@$!", const_expr_1232141234123>>)
     * 
     * See the comments in the method for an explanation of defining
     * an identifier.
     * 
     * @param expression
     * @param attributeName
     */
    public void addConstantExpressionEvaluation(String expression, String attributeName)
    {
        if (!((expression.trim().length()) == 0))
        {
            /*
             *  Identifier definition
             *  We define an identifier for more sensible error messages
             *  For example, if the user enters "1+" into the constant
             *  expression field and "1+" is placed as the second element
             *  of the tuple that is the argument for PrintT(), then the parse
             *  error would be something like "Encountered >>" which would be
             *  mysterious to the user. With an identifier defined, the message
             *  says "Encountered ----" which is the separator after each section in
             *  MC.tla. This error message is equally mysterious, but at least
             *  it is the same message that would appear were the same error present
             *  in another section in the model editor. We can potentially replace
             *  such messages with something more sensible in the future in the 
             *  appendError() method in TLCErrorView.
             */
            String id = ModelWriter.getValidIdentifier(CONSTANTEXPR_SCHEME);
            tlaBuffer.append(COMMENT).append("Constant expression definition ").append(ATTRIBUTE).append(attributeName)
                    .append(CR);
            tlaBuffer.append(id).append(DEFINES).append(CR).append(expression).append(CR);
            tlaBuffer.append(SEP).append(CR).append(CR);

            // ASSUME PrintT(<<"$!@$!@$!@$!@$!", const_expr_23423232>>) statement
            // The "$!@$!@$!@$!@$!" allows the toolbox to identify the
            // value of the constant expression in the TLC output
            tlaBuffer.append(COMMENT).append("Constant expression ASSUME statement ").append(ATTRIBUTE).append(
                    attributeName).append(CR);
            tlaBuffer.append("ASSUME PrintT(").append(BEGIN_TUPLE).append(CONSTANT_EXPRESSION_EVAL_IDENTIFIER).append(
                    COMMA).append(id).append(END_TUPLE).append(")").append(CR);
            tlaBuffer.append(SEP).append(CR).append(CR);
        }
    }

    /**
     * Assigns a right side to a label using an id generated from given schema
     * @param constant, constant containing the values
     * @param schema schema to generate the Id
     * @return generated id
     */
    public String addArrowAssignment(Assignment constant, String schema)
    {
        // constant instantiation
        // to .cfg : foo <- <id>
        // to _MC.tla : <id>(a, b, c)==
        // <expression>
        String id = ModelWriter.getValidIdentifier(schema);
        tlaBuffer.append(constant.getParametrizedLabel(id)).append(DEFINES).append(CR).append(constant.getRight())
                .append(CR);
        cfgBuffer.append("CONSTANT").append(CR);
        cfgBuffer.append(constant.getLabel()).append(ARROW).append(id).append(CR);
        return id;
    }

    /**
     * Creates a serial version of an MV set in both files
     * @param mvSet typed set containing the model values
     * @param comment a comment to put before the declarations, null and empty strings are OK
     */
    public void addMVTypedSet(TypedSet mvSet, String comment, String attributeName)
    {
        if (mvSet.getValueCount() != 0)
        {
            // create a declaration line
            // CONSTANTS
            // a, b, c
            if (comment != null && !(comment.length() == 0))
            {
                tlaBuffer.append(COMMENT).append(comment).append(ATTRIBUTE).append(attributeName).append(CR);
            }
            tlaBuffer.append("CONSTANTS").append(CR).append(mvSet.toStringWithoutBraces());
            tlaBuffer.append(CR).append(SEP).append(CR).append(CR);

            // create MV assignments
            // a = a
            // b = b
            // c = c
            if (comment != null && !(comment.length() == 0))
            {
                cfgBuffer.append(COMMENT).append(comment).append(CR);
            }
            cfgBuffer.append("CONSTANTS").append(CR);
            String mv;
            for (int i = 0; i < mvSet.getValueCount(); i++)
            {
                mv = mvSet.getValue(i);
                cfgBuffer.append(mv).append(EQ).append(mv).append(CR);
            }
        }
    }
    public void addFormulaList(String element, String keyword, String attributeName) {
    	final List<String[]> elements = new ArrayList<>(1);
    	elements.add(new String[] {element, EMPTY_STRING});
    	addFormulaList(elements, keyword, attributeName);
    }
    
    /**
     * Puts (String[])element[0] to CFG file and element[1] to TLA_MC file, if element[2] present, uses it as index.
     * 
     * @param elements a list of String[] elements
     * @param keyword the keyword to be used in the CFG file
     * @param attributeName the name of the attribute in the model file
     */
    public void addFormulaList(List<String[]> elements, String keyword, String attributeName)
    {
        if (elements.isEmpty())
        {
            return;
        }
        cfgBuffer.append(COMMENT).append(keyword + " definition").append(CR);
        cfgBuffer.append(keyword).append(CR);

        for (int i = 0; i < elements.size(); i++)
        {
            String[] element = elements.get(i);
            cfgBuffer.append(element[0]).append(CR);
            // when a definition in the root module is overridden as a model value
            // there is nothing to add to the MC.tla file so, we do not do the following
            if (!element[1].equals(EMPTY_STRING))
            {
                tlaBuffer.append(COMMENT).append(keyword + " definition ").append(ATTRIBUTE).append(attributeName)
                        .append(INDEX).append(element.length > 2 ? element[2] : i).append(CR);
                tlaBuffer.append(element[1]).append(CR).append(SEP).append(CR);
            }
        }
    }

    /**
     * New definitions are added to the _MC.tla file only
     * @param elements
     */
    public void addNewDefinitions(String definitions, String attributeName)
    {
        if (definitions.trim().length() == 0)
        {
            return;
        }
        tlaBuffer.append(COMMENT).append("New definitions ").append(ATTRIBUTE).append(attributeName).append(CR);
        tlaBuffer.append(definitions).append(CR).append(SEP).append(CR);
    }

    /**
     * Create the content for a single source element
     * @return a list with at most one String[] element
     * @throws CoreException 
     */
    public static List<String[]> createSourceContent(String propertyName, String labelingScheme, ILaunchConfiguration config)
            throws CoreException
    {
        Vector<String[]> result = new Vector<String[]>();
        String value = config.getAttribute(propertyName, EMPTY_STRING);
        if (value.trim().length() == 0)
        {
            return result;
        }
        String identifier = getValidIdentifier(labelingScheme);
        StringBuffer buffer = new StringBuffer();

        // the identifier
        buffer.append(identifier).append(DEFINES_CR);
        buffer.append(value);

        result.add(new String[] { identifier, buffer.toString() });
        return result;
    }

    public static List<String[]> createFalseInit(String var)
    {
        List<String[]> list = new Vector<String[]>();
        String identifier = getValidIdentifier(INIT_SCHEME);
        list.add(new String[] { identifier, identifier + DEFINES_CR + "FALSE/\\" + var + EQ + "0" });
        return list;
    }

    public static List<String[]> createFalseNext(String var)
    {
        List<String[]> list = new Vector<String[]>();
        String identifier = getValidIdentifier(NEXT_SCHEME);
        list.add(new String[] { identifier, identifier + DEFINES_CR + "FALSE/\\" + var + PRIME + EQ + var });
        return list;
    }

    /**
     * Converts formula list to a string representation
     * @param serializedFormulaList, list of strings representing formulas (with enablement flag)
     * @param labelingScheme
     * @return
     */
    public static List<String[]> createFormulaListContent(List<String> serializedFormulaList, String labelingScheme)
    {
        List<Formula> formulaList = ModelHelper.deserializeFormulaList(serializedFormulaList);
        return createFormulaListContentFormula(formulaList, labelingScheme);
    }
    
    public static List<String[]> createFormulaListContentFormula(List<Formula> serializedFormulaList, String labelingScheme)
    {
    	return createListContent(serializedFormulaList, labelingScheme);
    }

    /**
     * Create a list of overrides. If the override is not in the spec's root module, then
     * the config file will have     A <- [M] id . This means that A is defined in module M,
     * and its definition is being overriden in the spec root module which is dependent upon M.
     * The following is an example from Leslie Lamport that explains what occurred before changing
     * the code and what occurs now.
     * Consider the root module

    ----------------- MODULE TestA --------------------
    M(a,b) == INSTANCE TestB WITH CB <- a, CD <- b
    ==================================================

    which imports the module

    ----------------- MODULE TestB --------------------
    CONSTANTS CB, CD

    Foo(x) == <<x, CB, CD>>
    ==================================================

    If you go to definition overrides, you'll find the option of
    overriding M!Foo.  Selecting it, the toolbox asks you to define an operator
    M!Foo of 3 arguments.  If you do it and run TLC, you get the error

    The configuration file substitutes for Foo with
    def_ov_12533499062845000 of different number of arguments.

    Here's what's going on.  The INSTANCE statement imports the operator
    M!Foo into module TestA.  As you may recall, you use that operator
    in an expression by writing something like

    M(42, "a")!F(-13)

    but in the semantic tree, it looks just as if M!F were any other
    operator with three arguments.  When TLC sees the override instruction

    Foo <- [TestB]def_ov_12533495599614000

    in the .cfg file, it tries to substitute an operator def_ov_...  with
    3 arguments for the operator Foo of module TestB that has only a
    single argument.  Hence, the error.

    ------

    Here's the fix.  Instead of giving the user the option of overriding
    M!Foo, in the menu, he should simply see Foo and, if he clicks once
    it, he should see that it's in module TestB. If he chooses to override
    Foo, he should be asked to define an operator of one argument.
    
     * @param overrides
     * @param string
     * @return
     * 
     * Was throwing null-pointer exception when called with spec unparsed.
     * Hacked a fix to handle this case.  LL 20 Sep 2009
     */
    public static List<String[]> createOverridesContent(List<Assignment> overrides, String labelingScheme)
    {
        Vector<String[]> resultContent = new Vector<String[]>(overrides.size());
        String[] content;
        String id;
        Assignment formula;

        // getting the opdefnodes is necessary for retrieving the correct label
        // to appear in the cfg file as explained in the documentation for this method
        SpecObj specObj = ToolboxHandle.getCurrentSpec().getValidRootModule();
        if (specObj == null)
        {
            return resultContent;
        }
        OpDefNode[] opDefNodes = specObj.getExternalModuleTable().getRootModule().getOpDefs();
        Hashtable<String, OpDefNode> nodeTable = new Hashtable<String, OpDefNode>(opDefNodes.length);

        for (int j = 0; j < opDefNodes.length; j++)
        {
            String key = opDefNodes[j].getName().toString();
            nodeTable.put(key, opDefNodes[j]);
        }

        for (int i = 0; i < overrides.size(); i++)
        {
            id = getValidIdentifier(labelingScheme);
            // formulas
            // to .cfg : <id>
            // to _MC.tla : <id> == <expression>
            formula = overrides.get(i);

            OpDefNode defNode = nodeTable.get(formula.getLabel());

            if (defNode == null)
            {
                // should raise an error
                content = null;
            } else
            {
                OpDefNode source = defNode.getSource();
                if (source == defNode)
                {
                    // user is overriding a definition in the root module
                    if (formula.isModelValue() && !formula.isSetOfModelValues())
                    {
                        // model value
                        content = new String[] { formula.getLabel() + EQ + formula.getLabel(), EMPTY_STRING };
                    } else
                    {
                        // not a model value
                        content = new String[] { formula.getLabel() + ARROW + id,
                                formula.getParametrizedLabel(id) + DEFINES_CR + formula.getRight() };
                    }
                } else if (source.getSource() == source)
                {
                    // user is overriding a definition that is not in the root module
                    if (formula.isModelValue() && !formula.isSetOfModelValues())
                    {
                        // model value
                        content = new String[] {
                                source.getName().toString() + ARROW + "["
                                        + source.getOriginallyDefinedInModuleNode().getName().toString() + "]" + id
                                        + " " + id + EQ + source.getName().toString(), "CONSTANT " + id };
                    } else
                    {
                        // not a model value
                        content = new String[] {
                                source.getName().toString() + ARROW + "["
                                        + source.getOriginallyDefinedInModuleNode().getName().toString() + "]" + id,
                                formula.getParametrizedLabel(id) + DEFINES_CR + formula.getRight() };
                    }
                } else
                {
                    // should raise an error window
                    content = null;
                }
            }

            resultContent.add(content);
        }
        return resultContent;
    }

    /**
     * Converts formula list to a string representation
     * @param formulaList list of assignments
     * @param labelingScheme 
     * @return
     */
    public static List<String[]> createListContent(List<Formula> formulaList, String labelingScheme)
    {
        Vector<String[]> resultContent = new Vector<String[]>(formulaList.size());
        String[] content;
        String label;
        for (int i = 0; i < formulaList.size(); i++)
        {
            label = getValidIdentifier(labelingScheme);
            // formulas
            // to .cfg : <id>
            // to _MC.tla : <id> == <expression>
            content = new String[] { label, label + DEFINES_CR + formulaList.get(i).getFormula(), String.valueOf(i) };
            resultContent.add(content);
        }
        return resultContent;
    }

    /**
     * A pattern to match IDs generated by the {@link ModelWriter#getValidIdentifier(String)} method
     */
    public static final Pattern ID_MATCHER = Pattern.compile("(" + SPEC_SCHEME + "|" + INIT_SCHEME + "|" + NEXT_SCHEME
            + "|" + CONSTANT_SCHEME + "|" + SYMMETRY_SCHEME + "|" + DEFOV_SCHEME + "|" + CONSTRAINT_SCHEME + "|"
            + ACTIONCONSTRAINT_SCHEME + "|" + INVARIANT_SCHEME + "|" + PROP_SCHEME + ")_[0-9]{17,}");

    /**
     * Find the IDs in the given text and return the array of 
     * regions pointing to those or an empty array, if no IDs were found.
     * An ID is scheme_timestamp, created by {@link ModelWriter#getValidIdentifier(String)} e.G. next_125195338522638000
     * @param text text containing IDs (error text)
     * @return array of regions or empty array
     */
    public static IRegion[] findIds(String text)
    {
        if (text == null || text.length() == 0)
        {
            return new IRegion[0];
        }

        Matcher matcher = ModelWriter.ID_MATCHER.matcher(text);
        Vector<Region> regions = new Vector<Region>();
        while (matcher.find())
        {
            regions.add(new Region(matcher.start(), matcher.end() - matcher.start()));
        }
        return regions.toArray(new IRegion[regions.size()]);
    }

    /**
     * Retrieves new valid (not used) identifier from given schema
     * @param schema a naming schema, one of the {@link ModelWriter} SCHEMA constants
     * @return a valid identifier
     */
	public static String getValidIdentifier(String schema) {
		return String.format("%s_%s%s", schema, System.currentTimeMillis(), 1000 * COUNTER.incrementAndGet());
	}
}
