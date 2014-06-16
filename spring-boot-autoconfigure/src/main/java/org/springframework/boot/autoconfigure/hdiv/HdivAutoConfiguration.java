package org.springframework.boot.autoconfigure.hdiv;

import java.util.EnumSet;

import javax.annotation.PostConstruct;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.hdiv.config.HDIVConfig;
import org.hdiv.filter.ValidatorFilter;
import org.hdiv.listener.InitListener;
import org.hdiv.web.validator.EditableParameterValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration 
 * Auto-configuration} for HDIV Integration.
 * 
 * @since 1.1.1
 */
@Configuration
@ConditionalOnClass(ValidatorFilter.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class HdivAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(HDIVConfig.class)
	// TODO JavaConfig instead of XML
	@ImportResource("classpath:org/springframework/boot/autoconfigure/hdiv/hdiv-config.xml")
	protected static class HdivDefaultConfiguration {

		// TODO autodetect actuator endpoints and configure as startPages

		@Autowired
		private ApplicationContext context;

		@PostConstruct
		public void init() {

			// Add HDIVs Validator for editable validation to Spring MVC
			EditableParameterValidator hdivEditableValidator = (EditableParameterValidator) context
					.getBean("hdivEditableValidator");

			RequestMappingHandlerAdapter handlerAdapter = context.getBean(RequestMappingHandlerAdapter.class);
			ConfigurableWebBindingInitializer initializer = (ConfigurableWebBindingInitializer) handlerAdapter
					.getWebBindingInitializer();
			if (initializer.getValidator() != null) {
				// Wrap existing validator
				hdivEditableValidator.setInnerValidator(initializer.getValidator());
			}
			initializer.setValidator(hdivEditableValidator);

		}
	}

	@Bean
	@ConditionalOnBean(HDIVConfig.class)
	public ServletContextInitializer validatorFilter() {
		ServletContextInitializer initializer = new ServletContextInitializer() {

			@Override
			public void onStartup(ServletContext servletContext) throws ServletException {

				// Register HDIV InitListener and ValidatorFilter

				// InitListener
				servletContext.addListener(new InitListener());

				// ValidatorFilter
				FilterRegistration.Dynamic registration = servletContext.addFilter("validatorFilter",
						new ValidatorFilter());
				EnumSet<DispatcherType> dispatcherTypes = EnumSet.of(DispatcherType.REQUEST);
				// isMatchAfter false to put it before existing filters
				registration.addMappingForUrlPatterns(dispatcherTypes, false, "/*");
			}
		};
		return initializer;
	}

}
