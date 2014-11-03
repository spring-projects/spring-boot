/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.configurationprocessor.fieldvalues.javac;

import java.lang.reflect.Method;

/**
 * Reflection based access to {@code com.sun.source.tree.ExpressionTree}.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
class ExpressionTree extends ReflectionWrapper {

	private final Class<?> literalTreeType = findClass("com.sun.source.tree.LiteralTree");

	private final Method literalValueMethod = findMethod(this.literalTreeType, "getValue");

	public ExpressionTree(Object instance) {
		super(instance);
	}

	public String getKind() throws Exception {
		return findMethod("getKind").invoke(getInstance()).toString();
	}

	public Object getLiteralValue() throws Exception {
		if (this.literalTreeType.isAssignableFrom(getInstance().getClass())) {
			return this.literalValueMethod.invoke(getInstance());
		}
		return null;
	}

}
