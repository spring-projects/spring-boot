/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.validation;

import java.util.Locale;

import jakarta.validation.MessageInterpolator;
import jakarta.validation.MessageInterpolator.Context;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.StaticMessageSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MessageSourceMessageInterpolator}.
 *
 * @author Dmytro Nosan
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class MessageSourceMessageInterpolatorTests {

	private final Context context = mock(Context.class);

	private final StaticMessageSource messageSource = new StaticMessageSource();

	private final MessageSourceMessageInterpolator interpolator = new MessageSourceMessageInterpolator(
			this.messageSource, new IdentityMessageInterpolator());

	@Test
	void interpolateShouldReplaceParameters() {
		this.messageSource.addMessage("foo", Locale.getDefault(), "fooValue");
		this.messageSource.addMessage("bar", Locale.getDefault(), "");
		assertThat(this.interpolator.interpolate("{foo}{bar}", this.context)).isEqualTo("fooValue");
	}

	@Test
	void interpolateWhenParametersAreUnknownShouldLeaveThemUnchanged() {
		this.messageSource.addMessage("top", Locale.getDefault(), "{child}+{child}");
		assertThat(this.interpolator.interpolate("{foo}{top}{bar}", this.context))
			.isEqualTo("{foo}{child}+{child}{bar}");
	}

	@Test
	void interpolateWhenParametersAreUnknownUsingCodeAsDefaultShouldLeaveThemUnchanged() {
		this.messageSource.setUseCodeAsDefaultMessage(true);
		this.messageSource.addMessage("top", Locale.getDefault(), "{child}+{child}");
		assertThat(this.interpolator.interpolate("{foo}{top}{bar}", this.context))
			.isEqualTo("{foo}{child}+{child}{bar}");
	}

	@Test
	void interpolateShouldReplaceParameterThatReferencesAMessageThatMatchesItsCode() {
		this.messageSource.addMessage("foo", Locale.getDefault(), "foo");
		assertThat(this.interpolator.interpolate("{foo}", this.context)).isEqualTo("foo");
	}

	@Test
	void interpolateUsingCodeAsDefaultShouldReplaceParameterThatReferencesAMessageThatMatchesItsCode() {
		this.messageSource.setUseCodeAsDefaultMessage(true);
		this.messageSource.addMessage("foo", Locale.getDefault(), "foo");
		assertThat(this.interpolator.interpolate("{foo}", this.context)).isEqualTo("foo");
	}

	@Test
	void interpolateWhenParametersAreNestedShouldFullyReplaceAllParameters() {
		this.messageSource.addMessage("top", Locale.getDefault(), "{child}+{child}");
		this.messageSource.addMessage("child", Locale.getDefault(), "{{differentiator}.grandchild}");
		this.messageSource.addMessage("differentiator", Locale.getDefault(), "first");
		this.messageSource.addMessage("first.grandchild", Locale.getDefault(), "actualValue");
		assertThat(this.interpolator.interpolate("{top}", this.context)).isEqualTo("actualValue+actualValue");
	}

	@Test
	void interpolateWhenParameterBracesAreUnbalancedShouldLeaveThemUnchanged() {
		this.messageSource.addMessage("top", Locale.getDefault(), "topValue");
		assertThat(this.interpolator.interpolate("\\{top}", this.context)).isEqualTo("\\{top}");
		assertThat(this.interpolator.interpolate("{top\\}", this.context)).isEqualTo("{top\\}");
		assertThat(this.interpolator.interpolate("{{top}", this.context)).isEqualTo("{{top}");
		assertThat(this.interpolator.interpolate("{top}}", this.context)).isEqualTo("topValue}");
	}

	@Test
	void interpolateWhenBracesAreEscapedShouldIgnore() {
		this.messageSource.addMessage("foo", Locale.getDefault(), "fooValue");
		this.messageSource.addMessage("bar", Locale.getDefault(), "\\{foo}");
		this.messageSource.addMessage("bazz\\}", Locale.getDefault(), "bazzValue");
		assertThat(this.interpolator.interpolate("{foo}", this.context)).isEqualTo("fooValue");
		assertThat(this.interpolator.interpolate("{foo}\\a", this.context)).isEqualTo("fooValue\\a");
		assertThat(this.interpolator.interpolate("\\\\{foo}", this.context)).isEqualTo("\\\\fooValue");
		assertThat(this.interpolator.interpolate("\\\\\\{foo}", this.context)).isEqualTo("\\\\\\{foo}");
		assertThat(this.interpolator.interpolate("\\{foo}", this.context)).isEqualTo("\\{foo}");
		assertThat(this.interpolator.interpolate("{foo\\}", this.context)).isEqualTo("{foo\\}");
		assertThat(this.interpolator.interpolate("\\{foo\\}", this.context)).isEqualTo("\\{foo\\}");
		assertThat(this.interpolator.interpolate("{foo}\\", this.context)).isEqualTo("fooValue\\");
		assertThat(this.interpolator.interpolate("{bar}", this.context)).isEqualTo("\\{foo}");
		assertThat(this.interpolator.interpolate("{bazz\\}}", this.context)).isEqualTo("bazzValue");
	}

	@Test
	void interpolateWhenParametersContainACycleShouldThrow() {
		this.messageSource.addMessage("a", Locale.getDefault(), "{b}");
		this.messageSource.addMessage("b", Locale.getDefault(), "{c}");
		this.messageSource.addMessage("c", Locale.getDefault(), "{a}");
		assertThatIllegalArgumentException().isThrownBy(() -> this.interpolator.interpolate("{a}", this.context))
			.withMessage("Circular reference '{a -> b -> c -> a}'");
	}

	private static final class IdentityMessageInterpolator implements MessageInterpolator {

		@Override
		public String interpolate(String messageTemplate, Context context) {
			return messageTemplate;
		}

		@Override
		public String interpolate(String messageTemplate, Context context, Locale locale) {
			return messageTemplate;
		}

	}

}
