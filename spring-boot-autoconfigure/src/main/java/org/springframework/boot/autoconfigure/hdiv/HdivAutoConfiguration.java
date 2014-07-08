package org.springframework.boot.autoconfigure.hdiv;

import java.util.EnumSet;

import javax.annotation.PostConstruct;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.hdiv.config.annotation.EnableHdivWebSecurity;
import org.hdiv.config.annotation.ExclusionRegistry;
import org.hdiv.config.annotation.ValidationConfigurer;
import org.hdiv.config.annotation.configuration.HdivWebSecurityConfigurerAdapter;
import org.hdiv.filter.ValidatorFilter;
import org.hdiv.listener.InitListener;
import org.hdiv.web.validator.EditableParameterValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for HDIV Integration.
 */
@Configuration
@ConditionalOnClass(ValidatorFilter.class)
@ConditionalOnWebApplication
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class HdivAutoConfiguration {

	@Configuration
	@EnableHdivWebSecurity
	protected static class HdivDefaultConfiguration extends HdivWebSecurityConfigurerAdapter {

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

		@Override
		public void addExclusions(ExclusionRegistry registry) {

			// Static content
			registry.addUrlExclusions("/webjars/.*").method("GET");
			// It's not possible to exclude static by pattern, like /resources/.*
			registry.addUrlExclusions(".*.js").method("GET");
			registry.addUrlExclusions(".*.css").method("GET");
			registry.addUrlExclusions(".*.png").method("GET");
			registry.addUrlExclusions(".*.jpg").method("GET");
			registry.addUrlExclusions(".*.woff").method("GET");
			registry.addUrlExclusions(".*.ttf").method("GET");
			registry.addUrlExclusions(".*.svg").method("GET");
			registry.addUrlExclusions(".*.ico").method("GET");

			// Excluded URLs
			registry.addUrlExclusions("/").method("GET");

			// It's possible to autodetect actuator endpoints and configure as them as exclusion?
			// Actuator filters
			registry.addUrlExclusions("/health");
			registry.addUrlExclusions("/beans").method("GET");
			registry.addUrlExclusions("/trace").method("GET");
			registry.addUrlExclusions("/configprops").method("GET");
			registry.addUrlExclusions("/info").method("GET");
			registry.addUrlExclusions("/dump").method("GET");
			registry.addUrlExclusions("/autoconfig").method("GET");
			registry.addUrlExclusions("/metrics", "/metrics/.*").method("GET");
			registry.addUrlExclusions("/env", "/env/.*").method("GET");
			registry.addUrlExclusions("/mappings").method("GET");
		}

		@Override
		public void configureEditableValidation(ValidationConfigurer validationConfigurer) {

			// Enable default rules for all URLs.
			validationConfigurer.addValidation(".*");
		}

	}

	@Bean
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
