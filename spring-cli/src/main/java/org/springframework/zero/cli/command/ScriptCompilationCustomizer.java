/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.zero.cli.command;

import groovy.lang.Mixin;

import java.util.ArrayList;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.objectweb.asm.Opcodes;
import org.springframework.zero.cli.Command;

/**
 * Customizer for the compilation of CLI commands.
 * 
 * @author Dave Syer
 */
public class ScriptCompilationCustomizer extends CompilationCustomizer {

	public ScriptCompilationCustomizer() {
		super(CompilePhase.CONVERSION);
	}

	@Override
	public void call(SourceUnit source, GeneratorContext context, ClassNode classNode)
			throws CompilationFailedException {
		addOptionHandlerMixin(classNode);
		overrideOptionsMethod(source, classNode);
		addImports(source, context, classNode);
	}

	/**
	 * Add imports to the class node to make writing simple commands easier. No need to
	 * import {@link OptionParser}, {@link OptionSet}, {@link Command} or
	 * {@link OptionHandler}.
	 * 
	 * @param source the source node
	 * @param context the current context
	 * @param classNode the class node to manipulate
	 */
	private void addImports(SourceUnit source, GeneratorContext context,
			ClassNode classNode) {
		ImportCustomizer importCustomizer = new ImportCustomizer();
		importCustomizer.addImports("joptsimple.OptionParser", "joptsimple.OptionSet",
				OptionParsingCommand.class.getCanonicalName(),
				Command.class.getCanonicalName(), OptionHandler.class.getCanonicalName());
		importCustomizer.call(source, context, classNode);
	}

	/**
	 * If the script defines a block in this form:
	 * 
	 * <pre>
	 * options {
	 *   option "foo", "My Foo option"
	 *   option "bar", "Bar has a value" withOptionalArg() ofType Integer
	 * }
	 * </pre>
	 * 
	 * Then the block is taken and used to override the {@link OptionHandler#options()}
	 * method. In the example "option" is a call to
	 * {@link OptionHandler#option(String, String)}, and hence returns an
	 * {@link OptionSpecBuilder}. Makes a nice readable DSL for adding options.
	 * 
	 * @param source the source node
	 * @param classNode the class node to manipulate
	 */
	private void overrideOptionsMethod(SourceUnit source, ClassNode classNode) {

		BlockStatement block = source.getAST().getStatementBlock();
		List<Statement> statements = block.getStatements();

		for (Statement statement : new ArrayList<Statement>(statements)) {
			if (statement instanceof ExpressionStatement) {
				ExpressionStatement expr = (ExpressionStatement) statement;
				Expression expression = expr.getExpression();
				if (expression instanceof MethodCallExpression) {
					MethodCallExpression method = (MethodCallExpression) expression;
					if (method.getMethod().getText().equals("options")) {
						expression = method.getArguments();
						if (expression instanceof ArgumentListExpression) {
							ArgumentListExpression arguments = (ArgumentListExpression) expression;
							expression = arguments.getExpression(0);

							if (expression instanceof ClosureExpression) {
								ClosureExpression closure = (ClosureExpression) expression;
								classNode.addMethod(new MethodNode("options",
										Opcodes.ACC_PROTECTED, ClassHelper.VOID_TYPE,
										new Parameter[0], new ClassNode[0], closure
												.getCode()));
								statements.remove(statement);
							}

						}
					}
				}
			}
		}

	}

	/**
	 * Add {@link OptionHandler} as a mixin to the class node if it doesn't already
	 * declare it as a super class.
	 * 
	 * @param classNode the class node to manipulate
	 */
	private void addOptionHandlerMixin(ClassNode classNode) {
		// If we are not an OptionHandler then add that class as a mixin
		if (!classNode.isDerivedFrom(ClassHelper.make(OptionHandler.class))
				&& !classNode.isDerivedFrom(ClassHelper.make("OptionHandler"))) {
			AnnotationNode mixin = new AnnotationNode(ClassHelper.make(Mixin.class));
			mixin.addMember("value",
					new ClassExpression(ClassHelper.make(OptionHandler.class)));
			classNode.addAnnotation(mixin);
		}
	}

}
