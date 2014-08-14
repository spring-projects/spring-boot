package org.springframework.boot.autoconfigure.hdiv;

import javax.annotation.PostConstruct;

import org.hdiv.web.validator.EditableParameterValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

@Configuration
public class EditableValidationConfiguration {

	@Autowired
	private EditableParameterValidator editableParameterValidator;

	@Autowired
	private RequestMappingHandlerAdapter handlerAdapter;

	@PostConstruct
	public void initEditableValidation() {

		// Add HDIVs Validator for editable validation to Spring MVC
		ConfigurableWebBindingInitializer initializer = (ConfigurableWebBindingInitializer) handlerAdapter
				.getWebBindingInitializer();
		if (initializer.getValidator() != null) {
			// Wrap existing validator
			editableParameterValidator.setInnerValidator(initializer.getValidator());
		}
		initializer.setValidator(editableParameterValidator);

	}
}
