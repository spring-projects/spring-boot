/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.grpcservertest;

import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusException;
import org.jspecify.annotations.Nullable;

import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.stereotype.Component;

@Component
public class StandardExceptionHandler implements GrpcExceptionHandler {

	@Override
	public @Nullable StatusException handleException(Throwable exception) {
		if (exception instanceof IllegalArgumentException) {
			return new StatusException(Status.fromCode(Code.INVALID_ARGUMENT).withDescription(exception.getMessage()));
		}
		if (exception instanceof IllegalStateException) {
			return new StatusException(Status.fromCode(Code.INTERNAL).withDescription(exception.getMessage()));
		}
		return null;
	}

}
