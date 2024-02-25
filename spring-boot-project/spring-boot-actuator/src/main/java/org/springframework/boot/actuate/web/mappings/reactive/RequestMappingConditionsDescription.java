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

package org.springframework.boot.actuate.web.mappings.reactive;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.result.condition.MediaTypeExpression;
import org.springframework.web.reactive.result.condition.NameValueExpression;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.util.pattern.PathPattern;

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

	/**
     * Constructs a new RequestMappingConditionsDescription object based on the given RequestMappingInfo.
     * 
     * @param requestMapping the RequestMappingInfo object to extract conditions from
     */
    RequestMappingConditionsDescription(RequestMappingInfo requestMapping) {
		this.consumes = requestMapping.getConsumesCondition()
			.getExpressions()
			.stream()
			.map(MediaTypeExpressionDescription::new)
			.toList();
		this.headers = requestMapping.getHeadersCondition()
			.getExpressions()
			.stream()
			.map(NameValueExpressionDescription::new)
			.toList();
		this.methods = requestMapping.getMethodsCondition().getMethods();
		this.params = requestMapping.getParamsCondition()
			.getExpressions()
			.stream()
			.map(NameValueExpressionDescription::new)
			.toList();
		this.patterns = requestMapping.getPatternsCondition()
			.getPatterns()
			.stream()
			.map(PathPattern::getPatternString)
			.collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
		this.produces = requestMapping.getProducesCondition()
			.getExpressions()
			.stream()
			.map(MediaTypeExpressionDescription::new)
			.toList();
	}

	/**
     * Returns the list of media type expressions that specify the media types that can be consumed by the request.
     *
     * @return the list of media type expressions that specify the media types that can be consumed by the request
     */
    public List<MediaTypeExpressionDescription> getConsumes() {
		return this.consumes;
	}

	/**
     * Returns the list of headers associated with this RequestMappingConditionsDescription object.
     *
     * @return the list of headers
     */
    public List<NameValueExpressionDescription> getHeaders() {
		return this.headers;
	}

	/**
     * Returns the set of request methods associated with this RequestMappingConditionsDescription.
     *
     * @return the set of request methods
     */
    public Set<RequestMethod> getMethods() {
		return this.methods;
	}

	/**
     * Returns the list of NameValueExpressionDescription objects representing the parameters.
     *
     * @return the list of parameters
     */
    public List<NameValueExpressionDescription> getParams() {
		return this.params;
	}

	/**
     * Returns the set of patterns associated with this RequestMappingConditionsDescription.
     *
     * @return the set of patterns
     */
    public Set<String> getPatterns() {
		return this.patterns;
	}

	/**
     * Returns the list of media type expression descriptions for the produces condition.
     *
     * @return the list of media type expression descriptions for the produces condition
     */
    public List<MediaTypeExpressionDescription> getProduces() {
		return this.produces;
	}

	/**
	 * A description of a {@link MediaTypeExpression} in a request mapping condition.
	 */
	public static class MediaTypeExpressionDescription {

		private final String mediaType;

		private final boolean negated;

		/**
         * Constructs a new MediaTypeExpressionDescription object based on the provided MediaTypeExpression.
         * 
         * @param expression the MediaTypeExpression to be used for constructing the object
         */
        MediaTypeExpressionDescription(MediaTypeExpression expression) {
			this.mediaType = expression.getMediaType().toString();
			this.negated = expression.isNegated();
		}

		/**
         * Returns the media type of the MediaTypeExpressionDescription.
         *
         * @return the media type of the MediaTypeExpressionDescription
         */
        public String getMediaType() {
			return this.mediaType;
		}

		/**
         * Returns a boolean value indicating whether the MediaTypeExpressionDescription is negated.
         *
         * @return true if the MediaTypeExpressionDescription is negated, false otherwise
         */
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

		/**
         * Constructs a new NameValueExpressionDescription object based on the provided NameValueExpression.
         * 
         * @param expression the NameValueExpression to be used for constructing the NameValueExpressionDescription
         */
        NameValueExpressionDescription(NameValueExpression<?> expression) {
			this.name = expression.getName();
			this.value = expression.getValue();
			this.negated = expression.isNegated();
		}

		/**
         * Returns the name of the NameValueExpressionDescription object.
         *
         * @return the name of the NameValueExpressionDescription object
         */
        public String getName() {
			return this.name;
		}

		/**
         * Returns the value of the NameValueExpressionDescription object.
         *
         * @return the value of the NameValueExpressionDescription object
         */
        public Object getValue() {
			return this.value;
		}

		/**
         * Returns a boolean value indicating whether the expression is negated.
         *
         * @return true if the expression is negated, false otherwise.
         */
        public boolean isNegated() {
			return this.negated;
		}

	}

}
