package at.fh.swenga.plavent.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestMapping;

import at.fh.swenga.plavent.model.User;
import at.fh.swenga.plavent.repo.HappeningCategoryRepository;

/**
 * @author Stefan Heider:
 *         
 * description of this controller
 *
 */



@Controller
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, value = "session")
public class HappeningCategoryController {

	@Autowired
	private HappeningCategoryRepository categoryDao;
	
	public HappeningCategoryController() {
		// TODO Auto-generated constructor stub
	}


	private boolean errorsDetected(Model model, BindingResult bindingResult) {
		// Any errors? -> Create a String out of all errors and return to the page
		if (bindingResult.hasErrors()) {
			String errorMessage = "";
			for (FieldError fieldError : bindingResult.getFieldErrors()) {
				errorMessage += fieldError.getField() + " is invalid";
			}
			model.addAttribute("errorMessage", errorMessage);
			return true;
		}
		return false;
	}

	@Secured({ "ROLE_ADMIN" })
	@RequestMapping(value = { "showCategoryManagement" })
	public String showCategories(Model model) {	
		//Set attributes
		model.addAttribute("happeningCategories", categoryDao.findAll());
		return "categoryManagement";
	}
	
	//TODO: Create methods for requests:
	//showCreateCategoryForm
	//filterCategories
	
	//deleteCategory ==> Just update 'enabled' flag, do not delete them!
	//reactivateCategory => 'Update enabled flag to actiate them again
	

}