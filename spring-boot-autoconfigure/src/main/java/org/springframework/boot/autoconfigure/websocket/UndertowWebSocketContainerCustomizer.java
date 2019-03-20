/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.websocket;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

import org.springframework.boot.context.embedded.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;

/**
 * {@link WebSocketContainerCustomizer} for
 * {@link UndertowEmbeddedServletContainerFactory}.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
public class UndertowWebSocketContainerCustomizer
		extends WebSocketContainerCustomizer<UndertowEmbeddedServletContainerFactory> {

	@Override
	protected void doCustomize(UndertowEmbeddedServletContainerFactory container) {
		WebsocketDeploymentInfoCustomizer customizer = new WebsocketDeploymentInfoCustomizer();
		container.addDeploymentInfoCustomizers(customizer);
	}

	private static class WebsocketDeploymentInfoCustomizer
			implements UndertowDeploymentInfoCustomizer {

		@Override
		public void customize(DeploymentInfo deploymentInfo) {
			WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
			deploymentInfo.addServletContextAttribute(
					WebSocketDeploymentInfo.ATTRIBUTE_NAME, info);
		}

	}

}
