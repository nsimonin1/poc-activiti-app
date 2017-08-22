/**
 *
 */
package net.freelancertech.vaccation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author simon.pascal.ngos
 *
 */
@Controller
public class HomePageController {

	@GetMapping("/")
	public String homepage() {
		return "redirect:/index.htm";
	}

	@GetMapping("/index.htm")
	public String index() {
		return "index";
	}
}
