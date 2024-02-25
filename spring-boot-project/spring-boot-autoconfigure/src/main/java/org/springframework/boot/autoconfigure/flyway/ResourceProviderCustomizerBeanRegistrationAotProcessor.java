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

package org.springframework.boot.autoconfigure.flyway;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.javapoet.CodeBlock;

/**
 * Replaces the {@link ResourceProviderCustomizer} bean with a
 * {@link NativeImageResourceProviderCustomizer} bean.
 *
 * @author Moritz Halbritter
 */
class ResourceProviderCustomizerBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	/**
	 * Processes the given registered bean ahead of time and returns a
	 * BeanRegistrationAotContribution. If the bean class is ResourceProviderCustomizer,
	 * it creates a new AotContribution with the provided code fragments and the
	 * registered bean, and returns it as a BeanRegistrationAotContribution. If the bean
	 * class is not ResourceProviderCustomizer, it returns null.
	 * @param registeredBean the registered bean to process ahead of time
	 * @return the BeanRegistrationAotContribution for the processed bean, or null if the
	 * bean class is not ResourceProviderCustomizer
	 */
	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		if (registeredBean.getBeanClass().equals(ResourceProviderCustomizer.class)) {
			return BeanRegistrationAotContribution
				.withCustomCodeFragments((codeFragments) -> new AotContribution(codeFragments, registeredBean));
		}
		return null;
	}

	/**
	 * AotContribution class.
	 */
	private static class AotContribution extends BeanRegistrationCodeFragmentsDecorator {

		private final RegisteredBean registeredBean;

		/**
		 * Constructs a new AotContribution object with the specified delegate and
		 * registeredBean.
		 * @param delegate the BeanRegistrationCodeFragments delegate to be used
		 * @param registeredBean the RegisteredBean object to be used
		 */
		protected AotContribution(BeanRegistrationCodeFragments delegate, RegisteredBean registeredBean) {
			super(delegate);
			this.registeredBean = registeredBean;
		}

		/**
		 * Generates the code for the instance supplier method.
		 * @param generationContext The generation context.
		 * @param beanRegistrationCode The bean registration code.
		 * @param allowDirectSupplierShortcut Flag to allow direct supplier shortcut.
		 * @return The code block for the instance supplier method.
		 */
		@Override
		public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
				BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {
			GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("getInstance", (method) -> {
				method.addJavadoc("Get the bean instance for '$L'.", this.registeredBean.getBeanName());
				method.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
				method.returns(NativeImageResourceProviderCustomizer.class);
				CodeBlock.Builder code = CodeBlock.builder();
				code.addStatement("return new $T()", NativeImageResourceProviderCustomizer.class);
				method.addCode(code.build());
			});
			return generatedMethod.toMethodReference().toCodeBlock();
		}

	}

}
