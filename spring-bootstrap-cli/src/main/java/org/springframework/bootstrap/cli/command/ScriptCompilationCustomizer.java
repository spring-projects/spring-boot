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
package org.springframework.bootstrap.cli.command;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

/**
 * @author Dave Syer
 * 
 */
public class ScriptCompilationCustomizer extends CompilationCustomizer {

	public ScriptCompilationCustomizer() {
		super(CompilePhase.CONVERSION);
	}

	@Override
	public void call(SourceUnit source, GeneratorContext context, ClassNode classNode)
			throws CompilationFailedException {
		// AnnotationNode mixin = new AnnotationNode(ClassHelper.make(Mixin.class));
		// mixin.addMember("value",
		// new ClassExpression(ClassHelper.make(OptionHandler.class)));
		// classNode.addAnnotation(mixin);
		ImportCustomizer importCustomizer = new ImportCustomizer();
		importCustomizer.addImports("joptsimple.OptionParser", "joptsimple.OptionSet",
				OptionParsingCommand.class.getCanonicalName(),
				OptionHandler.class.getCanonicalName());
		importCustomizer.call(source, context, classNode);
	}

}
