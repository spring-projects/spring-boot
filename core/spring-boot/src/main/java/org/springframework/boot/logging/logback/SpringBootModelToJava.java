/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import java.io.IOException;
import java.util.List;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.action.ActionUtil;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.DefaultProcessor;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.model.util.PropertyModelHandlerHelper;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.tyler.base.ModelToJava;
import ch.qos.logback.tyler.base.TranslationOptions;
import ch.qos.logback.tyler.base.TylerModelInterpretationContext;

/**
 * Extended version of the Logback Tyler {@link ModelToJava} that translates a Logback XML
 * configuration, including Spring Boot's {@code <springProperty>} and
 * {@code <springProfile>} elements, into the source code of a Logback configurator. Used
 * during AOT processing.
 *
 * @author Ceki Gülcü
 * @see SpringBootTylerConfiguratorBase
 */
class SpringBootModelToJava extends ModelToJava {

	private static final TranslationOptions TRANSLATION_OPTIONS = TranslationOptions.builder()
		.translationTimeInclusionEnabled(true)
		.generatedClassName(SpringBootTylerConfiguratorBase.GENERATED_CLASS_NAME)
		.generatedClassSuperclassName(SpringBootTylerConfiguratorBase.class.getName())
		.build();

	SpringBootModelToJava() {
		super(new LoggerContext(), TRANSLATION_OPTIONS);
	}

	/**
	 * Translate the given Logback XML configuration into the source code of a
	 * configurator named {@link SpringBootTylerConfiguratorBase#GENERATED_CLASS_NAME}.
	 * @param xml the XML configuration
	 * @return the source code of the configurator
	 * @throws IllegalStateException if the configuration cannot be translated completely
	 */
	String translate(String xml) {
		try {
			Model model = extractModel(xml);
			checkStatus("parsing");
			SpringProfileIfNestedWithinSecondPhaseElementSanityChecker sanityChecker = new SpringProfileIfNestedWithinSecondPhaseElementSanityChecker();
			sanityChecker.setContext(getContext());
			sanityChecker.check(model);
			checkStatus("checking");
			String source = toJava(model);
			checkStatus("translating");
			return source;
		}
		catch (JoranException | IOException ex) {
			throw new IllegalStateException("Failed to translate Logback configuration", ex);
		}
	}

	private void checkStatus(String step) {
		List<Status> problems = getContext().getStatusManager()
			.getCopyOfStatusList()
			.stream()
			.filter((status) -> status.getEffectiveLevel() >= Status.WARN)
			.toList();
		if (!problems.isEmpty()) {
			StringBuilder message = new StringBuilder(
					"Logback configuration problems detected while " + step + " the configuration for AOT processing:");
			problems.forEach((problem) -> message.append(String.format("%n%s", problem)));
			throw new IllegalStateException(message.toString());
		}
	}

	@Override
	protected JoranConfigurator createJoranConfigurator() {
		return new JoranConfigurator() {

			@Override
			public void addElementSelectorAndActionAssociations(RuleStore ruleStore) {
				super.addElementSelectorAndActionAssociations(ruleStore);
				SpringBootJoranConfigurator.addSpringBootRules(ruleStore);
			}

		};
	}

	@Override
	protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
		super.addModelHandlerAssociations(defaultProcessor);
		defaultProcessor.addHandler(SpringPropertyModel.class,
				(context, interpretationContext) -> new TylerSpringPropertyModelHandler(context));
		defaultProcessor.addHandler(SpringProfileModel.class,
				(context, interpretationContext) -> new TylerSpringProfileModelHandler(context));
	}

	/**
	 * Emits code for a {@code <springProperty>} element. The property is resolved against
	 * the runtime {@code Environment} by
	 * {@link SpringBootTylerConfiguratorBase#springProperty(String, String)}.
	 */
	private static final class TylerSpringPropertyModelHandler extends ModelHandlerBase {

		TylerSpringPropertyModelHandler(Context context) {
			super(context);
		}

		@Override
		public void handle(ModelInterpretationContext intercon, Model model) {
			SpringPropertyModel propertyModel = (SpringPropertyModel) model;
			if (OptionHelper.isNullOrEmpty(propertyModel.getName())
					|| OptionHelper.isNullOrEmpty(propertyModel.getSource())) {
				addError("The \"name\" and \"source\" attributes of <springProperty> must be set");
				return;
			}
			TylerModelInterpretationContext tylerContext = (TylerModelInterpretationContext) intercon;
			tylerContext.getConfigureMethodSpecBuilder()
				.addStatement("$T.setProperty(this, $S, springProperty($S, $S), $T.stringToScope($S))",
						PropertyModelHandlerHelper.class, propertyModel.getName(), propertyModel.getSource(),
						propertyModel.getDefaultValue(), ActionUtil.class, propertyModel.getScope());
		}

	}

	/**
	 * Emits code for a {@code <springProfile>} element. Nested elements are only applied
	 * if the runtime {@code Environment} accepts the profiles, as determined by
	 * {@link SpringBootTylerConfiguratorBase#acceptsProfiles(String)}.
	 */
	private static final class TylerSpringProfileModelHandler extends ModelHandlerBase {

		TylerSpringProfileModelHandler(Context context) {
			super(context);
		}

		@Override
		public void handle(ModelInterpretationContext intercon, Model model) {
			TylerModelInterpretationContext tylerContext = (TylerModelInterpretationContext) intercon;
			tylerContext.getConfigureMethodSpecBuilder()
				.beginControlFlow("if (acceptsProfiles($S))", ((SpringProfileModel) model).getName());
		}

		@Override
		public void postHandle(ModelInterpretationContext intercon, Model model) {
			TylerModelInterpretationContext tylerContext = (TylerModelInterpretationContext) intercon;
			tylerContext.getConfigureMethodSpecBuilder().endControlFlow();
		}

	}

}
