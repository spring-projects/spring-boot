/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.PathMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * {@link EndpointDiscoverer} for {@link ExposableControllerEndpoint controller
 * endpoints}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @deprecated since 3.3.0 in favor of {@code @Endpoint} and {@code @WebEndpoint} support
 */
@ImportRuntimeHints(ControllerEndpointDiscoverer.ControllerEndpointDiscovererRuntimeHints.class)
@Deprecated(since = "3.3.0", forRemoval = true)
@SuppressWarnings("removal")
public class ControllerEndpointDiscoverer extends EndpointDiscoverer<ExposableControllerEndpoint, Operation>
		implements ControllerEndpointsSupplier {

	private final @Nullable List<PathMapper> endpointPathMappers;

	/**
	 * Create a new {@link ControllerEndpointDiscoverer} instance.
	 * @param applicationContext the source application context
	 * @param endpointPathMappers the endpoint path mappers
	 * @param filters filters to apply
	 */
	public ControllerEndpointDiscoverer(ApplicationContext applicationContext,
			@Nullable List<PathMapper> endpointPathMappers,
			Collection<EndpointFilter<ExposableControllerEndpoint>> filters) {
		super(applicationContext, ParameterValueMapper.NONE, Collections.emptyList(), filters, Collections.emptyList());
		this.endpointPathMappers = endpointPathMappers;
	}

	@Override
	protected boolean isEndpointTypeExposed(Class<?> beanType) {
		MergedAnnotations annotations = MergedAnnotations.from(beanType, SearchStrategy.SUPERCLASS);
		return annotations.isPresent(ControllerEndpoint.class) || annotations.isPresent(RestControllerEndpoint.class);
	}

	@Override
	protected ExposableControllerEndpoint createEndpoint(Object endpointBean, EndpointId id, Access defaultAccess,
			Collection<Operation> operations) {
		String rootPath = PathMapper.getRootPath(this.endpointPathMappers, id);
		return new DiscoveredControllerEndpoint(this, endpointBean, id, rootPath, defaultAccess);
	}

	@Override
	protected Operation createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
			OperationInvoker invoker) {
		throw new IllegalStateException("ControllerEndpoints must not declare operations");
	}

	@Override
	protected OperationKey createOperationKey(Operation operation) {
		throw new IllegalStateException("ControllerEndpoints must not declare operations");
	}

	@Override
	protected boolean isInvocable(ExposableControllerEndpoint endpoint) {
		return true;
	}

	static class ControllerEndpointDiscovererRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.reflection()
				.registerType(ControllerEndpointFilter.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		}

	}

}
