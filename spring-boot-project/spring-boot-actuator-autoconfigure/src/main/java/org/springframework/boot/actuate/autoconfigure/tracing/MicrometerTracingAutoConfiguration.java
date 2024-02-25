/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing;

import io.micrometer.common.annotation.ValueExpressionResolver;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.annotation.DefaultNewSpanParser;
import io.micrometer.tracing.annotation.ImperativeMethodInvocationProcessor;
import io.micrometer.tracing.annotation.MethodInvocationProcessor;
import io.micrometer.tracing.annotation.NewSpanParser;
import io.micrometer.tracing.annotation.SpanAspect;
import io.micrometer.tracing.annotation.SpanTagAnnotationHandler;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import org.aspectj.weaver.Advice;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the Micrometer Tracing API.
 *
 * @author Moritz Halbritter
 * @author Jonatan Ivanov
 * @since 3.0.0
 */
@AutoConfiguration
@ConditionalOnClass(Tracer.class)
@ConditionalOnBean(Tracer.class)
public class MicrometerTracingAutoConfiguration {

	/**
	 * {@code @Order} value of {@link #defaultTracingObservationHandler(Tracer)}.
	 */
	public static final int DEFAULT_TRACING_OBSERVATION_HANDLER_ORDER = Ordered.LOWEST_PRECEDENCE - 1000;

	/**
	 * {@code @Order} value of
	 * {@link #propagatingReceiverTracingObservationHandler(Tracer, Propagator)}.
	 */
	public static final int RECEIVER_TRACING_OBSERVATION_HANDLER_ORDER = 1000;

	/**
	 * {@code @Order} value of
	 * {@link #propagatingSenderTracingObservationHandler(Tracer, Propagator)}.
	 */
	public static final int SENDER_TRACING_OBSERVATION_HANDLER_ORDER = 2000;

	/**
     * Creates a new instance of DefaultTracingObservationHandler if no other bean of the same type is present.
     * The order of this bean is set to DEFAULT_TRACING_OBSERVATION_HANDLER_ORDER.
     * 
     * @param tracer the Tracer instance to be used by the DefaultTracingObservationHandler
     * @return a new instance of DefaultTracingObservationHandler
     */
    @Bean
	@ConditionalOnMissingBean
	@Order(DEFAULT_TRACING_OBSERVATION_HANDLER_ORDER)
	public DefaultTracingObservationHandler defaultTracingObservationHandler(Tracer tracer) {
		return new DefaultTracingObservationHandler(tracer);
	}

	/**
     * Creates a {@link PropagatingSenderTracingObservationHandler} bean if no other bean of the same type is present and if a bean of type {@link Propagator} is present.
     * The bean is annotated with {@link ConditionalOnMissingBean} and {@link ConditionalOnBean} to ensure it is only created if the required dependencies are available.
     * The bean is also ordered using the {@link Order} annotation with the value of {@link SENDER_TRACING_OBSERVATION_HANDLER_ORDER}.
     *
     * @param tracer the {@link Tracer} bean used for tracing
     * @param propagator the {@link Propagator} bean used for propagation
     * @return the created {@link PropagatingSenderTracingObservationHandler} bean
     */
    @Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(Propagator.class)
	@Order(SENDER_TRACING_OBSERVATION_HANDLER_ORDER)
	public PropagatingSenderTracingObservationHandler<?> propagatingSenderTracingObservationHandler(Tracer tracer,
			Propagator propagator) {
		return new PropagatingSenderTracingObservationHandler<>(tracer, propagator);
	}

	/**
     * Creates a {@link PropagatingReceiverTracingObservationHandler} bean if no other bean of the same type is present in the application context and if a bean of type {@link Propagator} is present.
     * The bean is annotated with {@link ConditionalOnMissingBean} and {@link ConditionalOnBean} to ensure its creation only when the required conditions are met.
     * The bean is also ordered using the value specified in the constant {@link RECEIVER_TRACING_OBSERVATION_HANDLER_ORDER}.
     * 
     * @param tracer the {@link Tracer} bean used for tracing
     * @param propagator the {@link Propagator} bean used for propagation
     * @return the created {@link PropagatingReceiverTracingObservationHandler} bean
     */
    @Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(Propagator.class)
	@Order(RECEIVER_TRACING_OBSERVATION_HANDLER_ORDER)
	public PropagatingReceiverTracingObservationHandler<?> propagatingReceiverTracingObservationHandler(Tracer tracer,
			Propagator propagator) {
		return new PropagatingReceiverTracingObservationHandler<>(tracer, propagator);
	}

	/**
     * SpanAspectConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Advice.class)
	@ConditionalOnProperty(prefix = "micrometer.observations.annotations", name = "enabled", havingValue = "true")
	static class SpanAspectConfiguration {

		/**
         * Creates a new instance of {@link DefaultNewSpanParser} if no other bean of type {@link NewSpanParser} is present.
         * 
         * @return the created instance of {@link DefaultNewSpanParser}
         */
        @Bean
		@ConditionalOnMissingBean(NewSpanParser.class)
		DefaultNewSpanParser newSpanParser() {
			return new DefaultNewSpanParser();
		}

		/**
         * Creates a new instance of {@link SpanTagAnnotationHandler} if no other bean of the same type is present in the application context.
         * 
         * @param beanFactory the {@link BeanFactory} used to retrieve other beans
         * @return a new instance of {@link SpanTagAnnotationHandler}
         */
        @Bean
		@ConditionalOnMissingBean
		SpanTagAnnotationHandler spanTagAnnotationHandler(BeanFactory beanFactory) {
			ValueExpressionResolver valueExpressionResolver = new SpelTagValueExpressionResolver();
			return new SpanTagAnnotationHandler(beanFactory::getBean, (ignored) -> valueExpressionResolver);
		}

		/**
         * Creates an instance of ImperativeMethodInvocationProcessor if there is no existing bean of type MethodInvocationProcessor.
         * 
         * @param newSpanParser the NewSpanParser bean used for parsing new spans
         * @param tracer the Tracer bean used for tracing
         * @param spanTagAnnotationHandler the SpanTagAnnotationHandler bean used for handling span tag annotations
         * @return an instance of ImperativeMethodInvocationProcessor
         */
        @Bean
		@ConditionalOnMissingBean(MethodInvocationProcessor.class)
		ImperativeMethodInvocationProcessor imperativeMethodInvocationProcessor(NewSpanParser newSpanParser,
				Tracer tracer, SpanTagAnnotationHandler spanTagAnnotationHandler) {
			return new ImperativeMethodInvocationProcessor(newSpanParser, tracer, spanTagAnnotationHandler);
		}

		/**
         * Creates a new instance of {@link SpanAspect} if no other bean of the same type is present.
         * 
         * @param methodInvocationProcessor the {@link MethodInvocationProcessor} to be used by the {@link SpanAspect}
         * @return a new instance of {@link SpanAspect}
         */
        @Bean
		@ConditionalOnMissingBean
		SpanAspect spanAspect(MethodInvocationProcessor methodInvocationProcessor) {
			return new SpanAspect(methodInvocationProcessor);
		}

	}

	/**
     * SpelTagValueExpressionResolver class.
     */
    private static final class SpelTagValueExpressionResolver implements ValueExpressionResolver {

		/**
         * Resolves the given SpEL expression with the provided parameter.
         * 
         * @param expression the SpEL expression to be resolved
         * @param parameter the parameter to be used in the expression evaluation
         * @return the result of the expression evaluation as a String
         * @throws IllegalStateException if unable to evaluate the SpEL expression
         */
        @Override
		public String resolve(String expression, Object parameter) {
			try {
				SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
				ExpressionParser expressionParser = new SpelExpressionParser();
				Expression expressionToEvaluate = expressionParser.parseExpression(expression);
				return expressionToEvaluate.getValue(context, parameter, String.class);
			}
			catch (Exception ex) {
				throw new IllegalStateException("Unable to evaluate SpEL expression '%s'".formatted(expression), ex);
			}
		}

	}

}
