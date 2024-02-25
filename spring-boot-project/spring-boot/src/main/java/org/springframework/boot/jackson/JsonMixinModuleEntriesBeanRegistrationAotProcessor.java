/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.jackson;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.AccessControl;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;

/**
 * {@link BeanRegistrationAotProcessor} that replaces any {@link JsonMixinModuleEntries}
 * by an hard-coded equivalent. This has the effect of disabling scanning at runtime.
 *
 * @author Stephane Nicoll
 */
class JsonMixinModuleEntriesBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	/**
     * Processes the given registered bean ahead of time.
     * 
     * @param registeredBean the registered bean to process
     * @return the bean registration AOT contribution
     */
    @Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		if (registeredBean.getBeanClass().equals(JsonMixinModuleEntries.class)) {
			return BeanRegistrationAotContribution
				.withCustomCodeFragments((codeFragments) -> new AotContribution(codeFragments, registeredBean));
		}
		return null;
	}

	/**
     * AotContribution class.
     */
    static class AotContribution extends BeanRegistrationCodeFragmentsDecorator {

		private static final Class<?> BEAN_TYPE = JsonMixinModuleEntries.class;

		private final RegisteredBean registeredBean;

		private final ClassLoader classLoader;

		/**
         * Constructs a new AotContribution object with the specified delegate and registeredBean.
         * 
         * @param delegate the BeanRegistrationCodeFragments delegate
         * @param registeredBean the RegisteredBean object
         */
        AotContribution(BeanRegistrationCodeFragments delegate, RegisteredBean registeredBean) {
			super(delegate);
			this.registeredBean = registeredBean;
			this.classLoader = registeredBean.getBeanFactory().getBeanClassLoader();
		}

		/**
         * Returns the target class name for the given registered bean.
         *
         * @param registeredBean the registered bean
         * @return the target class name
         */
        @Override
		public ClassName getTarget(RegisteredBean registeredBean) {
			return ClassName.get(BEAN_TYPE);
		}

		/**
         * Generates the code for the instance supplier of the registered bean.
         * 
         * @param generationContext The generation context.
         * @param beanRegistrationCode The bean registration code.
         * @param allowDirectSupplierShortcut Flag indicating whether direct supplier shortcut is allowed.
         * @return The code block representing the instance supplier code.
         */
        @Override
		public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
				BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {
			JsonMixinModuleEntries entries = this.registeredBean.getBeanFactory()
				.getBean(this.registeredBean.getBeanName(), JsonMixinModuleEntries.class);
			contributeHints(generationContext.getRuntimeHints(), entries);
			GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("getInstance", (method) -> {
				method.addJavadoc("Get the bean instance for '$L'.", this.registeredBean.getBeanName());
				method.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
				method.returns(BEAN_TYPE);
				CodeBlock.Builder code = CodeBlock.builder();
				code.add("return $T.create(", JsonMixinModuleEntries.class).beginControlFlow("(mixins) ->");
				entries.doWithEntry(this.classLoader, (type, mixin) -> addEntryCode(code, type, mixin));
				code.endControlFlow(")");
				method.addCode(code.build());
			});
			return generatedMethod.toMethodReference().toCodeBlock();
		}

		/**
         * Adds an entry code to the given {@link CodeBlock.Builder} for the specified type and mixin.
         * The access control for the types is determined using the lowest access control level between the type and mixin.
         * If the access control level is public, the entry code is added using the type and mixin classes.
         * Otherwise, the entry code is added using the fully qualified names of the type and mixin.
         *
         * @param code  the {@link CodeBlock.Builder} to add the entry code to
         * @param type  the type class
         * @param mixin the mixin class
         */
        private void addEntryCode(CodeBlock.Builder code, Class<?> type, Class<?> mixin) {
			AccessControl accessForTypes = AccessControl.lowest(AccessControl.forClass(type),
					AccessControl.forClass(mixin));
			if (accessForTypes.isPublic()) {
				code.addStatement("$L.and($T.class, $T.class)", "mixins", type, mixin);
			}
			else {
				code.addStatement("$L.and($S, $S)", "mixins", type.getName(), mixin.getName());
			}
		}

		/**
         * Contributes hints to the runtime hints and registers reflection hints for the given mixins.
         * 
         * @param runtimeHints the runtime hints to contribute to
         * @param entries the JSON mixin module entries
         */
        private void contributeHints(RuntimeHints runtimeHints, JsonMixinModuleEntries entries) {
			Set<Class<?>> mixins = new LinkedHashSet<>();
			entries.doWithEntry(this.classLoader, (type, mixin) -> mixins.add(mixin));
			new BindingReflectionHintsRegistrar().registerReflectionHints(runtimeHints.reflection(),
					mixins.toArray(Class<?>[]::new));
		}

	}

}
