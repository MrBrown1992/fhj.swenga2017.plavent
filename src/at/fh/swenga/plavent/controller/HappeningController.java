package at.fh.swenga.plavent.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import at.fh.swenga.plavent.model.Happening;
import at.fh.swenga.plavent.repo.HappeningCategoryRepository;
import at.fh.swenga.plavent.repo.HappeningRepository;
import at.fh.swenga.plavent.repo.HappeningStatusRepository;
import at.fh.swenga.plavent.repo.UserRepository;

/**
 * @author Alexander Hoedl:
 * 
 *         Controller to handle Happenings - Create new Happening - Modify
 *         existing Happening - Delete Happening (Deactivate) - Reactivate
 *         Happening
 */

@Controller
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, value = "session")
public class HappeningController {

	@Autowired
	HappeningRepository happeningRepo;

	@Autowired
	HappeningCategoryRepository happeningCategoryRepo;

	@Autowired
	HappeningStatusRepository happeningStatusRepo;

	@Autowired
	UserRepository userRepo;

	public HappeningController() {
	}

	/**
	 * Helper method to check if current logged in user is either owner of happening
	 * or has role ADMIN which allows to modify the task
	 * 
	 * @param happening
	 *            - Happening to check
	 * @param authentication
	 *            - current logged in user
	 * @return
	 */
	public boolean isHappeningHostOrAdmin(Happening happening, Authentication authentication) {

		if (happening == null || authentication == null) {
			return false;
		} else {
			return (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))
					|| happening.getHappeningHost().getUsername().equals(authentication.getName()));
		}
	}

	private boolean errorsDetected(Model model, BindingResult bindingResult) {
		// Any errors? -> Create a String out of all errors and return to the page
		if (bindingResult.hasErrors()) {
			String errorMessage = "";
			for (FieldError fieldError : bindingResult.getFieldErrors()) {
				errorMessage += fieldError.getField() + " is invalid<br>";
			}
			model.addAttribute("errorMessage", errorMessage);
			return true;
		}
		return false;
	}

	// -----------------------------------------------------------------------------------------
	// --- GENERAL HAPPENING MANAGEMENT ---
	// -----------------------------------------------------------------------------------------
	@Secured({ "ROLE_HOST" })
	@RequestMapping(value = { "showHappeningManagement" })
	public String showHappenings(Model model, Authentication authentication) {

		// ADMINS are allowed to see all happening. HOSTS just happenings shich belongs
		// to them and are active
		if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
			model.addAttribute("happenings", happeningRepo.findAll());
		} else {
			// Show just active ones!
			model.addAttribute("happenings", happeningRepo.getActiveHappeningsForHost(authentication.getName()));
		}

		return "happeningManagement";
	}

	@Secured({ "ROLE_HOST" })
	@RequestMapping(value = { "showCreateHappeningForm" })
	public String showCreateHappeningForm(Model model, Authentication authentication) {

		// Set required attributes
		//TOOD: Just show active (enabled) ones
		model.addAttribute("happeningCategories", happeningCategoryRepo.findAll());

		// ADMins are allowed to create a happening for every host. HOSTS just for
		// themself.
		if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
			// Just set Users with role HOST
			model.addAttribute("happeningHosts", userRepo.getUsersByRolename("ROLE_HOST"));
		} else {
			model.addAttribute("happeningHosts", userRepo.findFirstByUsername(authentication.getName()));
		}

		return "createModifyHappening";
	}

	@Secured({ "ROLE_HOST" })
	@RequestMapping(value = { "showModifyExistingHappingForm" })
	public String showModifyExistingHappingForm(Model model, @RequestParam(value = "happeningId") Happening happening,
			Authentication authentication) {

		// Check if user is Owner of Happening or has role ADMIN
		if (!isHappeningHostOrAdmin(happening, authentication)) {
			model.addAttribute("warningMessage", "Happening not found or no permission!");
			return showHappenings(model, authentication);
		}

		model.addAttribute("happening", happening);
		
		//TOOD: Just show active (enabled) ones
		model.addAttribute("happeningCategories", happeningCategoryRepo.findAll());

		// ADMins are allowed to create a happening for every host. HOSTS just for
		// themself.
		if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
			// Just set Users with role HOST
			model.addAttribute("happeningHosts", userRepo.getUsersByRolename("ROLE_HOST"));
		} else {
			model.addAttribute("happeningHosts", userRepo.findFirstByUsername(authentication.getName()));
		}

		return "createModifyHappening";
	}

	@Secured({ "ROLE_HOST" })
	@PostMapping("/createNewHappening")
	public String createNewHappening(@Valid Happening newHappening, @RequestParam(value = "host") String hostUsername,
			@RequestParam(value = "startDate") String startAsString,
			@RequestParam(value = "endDate") String endAsString, @RequestParam(value = "categoryID") int categoryID,
			Model model, Authentication authentication, BindingResult bindingResult) throws ParseException {

		// Check if user is newOwner of Happening is logged in user or logged in user is
		// ADMIN
		if (!(authentication.getName().equals(hostUsername)
				|| authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")))) {
			model.addAttribute("warningMessage", "Happening not found or no permission!");
			return showHappenings(model, authentication);
		}

		if (errorsDetected(model, bindingResult)) {
			return showHappenings(model, authentication);
		}

		// Set correct connection objects
		Calendar now = Calendar.getInstance();
		now.set(Calendar.HOUR_OF_DAY, 0);
		now.set(Calendar.MINUTE, 0);
		now.set(Calendar.SECOND, 0);
		now.set(Calendar.MILLISECOND, 0);

		SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy hh:mm");
		Calendar start = Calendar.getInstance();
		start.setTime(format.parse(startAsString));

		Calendar end = Calendar.getInstance();
		end.setTime(format.parse(endAsString));

		// Check if start date before "trimmed now" (Just date, time does not matter)
		// Check if end-Date is after start date
		if (start.before(now) || end.before(start)) {
			model.addAttribute("warningMessage",
					"Happening not allwed to start in past Or End is not allowed to be before start!");
			return showHappenings(model, authentication);
		}

		newHappening.setHappeningStatus(happeningStatusRepo.findFirstByStatusName("ACTIVE"));
		newHappening.setHappeningHost(userRepo.findFirstByUsername(hostUsername));
		newHappening.setStart(start);
		newHappening.setEnd(end);
		newHappening.setCategory(happeningCategoryRepo.findFirstByCategoryID(categoryID));
		happeningRepo.save(newHappening);

		return showHappenings(model, authentication);
	}

	@Secured({ "ROLE_HOST" })
	@PostMapping("/modifyExistingHappening")
	public String modifyExistingHappening(Happening newHappening, @RequestParam(value = "host") String hostUsername,
			@RequestParam(value = "startDate") String startAsString,
			@RequestParam(value = "endDate") String endAsString, @RequestParam(value = "categoryID") int categoryID,
			@RequestParam(value = "statusString") String happeningStatus, Model model, Authentication authentication)
			throws ParseException {

		// Check if user is newOwner of Happening is logged in user or logged in user is
		// ADMIN
		if (!(authentication.getName().equals(hostUsername)
				|| authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")))) {
			model.addAttribute("warningMessage", "Happening not found or no permission!");
			return showHappenings(model, authentication);
		}

		// Set correct connection objects
		Calendar now = Calendar.getInstance();
		now.set(Calendar.HOUR_OF_DAY, 0);
		now.set(Calendar.MINUTE, 0);
		now.set(Calendar.SECOND, 0);
		now.set(Calendar.MILLISECOND, 0);

		SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy hh:mm");
		Calendar start = Calendar.getInstance();
		start.setTime(format.parse(startAsString));

		Calendar end = Calendar.getInstance();
		end.setTime(format.parse(endAsString));

		// Check if start date before "trimmed now" (Just date, time does not matter)
		// Check if end-Date is after start date
		if (start.before(now) || end.before(start)) {
			model.addAttribute("warningMessage",
					"Happening not allwed to start in past Or End is not allowed to be before start!");
			return showHappenings(model, authentication);
		}

		// Set correct connection objects
		newHappening.setHappeningStatus(happeningStatusRepo.findFirstByStatusName(happeningStatus));
		newHappening.setHappeningHost(userRepo.findFirstByUsername(hostUsername));
		newHappening.setCategory(happeningCategoryRepo.findFirstByCategoryID(categoryID));
		happeningRepo.saveAndFlush(newHappening);

		return showHappenings(model, authentication);
	}

	@Secured({ "ROLE_HOST" })
	@GetMapping("/deleteExistingHappening")
	public String deleteHappening(Model model, @RequestParam(value = "happeningId") Happening happening,
			Authentication authentication) {

		// Check if user is Owner of Happening or has role ADMIN
		if (!isHappeningHostOrAdmin(happening, authentication)) {
			model.addAttribute("warningMessage", "Happening not found or no permission!");
			return showHappenings(model, authentication);
		}

		happening.setHappeningStatus(happeningStatusRepo.findFirstByStatusName("DELETED"));
		happeningRepo.save(happening);
		return showHappenings(model, authentication);
	}

	@Secured({ "ROLE_ADMIN" })
	@GetMapping("/reactivateExistingHappening")
	public String reactivateHappening(Model model, @RequestParam(value = "happeningId") Happening happening,
			Authentication authentication) {

		happening.setHappeningStatus(happeningStatusRepo.findFirstByStatusName("ACTIVE"));
		happeningRepo.save(happening);
		return showHappenings(model, authentication);
	}

	@Secured({ "ROLE_HOST" })
	@PostMapping("/filterHappenings")
	public String filterHappenings(Model model, @RequestParam String searchString) {
		model.addAttribute("happenings", happeningRepo.findByHappeningName(searchString));
		return "happeningManagement";
	}
	// -----------------------------------------------------------------------------------------

	@ExceptionHandler(Exception.class)
	public String handleAllException(Exception ex) {
		ex.printStackTrace();
		return "error";
	}
}