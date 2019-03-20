/*
 * Copyright 2012-2016 the original author or authors.
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

package sample.devtools;

import java.util.Date;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class MyController {

	@PostConstruct
	public void slowRestart() throws InterruptedException {
		Thread.sleep(5000);
	}

	@GetMapping("/")
	public ModelAndView get(HttpSession session) {
		Object sessionVar = session.getAttribute("var");
		if (sessionVar == null) {
			sessionVar = new Date();
			session.setAttribute("var", sessionVar);
		}
		ModelMap model = new ModelMap("message", Message.MESSAGE)
				.addAttribute("sessionVar", sessionVar);
		return new ModelAndView("hello", model);
	}

}
