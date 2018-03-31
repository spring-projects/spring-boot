/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.webservices.client;

import java.lang.reflect.Field;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.xml.transform.TransformerFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

/**
 * Builder that can be used to configure and create a
 * {@link org.springframework.ws.client.core.WebServiceTemplate}. By default the built
 * {@link org.springframework.ws.client.core.WebServiceTemplate} will attempt to use the
 * most suitable {@link org.springframework.ws.transport.WebServiceMessageSender}, call
 * {@link #detectWebServiceMessageSender(boolean) detectWebServiceMessageSender(false)} if
 * you prefer to keep the default. In a typical auto-configured Spring Boot application
 * this builder is available as a bean and can be injected whenever a
 * {@link WebServiceTemplate} is needed.
 *
 * @author Dmytro Nosan
 */
public class WebServiceTemplateBuilder {

	private static final Map<String, Class<? extends WebServiceMessageSenderFactory>> MESSAGE_SENDER_FACTORY_CLASSES;

	static {
		Map<String, Class<? extends WebServiceMessageSenderFactory>> candidates = new LinkedHashMap<>();
		candidates.put("org.apache.http.client.HttpClient",
				HttpComponentsMessageSenderFactory.class);
		candidates.put("org.springframework.http.client.ClientHttpRequestFactory",
				ClientHttpRequestMessageSenderFactory.class);
		MESSAGE_SENDER_FACTORY_CLASSES = Collections.unmodifiableMap(candidates);
	}

	private final Set<ClientInterceptor> interceptors;

	private final Set<WebServiceTemplateCustomizer> internalCustomizers;

	private final Set<WebServiceTemplateCustomizer> customizers;

	private final Set<Supplier<? extends WebServiceMessageSender>> webServiceMessageSenderSuppliers;

	private final Set<WebServiceMessageSenderCustomizer> webServiceMessageSenderCustomizers;

	private final Marshaller marshaller;

	private final Unmarshaller unmarshaller;

	private final DestinationProvider destinationProvider;

	private final Class<? extends TransformerFactory> transformerFactoryClass;

	private final WebServiceMessageFactory messageFactory;

	private final boolean detectWebServiceMessageSender;

	public WebServiceTemplateBuilder(WebServiceTemplateCustomizer... customizers) {
		this(Collections.emptySet(), Collections.emptySet(),
				append(Collections.<WebServiceTemplateCustomizer>emptySet(), customizers),
				Collections.emptySet(), Collections.emptySet(), null, null, null, null,
				null, true);
	}

	private WebServiceTemplateBuilder(Set<ClientInterceptor> interceptors,
			Set<WebServiceTemplateCustomizer> internalCustomizers,
			Set<WebServiceTemplateCustomizer> customizers,
			Set<Supplier<? extends WebServiceMessageSender>> webServiceMessageSenderSuppliers,
			Set<WebServiceMessageSenderCustomizer> webServiceMessageSenderCustomizers,
			Marshaller marshaller, Unmarshaller unmarshaller,
			DestinationProvider destinationProvider,
			Class<? extends TransformerFactory> transformerFactoryClass,
			WebServiceMessageFactory messageFactory,
			boolean detectWebServiceMessageSender) {
		this.interceptors = interceptors;
		this.internalCustomizers = internalCustomizers;
		this.customizers = customizers;
		this.webServiceMessageSenderSuppliers = webServiceMessageSenderSuppliers;
		this.webServiceMessageSenderCustomizers = webServiceMessageSenderCustomizers;
		this.marshaller = marshaller;
		this.unmarshaller = unmarshaller;
		this.destinationProvider = destinationProvider;
		this.transformerFactoryClass = transformerFactoryClass;
		this.messageFactory = messageFactory;
		this.detectWebServiceMessageSender = detectWebServiceMessageSender;
	}

	/**
	 * Set {@link ClientInterceptor ClientInterceptors} that should be used with the
	 * {@link WebServiceTemplate}. Interceptors are applied in the order that they were
	 * added after builder configuration has been applied.
	 *
	 * <b>Note!</b> Override existing interceptors
	 * @param interceptors the interceptors to set
	 * @return a new builder instance
	 * @see WebServiceTemplate#setInterceptors(ClientInterceptor[])
	 */
	public WebServiceTemplateBuilder setInterceptors(ClientInterceptor... interceptors) {
		Assert.notNull(interceptors, "interceptors must not be null");
		return setInterceptors(Arrays.asList(interceptors));
	}

	/**
	 * Set {@link ClientInterceptor ClientInterceptors} that should be used with the
	 * {@link WebServiceTemplate}. Interceptors are applied in the order that they were
	 * added after builder configuration has been applied.
	 *
	 * <b>Note!</b> Override existing interceptors
	 * @param interceptors the interceptors to set
	 * @return a new builder instance
	 * @see WebServiceTemplate#setInterceptors(ClientInterceptor[])
	 */
	public WebServiceTemplateBuilder setInterceptors(
			Collection<? extends ClientInterceptor> interceptors) {
		Assert.notNull(interceptors, "interceptors must not be null");
		return new WebServiceTemplateBuilder(
				append(Collections.<ClientInterceptor>emptySet(), interceptors),
				this.internalCustomizers, this.customizers,
				this.webServiceMessageSenderSuppliers,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Add additional {@link ClientInterceptor ClientInterceptors} that should be used
	 * with the {@link WebServiceTemplate}. Interceptors are applied in the order that
	 * they were added after builder configuration has been applied.
	 * @param interceptors the interceptors to add
	 * @return a new builder instance
	 * @see WebServiceTemplate#setInterceptors(ClientInterceptor[])
	 */
	public WebServiceTemplateBuilder addInterceptors(ClientInterceptor... interceptors) {
		Assert.notNull(interceptors, "interceptors must not be null");
		return addInterceptors(Arrays.asList(interceptors));
	}

	/**
	 * Add additional {@link ClientInterceptor ClientInterceptors} that should be used
	 * with the {@link WebServiceTemplate}. Interceptors are applied in the order that
	 * they were added after builder configuration has been applied.
	 * @param interceptors the interceptors to add
	 * @return a new builder instance
	 * @see WebServiceTemplate#setInterceptors(ClientInterceptor[])
	 */
	public WebServiceTemplateBuilder addInterceptors(
			Collection<? extends ClientInterceptor> interceptors) {
		Assert.notNull(interceptors, "interceptors must not be null");
		return new WebServiceTemplateBuilder(append(this.interceptors, interceptors),
				this.internalCustomizers, this.customizers,
				this.webServiceMessageSenderSuppliers,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Set {@link WebServiceTemplateCustomizer WebServiceTemplateCustomizers} that should
	 * be applied to the {@link WebServiceTemplate}. Customizers are applied in the order
	 * that they were added after builder configuration has been applied.
	 *
	 * <b>Note!</b> Override existing customizers
	 * @param customizers the customizers to set
	 * @return a new builder instance
	 */

	public WebServiceTemplateBuilder setCustomizers(
			Collection<? extends WebServiceTemplateCustomizer> customizers) {
		Assert.notNull(customizers, "customizers must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				append(Collections.<WebServiceTemplateCustomizer>emptySet(), customizers),
				this.webServiceMessageSenderSuppliers,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Set {@link WebServiceTemplateCustomizer WebServiceTemplateCustomizers} that should
	 * be applied to the {@link WebServiceTemplate}. Customizers are applied in the order
	 * that they were added after builder configuration has been applied.
	 *
	 * <b>Note!</b> Override existing customizers
	 * @param customizers the customizers to set
	 * @return a new builder instance
	 */
	public WebServiceTemplateBuilder setCustomizers(
			WebServiceTemplateCustomizer... customizers) {
		Assert.notNull(customizers, "customizers must not be null");
		return setCustomizers(Arrays.asList(customizers));
	}

	/**
	 * Add additional {@link WebServiceTemplateCustomizer WebServiceTemplateCustomizers}
	 * that should be applied to the {@link WebServiceTemplate}. Customizers are applied
	 * in the order that they were added after builder configuration has been applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 */
	public WebServiceTemplateBuilder addCustomizers(
			WebServiceTemplateCustomizer... customizers) {
		Assert.notNull(customizers, "customizers must not be null");
		return addCustomizers(Arrays.asList(customizers));
	}

	/**
	 * Add additional {@link WebServiceTemplateCustomizer WebServiceTemplateCustomizers}
	 * that should be applied to the {@link WebServiceTemplate}. Customizers are applied
	 * in the order that they were added after builder configuration has been applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 */

	public WebServiceTemplateBuilder addCustomizers(
			Collection<? extends WebServiceTemplateCustomizer> customizers) {
		Assert.notNull(customizers, "customizers must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				append(this.customizers, customizers),
				this.webServiceMessageSenderSuppliers,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Sets the {@code Suppliers} of {@link WebServiceMessageSender} that should be called
	 * each time when {@link #configure(WebServiceTemplate)} method is called.
	 *
	 * <b>Note!</b> Override existing WebServiceMessageSender {@code suppliers}
	 * @param webServiceMessageSenderSuppliers Suppliers for the messageSenders
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setMessageSenders(WebServiceMessageSender[])
	 */

	public WebServiceTemplateBuilder setWebServiceMessageSenders(
			Collection<? extends Supplier<? extends WebServiceMessageSender>> webServiceMessageSenderSuppliers) {
		Assert.notNull(webServiceMessageSenderSuppliers,
				"webServiceMessageSenderSuppliers must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers,
				append(Collections
						.<Supplier<? extends WebServiceMessageSender>>emptySet(),
						webServiceMessageSenderSuppliers),
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Add additional {@code Suppliers} of {@link WebServiceMessageSender} that should be
	 * called each time when {@link #configure(WebServiceTemplate)} method is called.
	 * @param webServiceMessageSenderSuppliers Suppliers for the messageSenders
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setMessageSenders(WebServiceMessageSender[])
	 */
	public WebServiceTemplateBuilder addWebServiceMessageSenders(
			Collection<? extends Supplier<? extends WebServiceMessageSender>> webServiceMessageSenderSuppliers) {
		Assert.notNull(webServiceMessageSenderSuppliers,
				"webServiceMessageSenderSuppliers must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers,
				append(this.webServiceMessageSenderSuppliers,
						webServiceMessageSenderSuppliers),
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Set {@link WebServiceTemplate#setCheckConnectionForFault(boolean)
	 * setCheckConnectionForFault} on the underlying.
	 * @param checkConnectionForFault Specify whether checkConnectionForFault should be
	 * enabled or not.
	 * @return a new builder instance.
	 **/
	public WebServiceTemplateBuilder setCheckConnectionForFault(
			boolean checkConnectionForFault) {
		return new WebServiceTemplateBuilder(this.interceptors,
				append(this.internalCustomizers,
						new CheckConnectionFaultCustomizer(checkConnectionForFault)),
				this.customizers, this.webServiceMessageSenderSuppliers,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Set {@link WebServiceTemplate#setCheckConnectionForError(boolean)
	 * setCheckConnectionForError} on the underlying.
	 * @param checkConnectionForError Specify whether checkConnectionForError should be
	 * enabled or not.
	 * @return a new builder instance.
	 **/

	public WebServiceTemplateBuilder setCheckConnectionForError(
			boolean checkConnectionForError) {
		return new WebServiceTemplateBuilder(this.interceptors,
				append(this.internalCustomizers,
						new CheckConnectionForErrorCustomizer(checkConnectionForError)),
				this.customizers, this.webServiceMessageSenderSuppliers,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Sets the {@code Supplier} of {@link WebServiceMessageSender} that should be called
	 * each time when {@link #configure(WebServiceTemplate)} method is called.
	 *
	 * <b>Note!</b> Override existing WebServiceMessageSender {@code suppliers}
	 * @param webServiceMessageSenderSupplier Supplier for the messageSender
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setMessageSenders(WebServiceMessageSender[])
	 * @see #setWebServiceMessageSenders(Collection)
	 */

	public WebServiceTemplateBuilder setWebServiceMessageSender(
			Supplier<? extends WebServiceMessageSender> webServiceMessageSenderSupplier) {
		Assert.notNull(webServiceMessageSenderSupplier,
				"webServiceMessageSenderSupplier must not be null");
		return setWebServiceMessageSenders(
				Collections.singleton(webServiceMessageSenderSupplier));
	}

	/**
	 * Add additional {@code Supplier} of {@link WebServiceMessageSender} that should be
	 * called each time when {@link #configure(WebServiceTemplate)} method is called.
	 * @param webServiceMessageSenderSupplier Supplier for the messageSender
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setMessageSenders(WebServiceMessageSender[])
	 * @see #addWebServiceMessageSenders(Collection)
	 */
	public WebServiceTemplateBuilder addWebServiceMessageSender(
			Supplier<? extends WebServiceMessageSender> webServiceMessageSenderSupplier) {
		Assert.notNull(webServiceMessageSenderSupplier,
				"webServiceMessageSenderSupplier must not be null");
		return addWebServiceMessageSenders(
				Collections.singleton(webServiceMessageSenderSupplier));
	}

	/**
	 * Sets the {@code Class} of {@link WebServiceMessageSender} that should be created
	 * each time when {@link #configure(WebServiceTemplate)} method is called.
	 *
	 * <b>Note!</b> Override existing WebServiceMessageSender {@code suppliers}
	 * @param webServiceMessageSenderClass {@code Class} of
	 * {@link WebServiceMessageSender}
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setMessageSenders(WebServiceMessageSender[])
	 * @see #setWebServiceMessageSender(Supplier)
	 * @see BeanUtils#instantiateClass(Class)
	 */

	public WebServiceTemplateBuilder setWebServiceMessageSender(
			Class<? extends WebServiceMessageSender> webServiceMessageSenderClass) {
		Assert.notNull(webServiceMessageSenderClass,
				"webServiceMessageSenderClass must not be null");
		return setWebServiceMessageSender(
				supplier(webServiceMessageSenderClass, BeanUtils::instantiateClass));
	}

	/**
	 * Add additional {@code Class} of {@link WebServiceMessageSender} that should be
	 * created each time when {@link #configure(WebServiceTemplate)} method is called.
	 * @param webServiceMessageSenderClass {@code Class} of
	 * {@link WebServiceMessageSender}
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setMessageSenders(WebServiceMessageSender[])
	 * @see #addWebServiceMessageSender(Supplier)
	 * @see BeanUtils#instantiateClass(Class)
	 */
	public WebServiceTemplateBuilder addWebServiceMessageSender(
			Class<? extends WebServiceMessageSender> webServiceMessageSenderClass) {
		Assert.notNull(webServiceMessageSenderClass,
				"webServiceMessageSenderClass must not be null");
		return addWebServiceMessageSender(
				supplier(webServiceMessageSenderClass, BeanUtils::instantiateClass));
	}

	/**
	 * Sets the message factory used for creating messages.
	 * @param messageFactory instance of WebServiceMessageFactory
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setMessageFactory(WebServiceMessageFactory)
	 **/

	public WebServiceTemplateBuilder setWebServiceMessageFactory(
			WebServiceMessageFactory messageFactory) {
		Assert.notNull(messageFactory, "messageFactory must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, this.webServiceMessageSenderSuppliers,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Set {@link WebServiceTemplate#setUnmarshaller(Unmarshaller) unmarshaller} on the
	 * underlying.
	 * @param unmarshaller message unmarshaller
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setUnmarshaller(Unmarshaller)
	 **/
	public WebServiceTemplateBuilder setUnmarshaller(Unmarshaller unmarshaller) {
		Assert.notNull(unmarshaller, "unmarshaller must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, this.webServiceMessageSenderSuppliers,
				this.webServiceMessageSenderCustomizers, this.marshaller, unmarshaller,
				this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Set {@link WebServiceTemplate#setMarshaller(Marshaller) marshaller} on the
	 * underlying.
	 * @param marshaller message marshaller
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setMarshaller(Marshaller)
	 **/
	public WebServiceTemplateBuilder setMarshaller(Marshaller marshaller) {
		Assert.notNull(marshaller, "marshaller must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, this.webServiceMessageSenderSuppliers,
				this.webServiceMessageSenderCustomizers, marshaller, this.unmarshaller,
				this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Sets the connection timeout in milliseconds on the underlying.
	 * @param connectionTimeout the connection timeout in milliseconds
	 * @return a new builder instance.
	 * @throws java.lang.IllegalStateException if the underlying source doesn't support a
	 * connection timeout.
	 */
	public WebServiceTemplateBuilder setConnectionTimeout(int connectionTimeout) {
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, this.webServiceMessageSenderSuppliers,
				append(this.webServiceMessageSenderCustomizers,
						new ConnectionTimeoutWebServiceMessageSenderCustomizer(
								connectionTimeout)),
				this.marshaller, this.unmarshaller, this.destinationProvider,
				this.transformerFactoryClass, this.messageFactory,
				this.detectWebServiceMessageSender);
	}

	/**
	 * Sets the read timeout in milliseconds on the underlying.
	 * @param readTimeout the read timeout in milliseconds
	 * @return a new builder instance.
	 * @throws java.lang.IllegalStateException if the underlying source doesn't support a
	 * read timeout.
	 */
	public WebServiceTemplateBuilder setReadTimeout(int readTimeout) {
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, this.webServiceMessageSenderSuppliers,
				append(this.webServiceMessageSenderCustomizers,
						new ReadTimeoutWebServiceMessageSenderCustomizer(readTimeout)),
				this.marshaller, this.unmarshaller, this.destinationProvider,
				this.transformerFactoryClass, this.messageFactory,
				this.detectWebServiceMessageSender);
	}

	/**
	 * Set {@link WebServiceTemplate#setFaultMessageResolver(FaultMessageResolver)
	 * faultMessageResolver} on the underlying.
	 * @param faultMessageResolver faultMessageResolver may be set to null to disable
	 * fault handling.
	 * @return a new builder instance.
	 **/
	public WebServiceTemplateBuilder setFaultMessageResolver(
			FaultMessageResolver faultMessageResolver) {
		return new WebServiceTemplateBuilder(this.interceptors,
				append(this.internalCustomizers,
						new FaultMessageResolverCustomizer(faultMessageResolver)),
				this.customizers, this.webServiceMessageSenderSuppliers,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Set {@link WebServiceTemplate#setTransformerFactoryClass(Class)
	 * setTransformerFactoryClass} on the underlying.
	 * @param transformerFactoryClass boolean value
	 * @return a new builder instance.
	 **/

	public WebServiceTemplateBuilder setTransformerFactoryClass(
			Class<? extends TransformerFactory> transformerFactoryClass) {
		Assert.notNull(transformerFactoryClass,
				"transformerFactoryClass must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, this.webServiceMessageSenderSuppliers,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Set the default URI to be used on operations that do not have a URI parameter.
	 *
	 * <b>Note!</b>Typically, either this property is set, or
	 * {@link #setDestinationProvider(DestinationProvider)}, but not both.
	 * @param defaultUri the destination provider URI to be used on operations that do not
	 * have a URI parameter.
	 * @return a new builder instance.
	 */
	public WebServiceTemplateBuilder setDefaultUri(String defaultUri) {
		Assert.hasText(defaultUri, "defaultUri must not be empty");
		return setDestinationProvider(() -> URI.create(defaultUri));
	}

	/**
	 * Set {@link WebServiceTemplate#setDestinationProvider(DestinationProvider)
	 * destinationProvider} on the underlying.
	 *
	 * <b>Note!</b>Typically, either this property is set, or
	 * {@link #setDefaultUri(String)}, but not both.
	 * @param destinationProvider the destination provider URI to be used on operations
	 * that do not have a URI parameter.
	 * @return a new builder instance.
	 */
	public WebServiceTemplateBuilder setDestinationProvider(
			DestinationProvider destinationProvider) {
		Assert.notNull(destinationProvider, "destinationProvider must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, this.webServiceMessageSenderSuppliers,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Set if the {@link WebServiceMessageSender} should be detected based on the
	 * classpath. Default is {@code true}.
	 * @param detectWebServiceMessageSender if the {@link WebServiceMessageSender} should
	 * be detected
	 * @return a new builder instance
	 */
	public WebServiceTemplateBuilder detectWebServiceMessageSender(
			boolean detectWebServiceMessageSender) {
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, this.webServiceMessageSenderSuppliers,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, detectWebServiceMessageSender);
	}

	/**
	 * Build a new {@link WebServiceTemplate} instance and configure it using this
	 * builder.
	 * @return a configured {@link WebServiceTemplate} instance.
	 * @see #build(Class)
	 * @see #configure(WebServiceTemplate)
	 */
	public WebServiceTemplate build() {
		return build(WebServiceTemplate.class);
	}

	/**
	 * Build a new {@link WebServiceTemplate} instance of the specified type and configure
	 * it using this builder.
	 * @param <T> the type of web service template
	 * @param webServiceTemplateClass the template type to create
	 * @return a configured {@link WebServiceTemplate} instance.
	 * @see WebServiceTemplateBuilder#build()
	 * @see #configure(WebServiceTemplate)
	 */

	public <T extends WebServiceTemplate> T build(Class<T> webServiceTemplateClass) {
		Assert.notNull(webServiceTemplateClass,
				"webServiceTemplateClass must not be null");
		return configure(BeanUtils.instantiateClass(webServiceTemplateClass));
	}

	/**
	 * Configure the provided {@link WebServiceTemplate} instance using this builder.
	 * @param <T> the type of web service template
	 * @param webServiceTemplate the {@link WebServiceTemplate} to configure
	 * @return the web service template instance
	 * @see #build()
	 * @see #build(Class)
	 */
	public <T extends WebServiceTemplate> T configure(T webServiceTemplate) {
		Assert.notNull(webServiceTemplate, "webServiceTemplate must not be null");

		configureSenders(webServiceTemplate);

		if (!CollectionUtils.isEmpty(this.internalCustomizers)) {
			for (WebServiceTemplateCustomizer internalCustomizer : this.internalCustomizers) {
				internalCustomizer.customize(webServiceTemplate);
			}
		}

		if (this.marshaller != null) {
			webServiceTemplate.setMarshaller(this.marshaller);
		}

		if (this.unmarshaller != null) {
			webServiceTemplate.setUnmarshaller(this.unmarshaller);
		}

		if (this.destinationProvider != null) {
			webServiceTemplate.setDestinationProvider(this.destinationProvider);
		}

		if (this.transformerFactoryClass != null) {
			webServiceTemplate.setTransformerFactoryClass(this.transformerFactoryClass);
		}

		if (this.messageFactory != null) {
			webServiceTemplate.setMessageFactory(this.messageFactory);
		}

		if (!CollectionUtils.isEmpty(this.customizers)) {
			for (WebServiceTemplateCustomizer customizer : this.customizers) {
				customizer.customize(webServiceTemplate);
			}
		}

		if (!CollectionUtils.isEmpty(this.interceptors)) {
			webServiceTemplate.setInterceptors(
					append(this.interceptors, webServiceTemplate.getInterceptors())
							.toArray(new ClientInterceptor[0]));
		}

		return webServiceTemplate;
	}

	private <T extends WebServiceTemplate> void configureSenders(T webServiceTemplate) {

		if (!CollectionUtils.isEmpty(this.webServiceMessageSenderSuppliers)) {
			Set<WebServiceMessageSender> webServiceMessageSenders = new LinkedHashSet<>();
			for (Supplier<? extends WebServiceMessageSender> webServiceMessageSenderSupplier : this.webServiceMessageSenderSuppliers) {
				webServiceMessageSenders.add(webServiceMessageSenderSupplier.get());
			}
			webServiceTemplate.setMessageSenders(
					webServiceMessageSenders.toArray(new WebServiceMessageSender[0]));
		}
		else if (this.detectWebServiceMessageSender) {
			webServiceTemplate.setMessageSenders(
					new WebServiceMessageSender[] { detectMessageSender() });
		}

		if (!CollectionUtils.isEmpty(this.webServiceMessageSenderCustomizers)) {
			if (!ObjectUtils.isEmpty(webServiceTemplate.getMessageSenders())) {
				for (WebServiceMessageSender webServiceMessageSender : webServiceTemplate
						.getMessageSenders()) {
					for (WebServiceMessageSenderCustomizer webServiceMessageSenderCustomizer : this.webServiceMessageSenderCustomizers) {
						webServiceMessageSenderCustomizer
								.customize(webServiceMessageSender);
					}
				}
			}
		}
	}

	private WebServiceMessageSender detectMessageSender() {
		ClassLoader classLoader = getClass().getClassLoader();
		for (Map.Entry<String, Class<? extends WebServiceMessageSenderFactory>> candidate : MESSAGE_SENDER_FACTORY_CLASSES
				.entrySet()) {
			if (ClassUtils.isPresent(candidate.getKey(), classLoader)) {
				WebServiceMessageSenderFactory webServiceMessageSenderFactory = BeanUtils
						.instantiateClass(candidate.getValue());
				Optional<WebServiceMessageSender> webServiceMessageSender = webServiceMessageSenderFactory
						.create();
				if (webServiceMessageSender.isPresent()) {
					return webServiceMessageSender.get();
				}
			}
		}
		return new HttpUrlConnectionMessageSender();
	}

	private static <T, F> Supplier<F> supplier(T value, Function<T, F> mapper) {
		return () -> mapper.apply(value);
	}

	private static <T> Set<T> append(Set<T> set, T[] additions) {
		return append(set, additions != null
				? new LinkedHashSet<>(Arrays.asList(additions)) : Collections.emptySet());
	}

	private static <T> Set<T> append(Set<T> set, T addition) {
		Set<T> result = new LinkedHashSet<>(set != null ? set : Collections.emptySet());
		result.add(addition);
		return Collections.unmodifiableSet(result);
	}

	private static <T> Set<T> append(Set<T> set, Collection<? extends T> additions) {
		Set<T> result = new LinkedHashSet<>(set != null ? set : Collections.emptySet());
		result.addAll(additions != null ? additions : Collections.emptyList());
		return Collections.unmodifiableSet(result);
	}

	private interface WebServiceMessageSenderFactory {

		Optional<WebServiceMessageSender> create();

	}

	private interface WebServiceMessageSenderCustomizer {

		void customize(WebServiceMessageSender webServiceMessageSender);

	}

	private static final class ClientHttpRequestMessageSenderFactory
			implements WebServiceMessageSenderFactory {

		private static final Map<String, String> REQUEST_FACTORY_CANDIDATES;

		static {
			Map<String, String> candidates = new LinkedHashMap<>();
			candidates.put("okhttp3.OkHttpClient",
					"org.springframework.http.client.OkHttp3ClientHttpRequestFactory");
			REQUEST_FACTORY_CANDIDATES = Collections.unmodifiableMap(candidates);
		}

		@Override
		public Optional<WebServiceMessageSender> create() {
			ClassLoader classLoader = getClass().getClassLoader();
			for (Map.Entry<String, String> candidate : REQUEST_FACTORY_CANDIDATES
					.entrySet()) {
				if (ClassUtils.isPresent(candidate.getKey(), classLoader)) {
					Class<?> factoryClass = ClassUtils
							.resolveClassName(candidate.getValue(), classLoader);
					ClientHttpRequestFactory clientHttpRequestFactory = (ClientHttpRequestFactory) BeanUtils
							.instantiateClass(factoryClass);
					return Optional.of(
							new ClientHttpRequestMessageSender(clientHttpRequestFactory));
				}
			}
			return Optional.empty();
		}

	}

	private static final class HttpComponentsMessageSenderFactory
			implements WebServiceMessageSenderFactory {

		@Override
		public Optional<WebServiceMessageSender> create() {
			return Optional.of(new HttpComponentsMessageSender());
		}

	}

	/**
	 * {@link WebServiceTemplateCustomizer} to set
	 * {@link WebServiceTemplate#checkConnectionForFault checkConnectionForFault }.
	 */
	private static final class CheckConnectionFaultCustomizer
			implements WebServiceTemplateCustomizer {

		private final boolean checkConnectionFault;

		private CheckConnectionFaultCustomizer(boolean checkConnectionFault) {
			this.checkConnectionFault = checkConnectionFault;
		}

		@Override
		public void customize(WebServiceTemplate webServiceTemplate) {
			webServiceTemplate.setCheckConnectionForFault(this.checkConnectionFault);
		}

	}

	/**
	 * {@link WebServiceTemplateCustomizer} to set
	 * {@link WebServiceTemplate#checkConnectionForError checkConnectionForError }.
	 */
	private static final class CheckConnectionForErrorCustomizer
			implements WebServiceTemplateCustomizer {

		private final boolean checkConnectionForError;

		private CheckConnectionForErrorCustomizer(boolean checkConnectionForError) {
			this.checkConnectionForError = checkConnectionForError;
		}

		@Override
		public void customize(WebServiceTemplate webServiceTemplate) {
			webServiceTemplate.setCheckConnectionForError(this.checkConnectionForError);
		}

	}

	/**
	 * {@link WebServiceTemplateCustomizer} to set
	 * {@link WebServiceTemplate#faultMessageResolver faultMessageResolver }.
	 */
	private static final class FaultMessageResolverCustomizer
			implements WebServiceTemplateCustomizer {

		private final FaultMessageResolver faultMessageResolver;

		private FaultMessageResolverCustomizer(
				FaultMessageResolver faultMessageResolver) {
			this.faultMessageResolver = faultMessageResolver;
		}

		@Override
		public void customize(WebServiceTemplate webServiceTemplate) {
			webServiceTemplate.setFaultMessageResolver(this.faultMessageResolver);
		}

	}

	/**
	 * {@link WebServiceMessageSenderCustomizer} to set connection timeout.
	 */
	private static final class ConnectionTimeoutWebServiceMessageSenderCustomizer
			extends TimeoutWebServiceMessageSenderCustomizer {

		private ConnectionTimeoutWebServiceMessageSenderCustomizer(int connectTimeout) {
			super(connectTimeout, Timeout.CONNECTION);
		}

	}

	/**
	 * {@link WebServiceMessageSenderCustomizer} to set read timeout.
	 */
	private static final class ReadTimeoutWebServiceMessageSenderCustomizer
			extends TimeoutWebServiceMessageSenderCustomizer {

		private ReadTimeoutWebServiceMessageSenderCustomizer(int readTimeout) {
			super(readTimeout, Timeout.READ);
		}

	}

	private abstract static class TimeoutWebServiceMessageSenderCustomizer
			implements WebServiceMessageSenderCustomizer {

		private static final Map<String, Class<? extends TimeoutCustomizer<? extends WebServiceMessageSender>>> CUSTOMIZERS;

		static {
			Map<String, Class<? extends TimeoutCustomizer<? extends WebServiceMessageSender>>> candidates = new LinkedHashMap<>();
			candidates.put(
					"org.springframework.ws.transport.http.HttpComponentsMessageSender",
					HttpComponentsTimeoutCustomizer.class);
			candidates.put(
					"org.springframework.ws.transport.http.ClientHttpRequestMessageSender",
					ClientHttpRequestTimeoutCustomizer.class);
			candidates.put(
					"org.springframework.ws.transport.http.HttpUrlConnectionMessageSender",
					HttpUrlConnectionTimeoutCustomizer.class);
			CUSTOMIZERS = Collections.unmodifiableMap(candidates);
		}

		private final Timeout type;

		private final int timeout;

		TimeoutWebServiceMessageSenderCustomizer(int timeout, Timeout type) {
			this.timeout = timeout;
			this.type = type;
		}

		@Override
		public final void customize(WebServiceMessageSender webServiceMessageSender) {
			ClassLoader classLoader = getClass().getClassLoader();
			customize(CUSTOMIZERS, webServiceMessageSender, this.type, this.timeout,
					classLoader);

		}

		@SuppressWarnings("unchecked")
		private static <T> void customize(
				Map<String, Class<? extends TimeoutCustomizer<? extends T>>> customizers,
				T target, Timeout type, int timeout, ClassLoader classLoader) {
			for (Map.Entry<String, Class<? extends TimeoutCustomizer<? extends T>>> candidate : customizers
					.entrySet()) {
				if (ClassUtils.isPresent(candidate.getKey(), classLoader)) {
					Class<?> candidateClass = ClassUtils
							.resolveClassName(candidate.getKey(), classLoader);
					if (ClassUtils.isAssignable(candidateClass, target.getClass())) {
						TimeoutCustomizer timeoutCustomizer = BeanUtils
								.instantiateClass(candidate.getValue());
						customize(timeoutCustomizer, target, type, timeout);
						return;
					}
				}
			}
			throw new IllegalStateException("There is no way to customize '"
					+ target.getClass() + "' " + "with '" + type.name().toLowerCase()
					+ "Timeout'. Please use a custom " + "customizer.");

		}

		private static <T> void customize(TimeoutCustomizer<T> customizer, T target,
				Timeout type, int timeout) {
			if (type == Timeout.CONNECTION) {
				customizer.setConnectionTimeout(target, timeout);
			}
			else if (type == Timeout.READ) {
				customizer.setReadTimeout(target, timeout);
			}
		}

		interface TimeoutCustomizer<T> {

			void setReadTimeout(T source, int timeout);

			void setConnectionTimeout(T source, int timeout);

		}

		enum Timeout {

			READ, CONNECTION

		}

		private static final class HttpComponentsTimeoutCustomizer
				implements TimeoutCustomizer<HttpComponentsMessageSender> {

			@Override
			public void setReadTimeout(HttpComponentsMessageSender source, int timeout) {
				source.setReadTimeout(timeout);
			}

			@Override
			public void setConnectionTimeout(HttpComponentsMessageSender source,
					int timeout) {
				source.setConnectionTimeout(timeout);
			}

		}

		private static final class HttpUrlConnectionTimeoutCustomizer
				implements TimeoutCustomizer<HttpUrlConnectionMessageSender> {

			@Override
			public void setReadTimeout(HttpUrlConnectionMessageSender source,
					int timeout) {
				source.setReadTimeout(Duration.ofMillis(timeout));
			}

			@Override
			public void setConnectionTimeout(HttpUrlConnectionMessageSender source,
					int timeout) {
				source.setConnectionTimeout(Duration.ofMillis(timeout));
			}

		}

		private static final class ClientHttpRequestTimeoutCustomizer
				implements TimeoutCustomizer<ClientHttpRequestMessageSender> {

			private static final Map<String, Class<? extends TimeoutCustomizer<? extends ClientHttpRequestFactory>>> CUSTOMIZERS;

			static {
				Map<String, Class<? extends TimeoutCustomizer<? extends ClientHttpRequestFactory>>> candidates = new LinkedHashMap<>();
				candidates.put(
						"org.springframework.http.client.HttpComponentsClientHttpRequestFactory",
						HttpComponentsClientHttpRequestFactoryTimeoutCustomizer.class);
				candidates.put(
						"org.springframework.http.client.OkHttp3ClientHttpRequestFactory",
						OkHttp3ClientHttpRequestFactoryTimeoutCustomizer.class);
				candidates.put(
						"org.springframework.http.client.SimpleClientHttpRequestFactory",
						SimpleClientHttpRequestFactoryTimeoutCustomizer.class);
				CUSTOMIZERS = Collections.unmodifiableMap(candidates);
			}

			@Override
			public void setReadTimeout(ClientHttpRequestMessageSender source,
					int timeout) {
				ClassLoader classLoader = getClass().getClassLoader();
				customize(CUSTOMIZERS, getRequestFactory(source), Timeout.READ, timeout,
						classLoader);
			}

			@Override
			public void setConnectionTimeout(ClientHttpRequestMessageSender source,
					int timeout) {
				ClassLoader classLoader = getClass().getClassLoader();
				customize(CUSTOMIZERS, getRequestFactory(source), Timeout.CONNECTION,
						timeout, classLoader);
			}

			private ClientHttpRequestFactory getRequestFactory(
					ClientHttpRequestMessageSender source) {
				ClientHttpRequestFactory requestFactory = source.getRequestFactory();
				if (!(requestFactory instanceof AbstractClientHttpRequestFactoryWrapper)) {
					return requestFactory;
				}
				Field field = ReflectionUtils.findField(
						AbstractClientHttpRequestFactoryWrapper.class, "requestFactory");
				Assert.notNull(field, "Field must not be null");
				ReflectionUtils.makeAccessible(field);
				do {
					requestFactory = (ClientHttpRequestFactory) ReflectionUtils
							.getField(field, requestFactory);
				}
				while (requestFactory instanceof AbstractClientHttpRequestFactoryWrapper);
				return requestFactory;
			}

			private static final class SimpleClientHttpRequestFactoryTimeoutCustomizer
					implements TimeoutCustomizer<SimpleClientHttpRequestFactory> {

				@Override
				public void setReadTimeout(SimpleClientHttpRequestFactory source,
						int timeout) {
					source.setReadTimeout(timeout);
				}

				@Override
				public void setConnectionTimeout(SimpleClientHttpRequestFactory source,
						int timeout) {
					source.setConnectTimeout(timeout);
				}

			}

			private static final class HttpComponentsClientHttpRequestFactoryTimeoutCustomizer
					implements TimeoutCustomizer<HttpComponentsClientHttpRequestFactory> {

				@Override
				public void setReadTimeout(HttpComponentsClientHttpRequestFactory source,
						int timeout) {
					source.setReadTimeout(timeout);
				}

				@Override
				public void setConnectionTimeout(
						HttpComponentsClientHttpRequestFactory source, int timeout) {
					source.setConnectTimeout(timeout);
				}

			}

			private static final class OkHttp3ClientHttpRequestFactoryTimeoutCustomizer
					implements TimeoutCustomizer<OkHttp3ClientHttpRequestFactory> {

				@Override
				public void setReadTimeout(OkHttp3ClientHttpRequestFactory source,
						int timeout) {
					source.setReadTimeout(timeout);
				}

				@Override
				public void setConnectionTimeout(OkHttp3ClientHttpRequestFactory source,
						int timeout) {
					source.setConnectTimeout(timeout);
				}

			}

		}

	}

}
