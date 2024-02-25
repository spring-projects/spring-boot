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

package org.springframework.boot.actuate.autoconfigure.web.server;

import java.util.List;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextFactory;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.web.context.ConfigurableWebServerApplicationContext;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerGracefulShutdownLifecycle;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.javapoet.ClassName;
import org.springframework.util.Assert;

/**
 * {@link ApplicationListener} used to initialize the management context when it's running
 * on a different port.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ChildManagementContextInitializer implements BeanRegistrationAotProcessor, SmartLifecycle {

	private final ManagementContextFactory managementContextFactory;

	private final ApplicationContext parentContext;

	private final ApplicationContextInitializer<ConfigurableApplicationContext> applicationContextInitializer;

	private volatile ConfigurableApplicationContext managementContext;

	/**
     * Constructs a new ChildManagementContextInitializer with the specified ManagementContextFactory, parentContext, and additional arguments.
     * 
     * @param managementContextFactory the ManagementContextFactory used to create the management context
     * @param parentContext the parent ApplicationContext
     */
    ChildManagementContextInitializer(ManagementContextFactory managementContextFactory,
			ApplicationContext parentContext) {
		this(managementContextFactory, parentContext, null);
	}

	/**
     * Constructs a new ChildManagementContextInitializer with the specified parameters.
     * 
     * @param managementContextFactory the management context factory to be used
     * @param parentContext the parent application context
     * @param applicationContextInitializer the application context initializer
     */
    @SuppressWarnings("unchecked")
	private ChildManagementContextInitializer(ManagementContextFactory managementContextFactory,
			ApplicationContext parentContext,
			ApplicationContextInitializer<? extends ConfigurableApplicationContext> applicationContextInitializer) {
		this.managementContextFactory = managementContextFactory;
		this.parentContext = parentContext;
		this.applicationContextInitializer = (ApplicationContextInitializer<ConfigurableApplicationContext>) applicationContextInitializer;
	}

	/**
     * Starts the ChildManagementContextInitializer.
     * 
     * This method checks if the parent context is an instance of WebServerApplicationContext. If not, it returns without performing any action.
     * If the management context is null, it creates a new ConfigurableApplicationContext for management, registers beans, refreshes the context, and assigns it to the managementContext variable.
     * If the management context is not null, it starts the management context.
     */
    @Override
	public void start() {
		if (!(this.parentContext instanceof WebServerApplicationContext)) {
			return;
		}
		if (this.managementContext == null) {
			ConfigurableApplicationContext managementContext = createManagementContext();
			registerBeans(managementContext);
			managementContext.refresh();
			this.managementContext = managementContext;
		}
		else {
			this.managementContext.start();
		}
	}

	/**
     * Stops the ChildManagementContextInitializer.
     * 
     * This method stops the ChildManagementContextInitializer by stopping the management context associated with it.
     * If the management context is not null, it will be stopped.
     */
    @Override
	public void stop() {
		if (this.managementContext != null) {
			this.managementContext.stop();
		}
	}

	/**
     * Returns a boolean value indicating whether the management context is running.
     * 
     * @return true if the management context is running, false otherwise
     */
    @Override
	public boolean isRunning() {
		return this.managementContext != null && this.managementContext.isRunning();
	}

	/**
     * Returns the phase of the ChildManagementContextInitializer.
     * The phase is calculated by adding 512 to the phase of the WebServerGracefulShutdownLifecycle.
     *
     * @return the phase of the ChildManagementContextInitializer
     */
    @Override
	public int getPhase() {
		return WebServerGracefulShutdownLifecycle.SMART_LIFECYCLE_PHASE + 512;
	}

	/**
     * Processes the registered bean ahead of time.
     * 
     * @param registeredBean the registered bean to process
     * @return the BeanRegistrationAotContribution object
     * @throws IllegalArgumentException if the parent context is not an instance of ConfigurableApplicationContext
     */
    @Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		Assert.isInstanceOf(ConfigurableApplicationContext.class, this.parentContext);
		BeanFactory parentBeanFactory = ((ConfigurableApplicationContext) this.parentContext).getBeanFactory();
		if (registeredBean.getBeanClass().equals(getClass())
				&& registeredBean.getBeanFactory().equals(parentBeanFactory)) {
			ConfigurableApplicationContext managementContext = createManagementContext();
			registerBeans(managementContext);
			return new AotContribution(managementContext);
		}
		return null;
	}

	/**
     * Returns whether the bean is excluded from Ahead-of-Time (AOT) processing.
     * 
     * @return {@code false} if the bean is not excluded from AOT processing, {@code true} otherwise.
     */
    @Override
	public boolean isBeanExcludedFromAotProcessing() {
		return false;
	}

	/**
     * Registers beans in the provided management context.
     * 
     * @param managementContext the configurable application context for management
     * @throws IllegalArgumentException if the management context is not an instance of AnnotationConfigRegistry
     */
    private void registerBeans(ConfigurableApplicationContext managementContext) {
		if (this.applicationContextInitializer != null) {
			this.applicationContextInitializer.initialize(managementContext);
			return;
		}
		Assert.isInstanceOf(AnnotationConfigRegistry.class, managementContext);
		AnnotationConfigRegistry registry = (AnnotationConfigRegistry) managementContext;
		this.managementContextFactory.registerWebServerFactoryBeans(this.parentContext, managementContext, registry);
		registry.register(EnableChildManagementContextConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
		if (isLazyInitialization()) {
			managementContext.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
		}
	}

	/**
     * Creates a management context for the child application context.
     * 
     * @return the created management context
     */
    protected final ConfigurableApplicationContext createManagementContext() {
		ConfigurableApplicationContext managementContext = this.managementContextFactory
			.createManagementContext(this.parentContext);
		managementContext.setId(this.parentContext.getId() + ":management");
		if (managementContext instanceof ConfigurableWebServerApplicationContext webServerApplicationContext) {
			webServerApplicationContext.setServerNamespace("management");
		}
		if (managementContext instanceof DefaultResourceLoader resourceLoader) {
			resourceLoader.setClassLoader(this.parentContext.getClassLoader());
		}
		CloseManagementContextListener.addIfPossible(this.parentContext, managementContext);
		return managementContext;
	}

	/**
     * Checks if lazy initialization is enabled for the child management context.
     * 
     * @return true if lazy initialization is enabled, false otherwise
     */
    private boolean isLazyInitialization() {
		AbstractApplicationContext context = (AbstractApplicationContext) this.parentContext;
		List<BeanFactoryPostProcessor> postProcessors = context.getBeanFactoryPostProcessors();
		return postProcessors.stream().anyMatch(LazyInitializationBeanFactoryPostProcessor.class::isInstance);
	}

	/**
     * Creates a new ChildManagementContextInitializer with the specified ApplicationContextInitializer.
     * 
     * @param applicationContextInitializer the ApplicationContextInitializer to be used
     * @return a new ChildManagementContextInitializer
     */
    ChildManagementContextInitializer withApplicationContextInitializer(
			ApplicationContextInitializer<? extends ConfigurableApplicationContext> applicationContextInitializer) {
		return new ChildManagementContextInitializer(this.managementContextFactory, this.parentContext,
				applicationContextInitializer);
	}

	/**
	 * {@link BeanRegistrationAotContribution} for
	 * {@link ChildManagementContextInitializer}.
	 */
	private static class AotContribution implements BeanRegistrationAotContribution {

		private final GenericApplicationContext managementContext;

		/**
         * Initializes the AotContribution object with the provided management context.
         * 
         * @param managementContext the ConfigurableApplicationContext to be used as the management context
         * @throws IllegalArgumentException if the managementContext is not an instance of GenericApplicationContext
         */
        AotContribution(ConfigurableApplicationContext managementContext) {
			Assert.isInstanceOf(GenericApplicationContext.class, managementContext);
			this.managementContext = (GenericApplicationContext) managementContext;
		}

		/**
         * Applies the AOT (Ahead of Time) configuration to the given generation context and bean registration code.
         * 
         * @param generationContext The generation context to apply the AOT configuration to.
         * @param beanRegistrationCode The bean registration code to apply the AOT configuration to.
         */
        @Override
		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
			GenerationContext managementGenerationContext = generationContext.withName("Management");
			ClassName generatedInitializerClassName = new ApplicationContextAotGenerator()
				.processAheadOfTime(this.managementContext, managementGenerationContext);
			GeneratedMethod postProcessorMethod = beanRegistrationCode.getMethods()
				.add("addManagementInitializer",
						(method) -> method.addJavadoc("Use AOT management context initialization")
							.addModifiers(Modifier.PRIVATE, Modifier.STATIC)
							.addParameter(RegisteredBean.class, "registeredBean")
							.addParameter(ChildManagementContextInitializer.class, "instance")
							.returns(ChildManagementContextInitializer.class)
							.addStatement("return instance.withApplicationContextInitializer(new $L())",
									generatedInitializerClassName));
			beanRegistrationCode.addInstancePostProcessor(postProcessorMethod.toMethodReference());
		}

	}

	/**
	 * {@link ApplicationListener} to propagate the {@link ContextClosedEvent} and
	 * {@link ApplicationFailedEvent} from a parent to a child.
	 */
	private static class CloseManagementContextListener implements ApplicationListener<ApplicationEvent> {

		private final ApplicationContext parentContext;

		private final ConfigurableApplicationContext childContext;

		/**
         * Constructs a new CloseManagementContextListener with the specified parent and child application contexts.
         * 
         * @param parentContext the parent application context
         * @param childContext the child application context
         */
        CloseManagementContextListener(ApplicationContext parentContext, ConfigurableApplicationContext childContext) {
			this.parentContext = parentContext;
			this.childContext = childContext;
		}

		/**
         * This method is called when an application event is triggered.
         * It checks the type of the event and calls the corresponding handler method.
         * 
         * @param event The application event that was triggered.
         */
        @Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof ContextClosedEvent contextClosedEvent) {
				onContextClosedEvent(contextClosedEvent);
			}
			if (event instanceof ApplicationFailedEvent applicationFailedEvent) {
				onApplicationFailedEvent(applicationFailedEvent);
			}
		}

		/**
         * Handles the ContextClosedEvent by propagating the close if necessary.
         * 
         * @param event the ContextClosedEvent triggered
         */
        private void onContextClosedEvent(ContextClosedEvent event) {
			propagateCloseIfNecessary(event.getApplicationContext());
		}

		/**
         * Handles the ApplicationFailedEvent by propagating the close if necessary.
         * 
         * @param event the ApplicationFailedEvent to handle
         */
        private void onApplicationFailedEvent(ApplicationFailedEvent event) {
			propagateCloseIfNecessary(event.getApplicationContext());
		}

		/**
         * Propagates the close operation to the child context if necessary.
         * 
         * @param applicationContext the ApplicationContext to check for close propagation
         */
        private void propagateCloseIfNecessary(ApplicationContext applicationContext) {
			if (applicationContext == this.parentContext) {
				this.childContext.close();
			}
		}

		/**
         * Adds the child context to the parent context if possible.
         * 
         * @param parentContext the parent application context
         * @param childContext the child configurable application context
         */
        static void addIfPossible(ApplicationContext parentContext, ConfigurableApplicationContext childContext) {
			if (parentContext instanceof ConfigurableApplicationContext configurableApplicationContext) {
				add(configurableApplicationContext, childContext);
			}
		}

		/**
         * Adds a child context to the parent context and registers a CloseManagementContextListener
         * to handle the closing of the child context.
         *
         * @param parentContext the parent context to which the child context will be added
         * @param childContext the child context to be added to the parent context
         */
        private static void add(ConfigurableApplicationContext parentContext,
				ConfigurableApplicationContext childContext) {
			parentContext.addApplicationListener(new CloseManagementContextListener(parentContext, childContext));
		}

	}

}
