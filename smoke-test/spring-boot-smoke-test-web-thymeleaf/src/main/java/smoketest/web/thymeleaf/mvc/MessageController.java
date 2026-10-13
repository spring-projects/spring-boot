/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.web.thymeleaf.mvc;

import jakarta.validation.Valid;
import smoketest.web.thymeleaf.Message;
import smoketest.web.thymeleaf.MessageRepository;

import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/")
public class MessageController {

	private final MessageRepository messageRepository;

	public MessageController(MessageRepository messageRepository) {
		this.messageRepository = messageRepository;
	}

	@GetMapping
	public ModelAndView list() {
		Iterable<Message> messages = this.messageRepository.findAll();
		return new ModelAndView("messages/list", "messages", messages);
	}

	@GetMapping("{id}")
	public ModelAndView view(@PathVariable("id") Message message) {
		return new ModelAndView("messages/view", "message", message);
	}

	@GetMapping(params = "form")
	public String createForm(@ModelAttribute Message message) {
		return "messages/form";
	}

	@PostMapping
	public ModelAndView create(@Valid Message message, BindingResult result, RedirectAttributes redirect) {
		if (result.hasErrors()) {
			return new ModelAndView("messages/form", "formErrors", result.getAllErrors());
		}
		message = this.messageRepository.save(message);
		redirect.addFlashAttribute("globalMessage", "view.success");
		Long id = message.getId();
		Assert.state(id != null, "'id' must not be null");
		return new ModelAndView("redirect:/{message.id}", "message.id", id);
	}

	@RequestMapping("foo")
	public String foo() {
		throw new RuntimeException("Expected exception in controller");
	}

	@GetMapping("delete/{id}")
	public ModelAndView delete(@PathVariable("id") Long id) {
		this.messageRepository.deleteMessage(id);
		Iterable<Message> messages = this.messageRepository.findAll();
		return new ModelAndView("messages/list", "messages", messages);
	}

	@GetMapping("modify/{id}")
	public ModelAndView modifyForm(@PathVariable("id") Message message) {
		return new ModelAndView("messages/form", "message", message);
	}

}
