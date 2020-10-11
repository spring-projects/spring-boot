package org.springframework.boot.context.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 *
 * An implementation of {@link AbstractFailureAnalyzer} to analyze failures caused by
 * {@link ConfigDataLocationNotFoundException}.
 *
 * @author Michal Mlak
 */
public class ConfigDataLocationNotFoundExceptionFailureAnalyzer
		extends AbstractFailureAnalyzer<ConfigDataLocationNotFoundException> {

	private static final String ACTION = "Make sure a config file is present at the configured path or the path itself is correct.";

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, ConfigDataLocationNotFoundException cause) {
		return new FailureAnalysis(cause.getMessage(), ACTION, cause);
	}

}
