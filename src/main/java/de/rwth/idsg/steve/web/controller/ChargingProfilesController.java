package de.rwth.idsg.steve.web.controller;

import de.rwth.idsg.steve.repository.ChargingProfileRepository;
import de.rwth.idsg.steve.repository.dto.ChargingProfile;
import de.rwth.idsg.steve.utils.DateTimeUtils;
import de.rwth.idsg.steve.web.dto.ChargingProfileForm;
import de.rwth.idsg.steve.web.dto.ChargingProfileQueryForm;
import jooq.steve.db.tables.records.ChargingProfileRecord;
import jooq.steve.db.tables.records.ChargingSchedulePeriodRecord;
import ocpp.cp._2015._10.ChargingProfileKindType;
import ocpp.cp._2015._10.ChargingProfilePurposeType;
import ocpp.cp._2015._10.ChargingRateUnitType;
import ocpp.cp._2015._10.RecurrencyKindType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 12.11.2018
 */
@Controller
@RequestMapping(value = "/manager/chargingProfiles")
public class ChargingProfilesController {

    @Autowired private ChargingProfileRepository repository;

    private static final String PARAMS = "params";

    // -------------------------------------------------------------------------
    // Paths
    // -------------------------------------------------------------------------

    private static final String QUERY_PATH = "/query";

    private static final String DETAILS_PATH = "/details/{chargingProfilePk}";
    private static final String DELETE_PATH = "/delete/{chargingProfilePk}";
    private static final String UPDATE_PATH = "/update";
    private static final String ADD_PATH = "/add";

    // -------------------------------------------------------------------------
    // HTTP methods
    // -------------------------------------------------------------------------

    @RequestMapping(method = RequestMethod.GET)
    public String getOverview(Model model) {
        ChargingProfileQueryForm queryForm = new ChargingProfileQueryForm();
        model.addAttribute(PARAMS, queryForm);
        initList(queryForm, model);
        return "data-man/chargingProfiles";
    }

    @RequestMapping(value = QUERY_PATH, method = RequestMethod.GET)
    public String getQuery(@ModelAttribute(PARAMS) ChargingProfileQueryForm queryForm, Model model) {
        initList(queryForm, model);
        return "data-man/chargingProfiles";
    }

    @RequestMapping(value = ADD_PATH, method = RequestMethod.GET)
    public String addGet(Model model) {
        model.addAttribute("form", new ChargingProfileForm());
        return "data-man/chargingProfileAdd";
    }

    @RequestMapping(params = "add", value = ADD_PATH, method = RequestMethod.POST)
    public String addPost(@Valid @ModelAttribute("form") ChargingProfileForm form,
                          BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "data-man/chargingProfileAdd";
        }

        repository.add(form);
        return toOverview();
    }

    @RequestMapping(params = "update", value = UPDATE_PATH, method = RequestMethod.POST)
    public String update(@Valid @ModelAttribute("form") ChargingProfileForm form,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "data-man/chargingProfileDetails";
        }

        repository.update(form);
        return toOverview();
    }

    @RequestMapping(params = "backToOverview", value = ADD_PATH, method = RequestMethod.POST)
    public String addBackToOverview() {
        return toOverview();
    }

    @RequestMapping(params = "backToOverview", value = UPDATE_PATH, method = RequestMethod.POST)
    public String updateBackToOverview() {
        return toOverview();
    }

    @RequestMapping(value = DELETE_PATH, method = RequestMethod.POST)
    public String delete(@PathVariable("chargingProfilePk") int chargingProfilePk) {
        repository.delete(chargingProfilePk);
        return toOverview();
    }

    @RequestMapping(value = DETAILS_PATH, method = RequestMethod.GET)
    public String getDetails(@PathVariable("chargingProfilePk") int chargingProfilePk, Model model) {
        ChargingProfile.Details details = repository.getDetails(chargingProfilePk);

        ChargingProfileRecord profile = details.getProfile();
        List<ChargingSchedulePeriodRecord> periods = details.getPeriods();

        ChargingProfileForm form = new ChargingProfileForm();
        form.setChargingProfilePk(profile.getChargingProfilePk());
        form.setDescription(profile.getDescription());
        form.setNote(profile.getNote());
        form.setStackLevel(profile.getStackLevel());
        form.setChargingProfilePurpose(ChargingProfilePurposeType.fromValue(profile.getChargingProfilePurpose()));
        form.setChargingProfileKind(ChargingProfileKindType.fromValue(profile.getChargingProfileKind()));
        form.setRecurrencyKind(profile.getRecurrencyKind() == null ? null : RecurrencyKindType.fromValue(profile.getRecurrencyKind()));
        form.setValidFrom(DateTimeUtils.toLocalDateTime(profile.getValidFrom()));
        form.setValidTo(DateTimeUtils.toLocalDateTime(profile.getValidTo()));
        form.setDurationInSeconds(profile.getDurationInSeconds());
        form.setStartSchedule(DateTimeUtils.toLocalDateTime(profile.getStartSchedule()));
        form.setChargingRateUnit(ChargingRateUnitType.fromValue(profile.getChargingRateUnit()));
        form.setMinChargingRate(profile.getMinChargingRate());

        List<ChargingProfileForm.SchedulePeriod> formPeriods =
                periods.stream()
                       .map(k -> {
                           ChargingProfileForm.SchedulePeriod p = new ChargingProfileForm.SchedulePeriod();
                           p.setStartPeriodInSeconds(k.getStartPeriodInSeconds());
                           p.setPowerLimitInAmperes(k.getPowerLimitInAmperes());
                           p.setNumberPhases(k.getNumberPhases());
                           return p;
                       })
                       .collect(Collectors.toList());

        form.setSchedulePeriods(formPeriods);

        model.addAttribute("form", form);

        return "data-man/chargingProfileDetails";
    }

    private void initList(ChargingProfileQueryForm queryForm, Model model) {
        model.addAttribute("profileList", repository.getOverview(queryForm));
    }

    private String toOverview() {
        return "redirect:/manager/chargingProfiles";
    }
}