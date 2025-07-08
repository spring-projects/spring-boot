/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.undertow.autoconfigure.servlet;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

import org.springframework.boot.undertow.servlet.UndertowDeploymentInfoCustomizer;
import org.springframework.boot.undertow.servlet.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;

/**
 * WebSocket customizer for {@link UndertowServletWebServerFactory}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class WebSocketUndertowServletWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<UndertowServletWebServerFactory>, Ordered {

	@Override
	public void customize(UndertowServletWebServerFactory factory) {
		WebsocketDeploymentInfoCustomizer customizer = new WebsocketDeploymentInfoCustomizer();
		factory.addDeploymentInfoCustomizers(customizer);
	}

	@Override
	public int getOrder() {
		return 0;
	}

	private static final class WebsocketDeploymentInfoCustomizer implements UndertowDeploymentInfoCustomizer {

		@Override
		public void customize(DeploymentInfo deploymentInfo) {
			WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
			deploymentInfo.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, info);
		}

	}

}
