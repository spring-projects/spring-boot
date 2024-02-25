/*
 * Copyright 2012-2022 the original author or authors.
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

package smoketest.web.thymeleaf.mvc;

import jakarta.validation.Valid;
import smoketest.web.thymeleaf.Message;
import smoketest.web.thymeleaf.MessageRepository;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * MessageController class.
 */
@Controller
@RequestMapping("/")
public class MessageController {

	private final MessageRepository messageRepository;

	/**
     * Constructs a new MessageController with the specified MessageRepository.
     * 
     * @param messageRepository the MessageRepository to be used by the MessageController
     */
    public MessageController(MessageRepository messageRepository) {
		this.messageRepository = messageRepository;
	}

	/**
     * Retrieves a list of messages.
     * 
     * @return ModelAndView object containing the list of messages
     */
    @GetMapping
	public ModelAndView list() {
		Iterable<Message> messages = this.messageRepository.findAll();
		return new ModelAndView("messages/list", "messages", messages);
	}

	/**
     * Retrieves and displays a specific message by its ID.
     * 
     * @param id the ID of the message to be retrieved
     * @return a ModelAndView object representing the view of the message
     */
    @GetMapping("{id}")
	public ModelAndView view(@PathVariable("id") Message message) {
		return new ModelAndView("messages/view", "message", message);
	}

	/**
     * Retrieves the create form for a new message.
     * 
     * @param message the message object to be bound to the form
     * @return the view name for the create form
     */
    @GetMapping(params = "form")
	public String createForm(@ModelAttribute Message message) {
		return "messages/form";
	}

	/**
     * Creates a new message.
     * 
     * @param message the message to be created
     * @param result the binding result for validation errors
     * @param redirect the redirect attributes for flash messages
     * @return a ModelAndView object for the view
     */
    @PostMapping
	public ModelAndView create(@Valid Message message, BindingResult result, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			return new ModelAndView("messages/form", "formErrors", result.getAllErrors());
		}
		message = this.messageRepository.save(message);
		redirect.addFlashAttribute("globalMessage", "view.success");
		return new ModelAndView("redirect:/{message.id}", "message.id", message.getId());
	}

	/**
     * Handles the request mapping for the "/foo" endpoint.
     * 
     * @return the response string
     * @throws RuntimeException if an expected exception occurs in the controller
     */
    @RequestMapping("foo")
	public String foo() {
		throw new RuntimeException("Expected exception in controller");
	}

	/**
     * Deletes a message with the given ID.
     * 
     * @param id the ID of the message to be deleted
     * @return a ModelAndView object containing the updated list of messages
     */
    @GetMapping("delete/{id}")
	public ModelAndView delete(@PathVariable("id") Long id) {
		this.messageRepository.deleteMessage(id);
		Iterable<Message> messages = this.messageRepository.findAll();
		return new ModelAndView("messages/list", "messages", messages);
	}

	/**
     * Retrieves the modify form for a specific message.
     * 
     * @param id the ID of the message to be modified
     * @return a ModelAndView object representing the modify form
     */
    @GetMapping("modify/{id}")
	public ModelAndView modifyForm(@PathVariable("id") Message message) {
		return new ModelAndView("messages/form", "message", message);
	}

}
