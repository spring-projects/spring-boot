/*
 * Copyright 2012-2014 the original author or authors.
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

package sample.hdiv.web;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import sample.hdiv.bean.FormBean;

@Controller
public class MainController {

	static final Logger LOG = LoggerFactory.getLogger(MainController.class);

	@RequestMapping("/")
	String home() {
		return "main";
	}

	@RequestMapping("/links")
	String links() {
		return "links";
	}

	@RequestMapping(value = "/form", method = RequestMethod.GET)
	String form(Model model) {

		model.addAttribute("formBean", new FormBean());
		return "form";
	}

	@RequestMapping(value = "/form", method = RequestMethod.POST)
	String submit(@Valid @ModelAttribute FormBean bean, BindingResult bindingResult) {

		if (bindingResult.hasErrors()) {
			return "form";
		}
		LOG.info("Name: " + bean.getName() + " Type: " + bean.getType());
		return "main";
	}

}
