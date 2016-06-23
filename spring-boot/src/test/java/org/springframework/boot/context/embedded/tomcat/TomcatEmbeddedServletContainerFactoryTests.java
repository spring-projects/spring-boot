/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.embedded.tomcat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Service;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;

import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactoryTests;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.testutil.InternalOutputCapture;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link TomcatEmbeddedServletContainerFactory} and
 * {@link TomcatEmbeddedServletContainer}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class TomcatEmbeddedServletContainerFactoryTests
		extends AbstractEmbeddedServletContainerFactoryTests {

	@Rule
	public InternalOutputCapture outputCapture = new InternalOutputCapture();

	@Override
	protected TomcatEmbeddedServletContainerFactory getFactory() {
		return new TomcatEmbeddedServletContainerFactory(0);
	}

	@After
	public void restoreTccl() {
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
	}

	// JMX MBean names clash if you get more than one Engine with the same name...
	@Test
	public void tomcatEngineNames() throws Exception {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory.getEmbeddedServletContainer();
		factory.setPort(SocketUtils.findAvailableTcpPort(40000));
		TomcatEmbeddedServletContainer container2 = (TomcatEmbeddedServletContainer) factory
				.getEmbeddedServletContainer();

		// Make sure that the names are different
		String firstContainerName = ((TomcatEmbeddedServletContainer) this.container)
				.getTomcat().getEngine().getName();
		String secondContainerName = container2.getTomcat().getEngine().getName();
		assertThat(firstContainerName).as("Tomcat engines must have different names")
				.isNotEqualTo(secondContainerName);
		container2.stop();
	}

	@Test
	public void tomcatListeners() throws Exception {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		LifecycleListener[] listeners = new LifecycleListener[4];
		for (int i = 0; i < listeners.length; i++) {
			listeners[i] = mock(LifecycleListener.class);
		}
		factory.setContextLifecycleListeners(Arrays.asList(listeners[0], listeners[1]));
		factory.addContextLifecycleListeners(listeners[2], listeners[3]);
		this.container = factory.getEmbeddedServletContainer();
		InOrder ordered = inOrder((Object[]) listeners);
		for (LifecycleListener listener : listeners) {
			ordered.verify(listener).lifecycleEvent((LifecycleEvent) anyObject());
		}
	}

	@Test
	public void tomcatCustomizers() throws Exception {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		TomcatContextCustomizer[] listeners = new TomcatContextCustomizer[4];
		for (int i = 0; i < listeners.length; i++) {
			listeners[i] = mock(TomcatContextCustomizer.class);
		}
		factory.setTomcatContextCustomizers(Arrays.asList(listeners[0], listeners[1]));
		factory.addContextCustomizers(listeners[2], listeners[3]);
		this.container = factory.getEmbeddedServletContainer();
		InOrder ordered = inOrder((Object[]) listeners);
		for (TomcatContextCustomizer listener : listeners) {
			ordered.verify(listener).customize((Context) anyObject());
		}
	}

	@Test
	public void tomcatConnectorCustomizers() throws Exception {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		TomcatConnectorCustomizer[] listeners = new TomcatConnectorCustomizer[4];
		for (int i = 0; i < listeners.length; i++) {
			listeners[i] = mock(TomcatConnectorCustomizer.class);
		}
		factory.setTomcatConnectorCustomizers(Arrays.asList(listeners[0], listeners[1]));
		factory.addConnectorCustomizers(listeners[2], listeners[3]);
		this.container = factory.getEmbeddedServletContainer();
		InOrder ordered = inOrder((Object[]) listeners);
		for (TomcatConnectorCustomizer listener : listeners) {
			ordered.verify(listener).customize((Connector) anyObject());
		}
	}

	@Test
	public void tomcatAdditionalConnectors() throws Exception {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		Connector[] listeners = new Connector[4];
		for (int i = 0; i < listeners.length; i++) {
			Connector connector = mock(Connector.class);
			given(connector.getState()).willReturn(LifecycleState.STOPPED);
			listeners[i] = connector;
		}
		factory.addAdditionalTomcatConnectors(listeners);
		this.container = factory.getEmbeddedServletContainer();
		Map<Service, Connector[]> connectors = ((TomcatEmbeddedServletContainer) this.container)
				.getServiceConnectors();
		assertThat(connectors.values().iterator().next().length)
				.isEqualTo(listeners.length + 1);
	}

	@Test
	public void addNullAdditionalConnectorThrows() {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Connectors must not be null");
		factory.addAdditionalTomcatConnectors((Connector[]) null);
	}

	@Test
	public void sessionTimeout() throws Exception {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		factory.setSessionTimeout(10);
		assertTimeout(factory, 1);
	}

	@Test
	public void sessionTimeoutInMins() throws Exception {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		factory.setSessionTimeout(1, TimeUnit.MINUTES);
		assertTimeout(factory, 1);
	}

	@Test
	public void noSessionTimeout() throws Exception {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		factory.setSessionTimeout(0);
		assertTimeout(factory, -1);
	}

	@Test
	public void valve() throws Exception {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		Valve valve = mock(Valve.class);
		factory.addContextValves(valve);
		this.container = factory.getEmbeddedServletContainer();
		verify(valve).setNext(any(Valve.class));
	}

	@Test
	public void setNullTomcatContextCustomizersThrows() {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TomcatContextCustomizers must not be null");
		factory.setTomcatContextCustomizers(null);
	}

	@Test
	public void addNullContextCustomizersThrows() {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TomcatContextCustomizers must not be null");
		factory.addContextCustomizers((TomcatContextCustomizer[]) null);
	}

	@Test
	public void setNullTomcatConnectorCustomizersThrows() {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TomcatConnectorCustomizers must not be null");
		factory.setTomcatConnectorCustomizers(null);
	}

	@Test
	public void addNullConnectorCustomizersThrows() {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TomcatConnectorCustomizers must not be null");
		factory.addConnectorCustomizers((TomcatConnectorCustomizer[]) null);
	}

	@Test
	public void uriEncoding() throws Exception {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		factory.setUriEncoding(Charset.forName("US-ASCII"));
		Tomcat tomcat = getTomcat(factory);
		assertThat(tomcat.getConnector().getURIEncoding()).isEqualTo("US-ASCII");
	}

	@Test
	public void defaultUriEncoding() throws Exception {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		Tomcat tomcat = getTomcat(factory);
		assertThat(tomcat.getConnector().getURIEncoding()).isEqualTo("UTF-8");
	}

	@Test
	public void sslCiphersConfiguration() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setCiphers(new String[] { "ALPHA", "BRAVO", "CHARLIE" });

		TomcatEmbeddedServletContainerFactory factory = getFactory();
		factory.setSsl(ssl);

		Tomcat tomcat = getTomcat(factory);
		Connector connector = tomcat.getConnector();

		SSLHostConfig[] sslHostConfigs = connector.getProtocolHandler()
				.findSslHostConfigs();
		assertThat(sslHostConfigs[0].getCiphers()).isEqualTo("ALPHA:BRAVO:CHARLIE");
	}

	@Test
	public void sslEnabledMultipleProtocolsConfiguration() throws Exception {
		Ssl ssl = getSsl(null, "password", "src/test/resources/test.jks");
		ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
		ssl.setCiphers(new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "BRAVO" });

		TomcatEmbeddedServletContainerFactory factory = getFactory();
		factory.setSsl(ssl);

		this.container = factory
				.getEmbeddedServletContainer(sessionServletRegistration());
		this.container.start();
		Tomcat tomcat = ((TomcatEmbeddedServletContainer) this.container).getTomcat();
		Connector connector = tomcat.getConnector();

		SSLHostConfig sslHostConfig = connector.getProtocolHandler()
				.findSslHostConfigs()[0];
		assertThat(sslHostConfig.getSslProtocol()).isEqualTo("TLS");
		assertThat(sslHostConfig.getEnabledProtocols())
				.containsExactlyInAnyOrder("TLSv1.1", "TLSv1.2");
	}

	@Test
	public void sslEnabledProtocolsConfiguration() throws Exception {
		Ssl ssl = getSsl(null, "password", "src/test/resources/test.jks");
		ssl.setEnabledProtocols(new String[] { "TLSv1.2" });
		ssl.setCiphers(new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "BRAVO" });

		TomcatEmbeddedServletContainerFactory factory = getFactory();
		factory.setSsl(ssl);

		this.container = factory
				.getEmbeddedServletContainer(sessionServletRegistration());
		Tomcat tomcat = ((TomcatEmbeddedServletContainer) this.container).getTomcat();
		Connector connector = tomcat.getConnector();

		this.container.start();
		SSLHostConfig sslHostConfig = connector.getProtocolHandler()
				.findSslHostConfigs()[0];
		assertThat(sslHostConfig.getSslProtocol()).isEqualTo("TLS");
		assertThat(sslHostConfig.getEnabledProtocols()).containsExactly("TLSv1.2");
	}

	@Test
	public void primaryConnectorPortClashThrowsIllegalStateException()
			throws InterruptedException, IOException {
		doWithBlockedPort(new BlockedPortAction() {

			@Override
			public void run(int port) {
				TomcatEmbeddedServletContainerFactory factory = getFactory();
				factory.setPort(port);

				try {
					TomcatEmbeddedServletContainerFactoryTests.this.container = factory
							.getEmbeddedServletContainer();
					TomcatEmbeddedServletContainerFactoryTests.this.container.start();
					fail();
				}
				catch (EmbeddedServletContainerException ex) {
					// Ignore
				}
			}

		});
	}

	@Test
	public void startupFailureDoesNotResultInUnstoppedThreadsBeingReported()
			throws IOException {
		super.portClashOfPrimaryConnectorResultsInPortInUseException();
		String string = this.outputCapture.toString();
		assertThat(string)
				.doesNotContain("appears to have started a thread named [main]");
	}

	@Override
	protected void addConnector(int port,
			AbstractEmbeddedServletContainerFactory factory) {
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		connector.setPort(port);
		((TomcatEmbeddedServletContainerFactory) factory)
				.addAdditionalTomcatConnectors(connector);
	}

	@Test
	public void jspServletInitParameters() {
		Map<String, String> initParameters = new HashMap<String, String>();
		initParameters.put("a", "alpha");
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		factory.getJspServlet().setInitParameters(initParameters);
		this.container = factory.getEmbeddedServletContainer();
		Wrapper jspServlet = getJspServlet();
		assertThat(jspServlet.findInitParameter("a")).isEqualTo("alpha");
	}

	@Test
	public void useForwardHeaders() throws Exception {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		factory.addContextValves(new RemoteIpValve());
		assertForwardHeaderIsUsed(factory);
	}

	@Test
	public void disableDoesNotSaveSessionFiles() throws Exception {
		File baseDir = this.temporaryFolder.newFolder();
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		// If baseDir is not set SESSIONS.ser is written to a different temp directory
		// each time. By setting it we can really ensure that data isn't saved
		factory.setBaseDirectory(baseDir);
		this.container = factory
				.getEmbeddedServletContainer(sessionServletRegistration());
		this.container.start();
		String s1 = getResponse(getLocalUrl("/session"));
		String s2 = getResponse(getLocalUrl("/session"));
		this.container.stop();
		this.container = factory
				.getEmbeddedServletContainer(sessionServletRegistration());
		this.container.start();
		String s3 = getResponse(getLocalUrl("/session"));
		System.out.println(s1);
		System.out.println(s2);
		System.out.println(s3);
		String message = "Session error s1=" + s1 + " s2=" + s2 + " s3=" + s3;
		assertThat(s2.split(":")[0]).as(message).isEqualTo(s1.split(":")[1]);
		assertThat(s3.split(":")[0]).as(message).isNotEqualTo(s2.split(":")[1]);
	}

	@Test
	public void jndiLookupsCanBePerformedDuringApplicationContextRefresh()
			throws NamingException {
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory(
				0) {

			@Override
			protected TomcatEmbeddedServletContainer getTomcatEmbeddedServletContainer(
					Tomcat tomcat) {
				tomcat.enableNaming();
				return super.getTomcatEmbeddedServletContainer(tomcat);
			}

		};

		// Container is created in onRefresh
		this.container = factory.getEmbeddedServletContainer();

		// Lookups should now be possible
		new InitialContext().lookup("java:comp/env");

		// Called in finishRefresh, giving us an opportunity to remove the context binding
		// and avoid a leak
		this.container.start();

		// Lookups should no longer be possible
		this.thrown.expect(NamingException.class);
		new InitialContext().lookup("java:comp/env");
	}

	@Override
	protected Wrapper getJspServlet() {
		Container context = ((TomcatEmbeddedServletContainer) this.container).getTomcat()
				.getHost().findChildren()[0];
		return (Wrapper) context.findChild("jsp");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, String> getActualMimeMappings() {
		Context context = (Context) ((TomcatEmbeddedServletContainer) this.container)
				.getTomcat().getHost().findChildren()[0];
		return (Map<String, String>) ReflectionTestUtils.getField(context,
				"mimeMappings");
	}

	private void assertTimeout(TomcatEmbeddedServletContainerFactory factory,
			int expected) {
		Tomcat tomcat = getTomcat(factory);
		Context context = (Context) tomcat.getHost().findChildren()[0];
		assertThat(context.getSessionTimeout()).isEqualTo(expected);
	}

	private Tomcat getTomcat(TomcatEmbeddedServletContainerFactory factory) {
		this.container = factory.getEmbeddedServletContainer();
		return ((TomcatEmbeddedServletContainer) this.container).getTomcat();
	}

}
