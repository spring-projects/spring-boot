/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.joran.sanity.IfNestedWithinSecondPhaseElementSC;
import ch.qos.logback.classic.model.LoggerModel;
import ch.qos.logback.classic.model.RootLoggerModel;
import ch.qos.logback.core.joran.sanity.Pair;
import ch.qos.logback.core.joran.sanity.SanityChecker;
import ch.qos.logback.core.model.AppenderModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.spi.ContextAwareBase;
import org.jspecify.annotations.Nullable;

/**
 * {@link SanityChecker} to ensure that {@code springProfile} elements are not nested
 * within second-phase elements.
 *
 * @author Phillip Webb
 * @see IfNestedWithinSecondPhaseElementSC
 */
class SpringProfileIfNestedWithinSecondPhaseElementSanityChecker extends ContextAwareBase implements SanityChecker {

	private static final List<Class<? extends Model>> SECOND_PHASE_TYPES = List.of(AppenderModel.class,
			LoggerModel.class, RootLoggerModel.class);

	@Override
	public void check(@Nullable Model model) {
		if (model == null) {
			return;
		}
		List<Model> models = new ArrayList<>();
		SECOND_PHASE_TYPES.forEach((type) -> deepFindAllModelsOfType(type, models, model));
		List<Pair<Model, Model>> nestedPairs = deepFindNestedSubModelsOfType(SpringProfileModel.class, models);
		if (!nestedPairs.isEmpty()) {
			addWarn("<springProfile> elements cannot be nested within an <appender>, <logger> or <root> element");
			nestedPairs.forEach((nested) -> {
				Model first = nested.first;
				Model second = nested.second;
				addWarn("Element <%s> at line %s contains a nested <%s> element at line %s".formatted(first.getTag(),
						first.getLineNumber(), second.getTag(), second.getLineNumber()));
			});
		}
	}

}
