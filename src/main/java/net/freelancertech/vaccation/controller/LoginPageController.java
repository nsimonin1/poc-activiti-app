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
public class LoginPageController {

	@GetMapping("/login.htm")
	public String homepage() {
		return "login";
	}

}
