/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.cli.compiler;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

/**
 * Smart extension of {@link ImportCustomizer} that will only add a specific import if a
 * class with the same name is not already explicitly imported.
 *
 * @author Dave Syer
 * @since 1.1
 */
class SmartImportCustomizer extends ImportCustomizer {

	private SourceUnit source;

	SmartImportCustomizer(SourceUnit source) {
		this.source = source;
	}

	@Override
	public ImportCustomizer addImport(String alias, String className) {
		if (this.source.getAST()
				.getImport(ClassHelper.make(className).getNameWithoutPackage()) == null) {
			super.addImport(alias, className);
		}
		return this;
	}

	@Override
	public ImportCustomizer addImports(String... imports) {
		for (String alias : imports) {
			if (this.source.getAST()
					.getImport(ClassHelper.make(alias).getNameWithoutPackage()) == null) {
				super.addImports(alias);
			}
		}
		return this;
	}

}
