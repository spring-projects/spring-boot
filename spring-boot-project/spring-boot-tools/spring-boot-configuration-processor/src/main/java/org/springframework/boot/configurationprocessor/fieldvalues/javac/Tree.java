/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.configurationprocessor.fieldvalues.javac;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Reflection based access to {@code com.sun.source.tree.Tree}.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
class Tree extends ReflectionWrapper {

	private final Class<?> treeVisitorType = findClass("com.sun.source.tree.TreeVisitor");

	private final Method acceptMethod = findMethod("accept", this.treeVisitorType,
			Object.class);

	private final Method getClassTreeMembers = findMethod(
			findClass("com.sun.source.tree.ClassTree"), "getMembers");

	Tree(Object instance) {
		super("com.sun.source.tree.Tree", instance);
	}

	public void accept(TreeVisitor visitor) throws Exception {
		this.acceptMethod.invoke(getInstance(),
				Proxy.newProxyInstance(getInstance().getClass().getClassLoader(),
						new Class<?>[] { this.treeVisitorType },
						new TreeVisitorInvocationHandler(visitor)),
				0);
	}

	/**
	 * {@link InvocationHandler} to call the {@link TreeVisitor}.
	 */
	private class TreeVisitorInvocationHandler implements InvocationHandler {

		private TreeVisitor treeVisitor;

		TreeVisitorInvocationHandler(TreeVisitor treeVisitor) {
			this.treeVisitor = treeVisitor;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			if (method.getName().equals("visitClass") && (Integer) args[1] == 0) {
				Iterable members = (Iterable) Tree.this.getClassTreeMembers
						.invoke(args[0]);
				for (Object member : members) {
					if (member != null) {
						Tree.this.acceptMethod.invoke(member, proxy,
								((Integer) args[1]) + 1);
					}
				}
			}
			if (method.getName().equals("visitVariable")) {
				this.treeVisitor.visitVariable(new VariableTree(args[0]));
			}
			return null;
		}

	}

}
