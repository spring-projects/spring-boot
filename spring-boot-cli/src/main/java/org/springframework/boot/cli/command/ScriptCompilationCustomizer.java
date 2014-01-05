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
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.springframework.boot.cli.Command;
import org.springframework.boot.cli.command.InitCommand.Commands;

/**
 * Customizer for the compilation of CLI script commands.
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
		addImports(source, context, classNode);
	}

	/**
	 * If the script defines a block in this form:
	 * 
	 * <pre>
	 * command("foo") { args ->
	 *     println "Command foo called with args: ${args}"
	 * }
	 * </pre>
	 * 
	 * Then the block is taken and used to create a Command named "foo" that runs the
	 * closure when it is executed.
	 * 
	 * If you want to declare options (and provide help text), use this form:
	 * 
	 * <pre>
	 * command("foo") {
	 * 
	 *   options {
	 *     option "foo", "My Foo option"
	 *     option "bar", "Bar has a value" withOptionalArg() ofType Integer
	 *   }
	 *   
	 *   run { options ->
	 *   	println "Command foo called with bar=${options.valueOf('bar')}"
	 *   }
	 * 
	 * }
	 * </pre>
	 * 
	 * In this case the "options" block is taken and used to override the
	 * {@link OptionHandler#options()} method. Each "option" is a call to
	 * {@link OptionHandler#option(String, String)}, and hence returns an
	 * {@link OptionSpecBuilder}. Makes a nice readable DSL for adding options.
	 * 
	 * @param source the source node
	 * @param classNode the class node to manipulate
	 */
	private void findCommands(SourceUnit source, ClassNode classNode) {
		CommandVisitor visitor = new CommandVisitor(source, classNode);
		classNode.visitContents(visitor);
		visitor.addFactory(classNode);
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
	 * Helper to extract a Commands instance (adding that interface to the current class
	 * node) so individual commands can be registered with the CLI.
	 * 
	 * @author Dave Syer
	 */
	private static class CommandVisitor extends ClassCodeVisitorSupport {

		private SourceUnit source;
		private MapExpression closures = new MapExpression();
		private MapExpression options = new MapExpression();
		private List<ExpressionStatement> statements = new ArrayList<ExpressionStatement>();
		private ExpressionStatement statement;
		private ClassNode classNode;

		public CommandVisitor(SourceUnit source, ClassNode classNode) {
			this.source = source;
			this.classNode = classNode;
		}

		private boolean hasCommands() {
			return !this.closures.getMapEntryExpressions().isEmpty();
		}

		private void addFactory(ClassNode classNode) {
			if (!hasCommands()) {
				return;
			}
			classNode.addInterface(ClassHelper.make(Commands.class));
			classNode.addProperty(new PropertyNode("commands", Modifier.PUBLIC
					| Modifier.FINAL, ClassHelper.MAP_TYPE.getPlainNodeReference(),
					classNode, this.closures, null, null));
			classNode.addProperty(new PropertyNode("options", Modifier.PUBLIC
					| Modifier.FINAL, ClassHelper.MAP_TYPE.getPlainNodeReference(),
					classNode, this.options, null, null));
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
					Expression expression = arguments.getExpression(1);
					if (expression instanceof ClosureExpression) {
						ClosureExpression closure = (ClosureExpression) expression;
						ActionExtractorVisitor action = new ActionExtractorVisitor(
								this.source, this.classNode, name.getText());
						closure.getCode().visit(action);
						if (action.hasOptions()) {
							this.options.addMapEntryExpression(name, action.getOptions());
							expression = action.getAction();
						}
						else {
							expression = new ClosureExpression(
									new Parameter[] { new Parameter(
											ClassHelper.make(String[].class), "args") },
									closure.getCode());
						}
						this.closures.addMapEntryExpression(name, expression);
					}
				}
			}
		}

	}

	/**
	 * Helper to pull out options and action closures from a command declaration (if they
	 * are there).
	 * 
	 * @author Dave Syer
	 */
	private static class ActionExtractorVisitor extends ClassCodeVisitorSupport {

		private static final Parameter[] OPTIONS_PARAMETERS = new Parameter[] { new Parameter(
				ClassHelper.make(OptionSet.class), "options") };
		private SourceUnit source;
		private ClassNode classNode;
		private Expression options;
		private ClosureExpression action;
		private String name;

		public ActionExtractorVisitor(SourceUnit source, ClassNode classNode, String name) {
			this.source = source;
			this.classNode = classNode;
			this.name = name;
		}

		@Override
		protected SourceUnit getSourceUnit() {
			return this.source;
		}

		public boolean hasOptions() {
			return this.options != null;
		}

		public Expression getOptions() {
			return this.options;
		}

		public ClosureExpression getAction() {
			return this.action != null ? this.action : new ClosureExpression(
					OPTIONS_PARAMETERS, new EmptyStatement());
		}

		@Override
		public void visitMethodCallExpression(MethodCallExpression call) {
			Expression methodCall = call.getMethod();
			if (methodCall instanceof ConstantExpression) {
				ConstantExpression method = (ConstantExpression) methodCall;
				if ("options".equals(method.getValue())) {
					ArgumentListExpression arguments = (ArgumentListExpression) call
							.getArguments();
					Expression expression = arguments.getExpression(0);
					if (expression instanceof ClosureExpression) {
						ClosureExpression closure = (ClosureExpression) expression;
						InnerClassNode type = new InnerClassNode(this.classNode,
								this.classNode.getName() + "$" + this.name
										+ "OptionHandler", Modifier.PUBLIC,
								ClassHelper.make(OptionHandler.class));
						type.addMethod("options", Modifier.PROTECTED,
								ClassHelper.VOID_TYPE, Parameter.EMPTY_ARRAY,
								ClassNode.EMPTY_ARRAY, closure.getCode());
						this.classNode.getModule().addClass(type);
						this.options = new ConstructorCallExpression(type,
								ArgumentListExpression.EMPTY_ARGUMENTS);
					}
				}
				else if ("run".equals(method.getValue())) {
					ArgumentListExpression arguments = (ArgumentListExpression) call
							.getArguments();
					Expression expression = arguments.getExpression(0);
					if (expression instanceof ClosureExpression) {
						ClosureExpression closure = (ClosureExpression) expression;
						this.action = new ClosureExpression(OPTIONS_PARAMETERS,
								closure.getCode());
					}
				}
			}
		}
	}

}
