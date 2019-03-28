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

package org.springframework.boot.actuate.web.mappings.servlet;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.condition.MediaTypeExpression;
import org.springframework.web.servlet.mvc.condition.NameValueExpression;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

/**
 * Description of the conditions of a {@link RequestMappingInfo}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class RequestMappingConditionsDescription {

	private final List<MediaTypeExpressionDescription> consumes;

	private final List<NameValueExpressionDescription> headers;

	private final Set<RequestMethod> methods;

	private final List<NameValueExpressionDescription> params;

	private final Set<String> patterns;

	private final List<MediaTypeExpressionDescription> produces;

	RequestMappingConditionsDescription(RequestMappingInfo requestMapping) {
		this.consumes = requestMapping.getConsumesCondition().getExpressions().stream()
				.map(MediaTypeExpressionDescription::new).collect(Collectors.toList());
		this.headers = requestMapping.getHeadersCondition().getExpressions().stream()
				.map(NameValueExpressionDescription::new).collect(Collectors.toList());
		this.methods = requestMapping.getMethodsCondition().getMethods();
		this.params = requestMapping.getParamsCondition().getExpressions().stream()
				.map(NameValueExpressionDescription::new).collect(Collectors.toList());
		this.patterns = requestMapping.getPatternsCondition().getPatterns();
		this.produces = requestMapping.getProducesCondition().getExpressions().stream()
				.map(MediaTypeExpressionDescription::new).collect(Collectors.toList());
	}

	public List<MediaTypeExpressionDescription> getConsumes() {
		return this.consumes;
	}

	public List<NameValueExpressionDescription> getHeaders() {
		return this.headers;
	}

	public Set<RequestMethod> getMethods() {
		return this.methods;
	}

	public List<NameValueExpressionDescription> getParams() {
		return this.params;
	}

	public Set<String> getPatterns() {
		return this.patterns;
	}

	public List<MediaTypeExpressionDescription> getProduces() {
		return this.produces;
	}

	/**
	 * A description of a {@link MediaTypeExpression} in a request mapping condition.
	 */
	public static class MediaTypeExpressionDescription {

		private final String mediaType;

		private final boolean negated;

		MediaTypeExpressionDescription(MediaTypeExpression expression) {
			this.mediaType = expression.getMediaType().toString();
			this.negated = expression.isNegated();
		}

		public String getMediaType() {
			return this.mediaType;
		}

		public boolean isNegated() {
			return this.negated;
		}

	}

	/**
	 * A description of a {@link NameValueExpression} in a request mapping condition.
	 */
	public static class NameValueExpressionDescription {

		private final String name;

		private final Object value;

		private final boolean negated;

		NameValueExpressionDescription(NameValueExpression<?> expression) {
			this.name = expression.getName();
			this.value = expression.getValue();
			this.negated = expression.isNegated();
		}

		public String getName() {
			return this.name;
		}

		public Object getValue() {
			return this.value;
		}

		public boolean isNegated() {
			return this.negated;
		}

	}

}
