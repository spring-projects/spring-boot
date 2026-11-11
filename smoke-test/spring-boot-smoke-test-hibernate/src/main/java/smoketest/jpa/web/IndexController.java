/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jpa.web;

import java.util.List;

import smoketest.jpa.domain.Note;
import smoketest.jpa.repository.NoteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {

	@Autowired
	private NoteRepository noteRepository;

	@GetMapping("/")
	@Transactional(readOnly = true)
	public ModelAndView index() {
		List<Note> notes = this.noteRepository.findAll();
		ModelAndView modelAndView = new ModelAndView("index");
		modelAndView.addObject("notes", notes);
		return modelAndView;
	}

}
