package de.tum.in.www1.artemis.web.rest.science;

import static de.tum.in.www1.artemis.config.Constants.PROFILE_CORE;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.science.ScienceSetting;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.repository.science.ScienceSettingRepository;
import de.tum.in.www1.artemis.security.annotations.EnforceAtLeastStudent;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;

/**
 * REST controller for managing ScienceSettings.
 */

@Profile(PROFILE_CORE)
@RestController
@RequestMapping("api/")
public class ScienceSettingsResource {

    private static final Logger log = LoggerFactory.getLogger(ScienceSettingsResource.class);

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final UserRepository userRepository;

    private final ScienceSettingRepository scienceSettingRepository;

    public ScienceSettingsResource(UserRepository userRepository, ScienceSettingRepository scienceSettingRepository) {
        this.userRepository = userRepository;
        this.scienceSettingRepository = scienceSettingRepository;
    }

    /**
     * GET science-settings : Get all ScienceSettings for current user
     * <p>
     * Fetches the ScienceSettings for the current user from the server.
     *
     * @return the list of found ScienceSettings
     */
    @GetMapping("science-settings")
    @EnforceAtLeastStudent
    public ResponseEntity<Set<ScienceSetting>> getScienceSettingsForCurrentUser() {
        User user = userRepository.getUser();
        log.debug("REST request to get all ScienceSettings for current user {}", user);
        Set<ScienceSetting> scienceSettings = scienceSettingRepository.findAllByUserId(user.getId());
        return new ResponseEntity<>(scienceSettings, HttpStatus.OK);
    }

    /**
     * PUT science-settings : Save ScienceSettings for current user
     * <p>
     * Saves the provided ScienceSettings to the server.
     *
     * @param scienceSettings which should be saved to the scienceSetting database.
     * @return the ScienceSettings that just got saved for the current user as array
     *         200 for a successful execution, 400 if the user provided empty settings to save, 500 if the save call returns empty settings
     */
    @PutMapping("science-settings")
    @EnforceAtLeastStudent
    public ResponseEntity<ScienceSetting[]> saveScienceSettingsForCurrentUser(@NotNull @RequestBody ScienceSetting[] scienceSettings) {
        if (scienceSettings.length == 0) {
            throw new BadRequestAlertException("Cannot save non-existing Science Settings", "ScienceSettings", "scienceSettingsEmpty");
        }
        User user = userRepository.getUserWithGroupsAndAuthorities();
        log.debug("REST request to save ScienceSettings : {} for current user {}", scienceSettings, user);
        List<ScienceSetting> scienceSettingList = Arrays.stream(scienceSettings).peek(scienceSetting -> scienceSetting.setUser(user)).toList();
        List<ScienceSetting> persistedSettingList = scienceSettingRepository.saveAll(scienceSettingList);
        if (persistedSettingList.isEmpty()) {
            throw new BadRequestAlertException("Error occurred during saving of Science Settings", "ScienceSettings", "scienceSettingsEmptyAfterSave");
        }
        ScienceSetting[] persistedSettingArray = persistedSettingList.toArray(new ScienceSetting[0]);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, "scienceSetting", "test")).body(persistedSettingArray);
    }
}
