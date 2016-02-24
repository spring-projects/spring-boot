/*
 * Copyright 2016-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.grpc.server;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Service;

/**
 * Use this to annotate a gRPC service implementation.
 * @author Ray Tsang
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface GrpcService {
	/**
	 * The wrapper class that gRPC generator generates. For example, given a Greeter
	 * service: <pre>
	 * service Greeter {
	 *   rpc SayHello (HelloRequest) returns (HelloResponse) {}
	 * }
	 * </pre> The wrapper class is <code>GreeterRpc</code>, which encapsulates an inner
	 * interface <code>Greeter</code>. The implementation class of <code>Greeter</code>
	 * interface should then be annotated like this: <pre>
	 * &#64;GrpcService(GreeterRpc.class)
	 * public class GreeterImpl implements Greeter {
	 *   ...
	 * }
	 * </pre>
	 */
	Class<?> value();
}
