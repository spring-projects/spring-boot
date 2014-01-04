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

package org.springframework.boot.cli.command;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;

import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapExpression;
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
import org.springframework.asm.Opcodes;
import org.springframework.boot.cli.Command;
import org.springframework.boot.cli.command.InitCommand.Commands;

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
		findCommands(source, classNode);
		overrideOptionsMethod(source, classNode);
		addImports(source, context, classNode);
	}

	private void findCommands(SourceUnit source, ClassNode classNode) {
		CommandVisitor visitor = new CommandVisitor(source);
		classNode.visitContents(visitor);
		visitor.addFactory(classNode);
	}

	private static class CommandVisitor extends ClassCodeVisitorSupport {

		private SourceUnit source;
		private MapExpression map = new MapExpression();
		private List<ExpressionStatement> statements = new ArrayList<ExpressionStatement>();
		private ExpressionStatement statement;

		public CommandVisitor(SourceUnit source) {
			this.source = source;
		}

		private boolean hasCommands() {
			return !this.map.getMapEntryExpressions().isEmpty();
		}

		private void addFactory(ClassNode classNode) {
			if (!hasCommands()) {
				return;
			}
			classNode.addInterface(ClassHelper.make(Commands.class));
			classNode.addProperty(new PropertyNode("commands", Modifier.PUBLIC
					| Modifier.FINAL, ClassHelper.MAP_TYPE.getPlainNodeReference(),
					classNode, this.map, null, null));
		}

		@Override
		protected SourceUnit getSourceUnit() {
			return this.source;
		}

		@Override
		public void visitBlockStatement(BlockStatement block) {
			this.statements.clear();
			super.visitBlockStatement(block);
			block.getStatements().removeAll(this.statements);
		}

		@Override
		public void visitExpressionStatement(ExpressionStatement statement) {
			this.statement = statement;
			super.visitExpressionStatement(statement);
		}

		@Override
		public void visitMethodCallExpression(MethodCallExpression call) {
			Expression methodCall = call.getMethod();
			if (methodCall instanceof ConstantExpression) {
				ConstantExpression method = (ConstantExpression) methodCall;
				if ("command".equals(method.getValue())) {
					ArgumentListExpression arguments = (ArgumentListExpression) call
							.getArguments();
					this.statements.add(this.statement);
					ConstantExpression name = (ConstantExpression) arguments
							.getExpression(0);
					ClosureExpression closure = (ClosureExpression) arguments
							.getExpression(1);
					this.map.addMapEntryExpression(name, closure);
				}
			}
		}

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

		ClosureExpression closure = options(source, classNode);
		if (closure != null) {
			classNode.addMethod(new MethodNode("options", Opcodes.ACC_PROTECTED,
					ClassHelper.VOID_TYPE, new Parameter[0], new ClassNode[0], closure
							.getCode()));
			classNode.setSuperClass(ClassHelper.make(OptionHandler.class));
		}

	}

	private ClosureExpression options(SourceUnit source, ClassNode classNode) {

		BlockStatement block = source.getAST().getStatementBlock();
		List<Statement> statements = block.getStatements();

		for (Statement statement : new ArrayList<Statement>(statements)) {
			if (statement instanceof ExpressionStatement) {
				ExpressionStatement expr = (ExpressionStatement) statement;
				Expression expression = expr.getExpression();
				if (expression instanceof MethodCallExpression) {
					MethodCallExpression method = (MethodCallExpression) expression;
					if (method.getMethod().getText().equals("options")) {
						statements.remove(statement);
						expression = method.getArguments();
						if (expression instanceof ArgumentListExpression) {
							ArgumentListExpression arguments = (ArgumentListExpression) expression;
							expression = arguments.getExpression(0);
							if (expression instanceof ClosureExpression) {
								return (ClosureExpression) expression;
							}
						}
					}
				}
			}
		}

		return null;

	}

}
