/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.testcontainers.context;

import java.lang.reflect.Field;

import javax.lang.model.element.Modifier;

import org.testcontainers.containers.Container;

import org.springframework.aot.generate.AccessControl;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.testcontainers.beans.TestcontainerBeanDefinition;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.javapoet.AnnotationSpec;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RootBeanDefinition} used for testcontainer bean definitions.
 *
 * @author Phillip Webb
 */
class TestcontainerFieldBeanDefinition extends RootBeanDefinition implements TestcontainerBeanDefinition {

	private final Container<?> container;

	private final MergedAnnotations annotations;

	TestcontainerFieldBeanDefinition(Field field, Container<?> container) {
		this.container = container;
		this.annotations = MergedAnnotations.from(field);
		setBeanClass(container.getClass());
		setInstanceSupplier(() -> container);
		setRole(ROLE_INFRASTRUCTURE);
		setAttribute(TestcontainerFieldBeanDefinition.class.getName(), field);
	}

	@Override
	public String getContainerImageName() {
		return this.container.getDockerImageName();
	}

	@Override
	public MergedAnnotations getAnnotations() {
		return this.annotations;
	}

	/**
	 * {@link BeanRegistrationAotProcessor} that replaces InstanceSupplier of
	 * {@link Container} by either direct field usage or a reflection equivalent.
	 * <p>
	 * If the field is inaccessible, the reflection will be used; otherwise, direct access
	 * to the field will be used.
	 *
	 */
	static class TestcontainersBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

		@Override
		public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
			RootBeanDefinition bd = registeredBean.getMergedBeanDefinition();
			String attributeName = TestcontainerFieldBeanDefinition.class.getName();
			Object field = bd.getAttribute(attributeName);
			if (field != null) {
				Assert.isInstanceOf(Field.class, field, "BeanDefinition attribute '" + attributeName
						+ "' value must be a type of '" + Field.class + "'");
				return BeanRegistrationAotContribution.withCustomCodeFragments(
						(codeFragments) -> new AotContribution(codeFragments, registeredBean, ((Field) field)));
			}
			return null;
		}

		private static final class AotContribution extends BeanRegistrationCodeFragmentsDecorator {

			private final RegisteredBean registeredBean;

			private final Field field;

			private AotContribution(BeanRegistrationCodeFragments delegate, RegisteredBean registeredBean,
					Field field) {
				super(delegate);
				this.registeredBean = registeredBean;
				this.field = field;
			}

			@Override
			public ClassName getTarget(RegisteredBean registeredBean) {
				return ClassName.get(this.field.getDeclaringClass());
			}

			@Override
			public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
					BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {

				if (isFieldAccessible(beanRegistrationCode, this.field)) {
					return CodeBlock.of("() -> $T.$L", this.field.getDeclaringClass(), this.field.getName());
				}

				generationContext.getRuntimeHints().reflection().registerField(this.field);

				return beanRegistrationCode.getMethods()
					.add("getInstance", (method) -> method.addModifiers(Modifier.PRIVATE, Modifier.STATIC)
						.addJavadoc("Get the bean instance for '$L'.", this.registeredBean.getBeanName())
						.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
							.addMember("value", "$S", "rawtypes")
							.build())
						.returns(this.registeredBean.getBeanClass())
						.addStatement("$T<?> clazz = $T.resolveClassName($S, $T.class.getClassLoader())", Class.class,
								ClassUtils.class, this.field.getDeclaringClass().getTypeName(),
								beanRegistrationCode.getClassName())
						.addStatement("$T field = $T.findField(clazz, $S)", Field.class, ReflectionUtils.class,
								this.field.getName())
						.addStatement("$T.notNull(field, $S)", Assert.class,
								"Field '" + this.field.getName() + "' is not found")
						.addStatement("$T.makeAccessible(field)", ReflectionUtils.class)
						.addStatement("$T container = $T.getField(field, null)", Object.class, ReflectionUtils.class)
						.addStatement("$T.notNull(container, $S)", Assert.class,
								"Container field '" + this.field.getName() + "' must not have a null value")
						.addStatement("return ($T) container", this.registeredBean.getBeanClass()))
					.toMethodReference()
					.toCodeBlock();
			}

			private static boolean isFieldAccessible(BeanRegistrationCode beanRegistrationCode, Field field) {
				ClassName className = beanRegistrationCode.getClassName();
				return AccessControl.forClass(field.getDeclaringClass()).isAccessibleFrom(className)
						&& AccessControl.forMember(field).isAccessibleFrom(className);
			}

		}

	}

}
