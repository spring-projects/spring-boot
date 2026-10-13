/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson2;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.lang.model.element.Modifier;

import org.jspecify.annotations.Nullable;

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
import org.springframework.beans.factory.aot.CodeWarnings;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;

/**
 * {@link BeanRegistrationAotProcessor} that replaces any {@link JsonMixinModuleEntries}
 * by a hard-coded equivalent. This has the effect of disabling scanning at runtime.
 *
 * @author Stephane Nicoll
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
class JsonMixinModuleEntriesBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	@Override
	public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		if (registeredBean.getBeanClass().equals(JsonMixinModuleEntries.class)) {
			return BeanRegistrationAotContribution
				.withCustomCodeFragments((codeFragments) -> new AotContribution(codeFragments, registeredBean));
		}
		return null;
	}

	static class AotContribution extends BeanRegistrationCodeFragmentsDecorator {

		private static final Class<?> BEAN_TYPE = JsonMixinModuleEntries.class;

		private final RegisteredBean registeredBean;

		private final @Nullable ClassLoader classLoader;

		AotContribution(BeanRegistrationCodeFragments delegate, RegisteredBean registeredBean) {
			super(delegate);
			this.registeredBean = registeredBean;
			this.classLoader = registeredBean.getBeanFactory().getBeanClassLoader();
		}

		@Override
		public ClassName getTarget(RegisteredBean registeredBean) {
			return ClassName.get(BEAN_TYPE);
		}

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
				CodeWarnings codeWarnings = new CodeWarnings();
				codeWarnings.detectDeprecation(BEAN_TYPE);
				code.add("return $T.create(", BEAN_TYPE).beginControlFlow("(mixins) ->");
				entries.doWithEntry(this.classLoader, (type, mixin) -> {
					codeWarnings.detectDeprecation(type, mixin);
					addEntryCode(code, type, mixin);
				});
				code.endControlFlow(")");
				codeWarnings.suppress(method);
				method.addCode(code.build());
			});
			return generatedMethod.toMethodReference().toCodeBlock();
		}

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

		private void contributeHints(RuntimeHints runtimeHints, JsonMixinModuleEntries entries) {
			Set<Class<?>> mixins = new LinkedHashSet<>();
			entries.doWithEntry(this.classLoader, (type, mixin) -> mixins.add(mixin));
			new BindingReflectionHintsRegistrar().registerReflectionHints(runtimeHints.reflection(),
					mixins.toArray(Class<?>[]::new));
		}

	}

}
