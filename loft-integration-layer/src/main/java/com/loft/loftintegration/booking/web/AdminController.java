package com.loft.loftintegration.booking.web;

import com.loft.loftintegration.booking.model.*;
import com.loft.loftintegration.booking.repository.ActivityTypeRepository;
import com.loft.loftintegration.booking.repository.ResourceRepository;
import com.loft.loftintegration.booking.repository.ShiftTemplateRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Admin controller for managing the catalog (ActivityTypes), resources, and shift templates.
 * This provides a staff-facing admin experience beyond the static guest booking page.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ActivityTypeRepository activityTypeRepository;
    private final ResourceRepository resourceRepository;
    private final ShiftTemplateRepository shiftTemplateRepository;

    public AdminController(ActivityTypeRepository activityTypeRepository,
                           ResourceRepository resourceRepository,
                           ShiftTemplateRepository shiftTemplateRepository) {
        this.activityTypeRepository = activityTypeRepository;
        this.resourceRepository = resourceRepository;
        this.shiftTemplateRepository = shiftTemplateRepository;
    }

    @GetMapping
    public String adminDashboard(Model model) {
        model.addAttribute("pageTitle", "Admin Dashboard - Loft");
        return "admin/dashboard";
    }

    // ========== Activity Types (Catalog) Management ==========

    @GetMapping("/catalog")
    public String listActivityTypes(@RequestParam(defaultValue = "LOFT") String propertyCode, Model model) {
        List<ActivityType> activityTypes = activityTypeRepository.findByPropertyCode(propertyCode);
        model.addAttribute("activityTypes", activityTypes);
        model.addAttribute("propertyCode", propertyCode);
        model.addAttribute("pageTitle", "Manage Catalog - Loft");
        return "admin/catalog";
    }

    @GetMapping("/catalog/new")
    public String newActivityTypeForm(Model model) {
        model.addAttribute("activityType", new ActivityTypeForm());
        model.addAttribute("categories", ActivityCategory.values());
        model.addAttribute("pageTitle", "New Activity Type - Loft");
        return "admin/activity-type-form";
    }

    @PostMapping("/catalog/save")
    public String saveActivityType(@ModelAttribute ActivityTypeForm form, RedirectAttributes redirectAttributes) {
        ActivityType activityType;
        if (form.getId() != null) {
            activityType = activityTypeRepository.findById(form.getId())
                .orElseThrow(() -> new IllegalArgumentException("ActivityType not found"));
        } else {
            activityType = new ActivityType(
                "LOFT",
                form.getName(),
                ActivityCategory.valueOf(form.getCategory()),
                form.getDefaultDurationMinutes(),
                BigDecimal.valueOf(form.getPrice()),
                form.getCurrency()
            );
        }
        activityTypeRepository.save(activityType);
        redirectAttributes.addFlashAttribute("successMessage", "Activity type saved successfully");
        return "redirect:/admin/catalog";
    }

    @GetMapping("/catalog/edit/{id}")
    public String editActivityType(@PathVariable Long id, Model model) {
        ActivityType activityType = activityTypeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ActivityType not found"));
        ActivityTypeForm form = new ActivityTypeForm();
        form.setId(activityType.getId());
        form.setName(activityType.getName());
        form.setCategory(activityType.getCategory().name());
        form.setDefaultDurationMinutes(activityType.getDefaultDurationMinutes());
        form.setPrice(activityType.getPrice().doubleValue());
        form.setCurrency(activityType.getCurrency());
        model.addAttribute("activityType", form);
        model.addAttribute("categories", ActivityCategory.values());
        model.addAttribute("pageTitle", "Edit Activity Type - Loft");
        return "admin/activity-type-form";
    }

    @PostMapping("/catalog/delete/{id}")
    public String deleteActivityType(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        activityTypeRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Activity type deleted successfully");
        return "redirect:/admin/catalog";
    }

    // ========== Resources Management ==========

    @GetMapping("/resources")
    public String listResources(@RequestParam(defaultValue = "LOFT") String propertyCode, Model model) {
        List<Resource> allResources = resourceRepository.findAll();
        List<Resource> resources = allResources.stream()
            .filter(r -> r.getPropertyCode().equals(propertyCode))
            .collect(Collectors.toList());
        model.addAttribute("resources", resources);
        model.addAttribute("propertyCode", propertyCode);
        model.addAttribute("resourceTypes", ResourceType.values());
        model.addAttribute("pageTitle", "Manage Resources - Loft");
        return "admin/resources";
    }

    @GetMapping("/resources/new")
    public String newResourceForm(Model model) {
        model.addAttribute("resource", new ResourceForm());
        model.addAttribute("resourceTypes", ResourceType.values());
        model.addAttribute("pageTitle", "New Resource - Loft");
        return "admin/resource-form";
    }

    @PostMapping("/resources/save")
    public String saveResource(@ModelAttribute ResourceForm form, RedirectAttributes redirectAttributes) {
        Resource resource;
        if (form.getId() != null) {
            resource = resourceRepository.findById(form.getId())
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));
        } else {
            resource = new Resource(
                "LOFT",
                ResourceType.valueOf(form.getResourceType()),
                form.getName()
            );
        }
        resource.setActive(form.isActive());
        resourceRepository.save(resource);
        redirectAttributes.addFlashAttribute("successMessage", "Resource saved successfully");
        return "redirect:/admin/resources";
    }

    @GetMapping("/resources/edit/{id}")
    public String editResource(@PathVariable Long id, Model model) {
        Resource resource = resourceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Resource not found"));
        ResourceForm form = new ResourceForm();
        form.setId(resource.getId());
        form.setResourceType(resource.getResourceType().name());
        form.setName(resource.getName());
        form.setActive(resource.isActive());
        model.addAttribute("resource", form);
        model.addAttribute("resourceTypes", ResourceType.values());
        model.addAttribute("pageTitle", "Edit Resource - Loft");
        return "admin/resource-form";
    }

    @PostMapping("/resources/delete/{id}")
    public String deleteResource(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        resourceRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Resource deleted successfully");
        return "redirect:/admin/resources";
    }

    // ========== Shift Templates Management ==========

    @GetMapping("/shifts")
    public String listShiftTemplates(Model model) {
        List<ShiftTemplate> shifts = shiftTemplateRepository.findAll();
        model.addAttribute("shifts", shifts);
        model.addAttribute("pageTitle", "Manage Shift Templates - Loft");
        return "admin/shifts";
    }

    @GetMapping("/shifts/new")
    public String newShiftTemplateForm(Model model) {
        model.addAttribute("shiftTemplate", new ShiftTemplateForm());
        model.addAttribute("resources", resourceRepository.findByPropertyCodeAndResourceTypeAndActiveTrue("LOFT", ResourceType.STAFF));
        model.addAttribute("daysOfWeek", Arrays.stream(DayOfWeek.values()).map(Enum::name).collect(Collectors.toList()));
        model.addAttribute("pageTitle", "New Shift Template - Loft");
        return "admin/shift-template-form";
    }

    @PostMapping("/shifts/save")
    public String saveShiftTemplate(@ModelAttribute ShiftTemplateForm form, RedirectAttributes redirectAttributes) {
        ShiftTemplate shiftTemplate;
        if (form.getId() != null) {
            shiftTemplate = shiftTemplateRepository.findById(form.getId())
                .orElseThrow(() -> new IllegalArgumentException("ShiftTemplate not found"));
        } else {
            Resource staffResource = resourceRepository.findById(form.getResourceId())
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));
            
            Set<DayOfWeek> days = form.getDaysOfWeek().stream()
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());
            
            shiftTemplate = new ShiftTemplate(
                staffResource,
                form.getName(),
                days,
                LocalTime.parse(form.getStartTime()),
                LocalTime.parse(form.getEndTime())
            );
        }
        shiftTemplateRepository.save(shiftTemplate);
        redirectAttributes.addFlashAttribute("successMessage", "Shift template saved successfully");
        return "redirect:/admin/shifts";
    }

    @GetMapping("/shifts/edit/{id}")
    public String editShiftTemplate(@PathVariable Long id, Model model) {
        ShiftTemplate shift = shiftTemplateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ShiftTemplate not found"));
        ShiftTemplateForm form = new ShiftTemplateForm();
        form.setId(shift.getId());
        form.setResourceId(shift.getResource().getId());
        form.setName(shift.getName());
        form.setDaysOfWeek(shift.getDaysOfWeek().stream().map(Enum::name).collect(Collectors.toSet()));
        form.setStartTime(shift.getStartTime().toString());
        form.setEndTime(shift.getEndTime().toString());
        model.addAttribute("shiftTemplate", form);
        model.addAttribute("resources", resourceRepository.findByPropertyCodeAndResourceTypeAndActiveTrue("LOFT", ResourceType.STAFF));
        model.addAttribute("daysOfWeek", Arrays.stream(DayOfWeek.values()).map(Enum::name).collect(Collectors.toList()));
        model.addAttribute("pageTitle", "Edit Shift Template - Loft");
        return "admin/shift-template-form";
    }

    @PostMapping("/shifts/delete/{id}")
    public String deleteShiftTemplate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        shiftTemplateRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Shift template deleted successfully");
        return "redirect:/admin/shifts";
    }
}
