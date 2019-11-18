/*
 * (c) 2016-2019 Swirlds, Inc.
 *
 * This software is the confidential and proprietary information of
 * Swirlds, Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Swirlds.
 *
 * SWIRLDS MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SWIRLDS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

package com.swirlds.regression.jsonConfigs;

import com.swirlds.regression.RunType;
import com.swirlds.regression.jsonConfigs.runTypeConfigs.FreezeConfig;
import com.swirlds.regression.jsonConfigs.runTypeConfigs.ReconnectConfig;
import com.swirlds.regression.jsonConfigs.runTypeConfigs.RecoverConfig;
import com.swirlds.regression.jsonConfigs.runTypeConfigs.RestartConfig;
import com.swirlds.regression.validators.ValidatorType;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestConfig implements FileRequirement {

	public List<ValidatorType> validators;
	private String name;
	private String description;
	private int duration;
	private List<SettingsConfig> settings;
	private AppConfig app;
	private List<String> resultFiles;
	private String log4j2File = "log4j2-regression.xml";
	private SavedState startSavedState = null;
	private List<SavedState> startSavedStates = null;

	private ReconnectConfig reconnectConfig = null;
	private RestartConfig restartConfig = null;
	private FreezeConfig freezeConfig = null;
	private RecoverConfig recoverConfig = null;

	public RunType getRunType() {
		if (restartConfig != null) {
			return RunType.RESTART;
		} else if (freezeConfig != null) {
			return RunType.FREEZE;
		} else if (reconnectConfig != null) {
			return RunType.RECONNECT;
		} else if (recoverConfig != null) {
			return RunType.RECOVER;
		}
		return RunType.STANDARD;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public List<SettingsConfig> getSettings() {
		return settings;
	}

	public void setSettings(List<SettingsConfig> settings) {
		this.settings = settings;
	}

	public AppConfig getApp() {
		return app;
	}

	public void setApp(AppConfig appConfig) {
		this.app = appConfig;
	}

	public List<String> getResultFiles() {
		return resultFiles;
	}

	public void setResultFiles(List<String> resultFiles) {
		this.resultFiles = resultFiles;
	}

	public List<ValidatorType> getValidator() {
		return validators;
	}

	public void setValidator(String[] validatorStrings) {
		validators = new LinkedList<>();
		for (String item : validatorStrings) {
			validators.add(ValidatorType.valueOf(item));
		}
	}

	public String getLog4j2File() {
		return log4j2File;
	}

	public void setLog4j2File(String log4j2File) {
		this.log4j2File = log4j2File;
	}

	public SavedState getStartSavedState() {
		return startSavedState;
	}

	public void setStartSavedState(SavedState startSavedState) {
		this.startSavedState = startSavedState;
	}

	public List<SavedState> getStartSavedStates() {
		return startSavedStates;
	}

	public void setStartSavedStates(List<SavedState> startSavedStates) {
		this.startSavedStates = startSavedStates;
	}

	public ReconnectConfig getReconnectConfig() {
		return reconnectConfig;
	}

	public void setReconnectConfig(ReconnectConfig reconnectConfig) {
		this.reconnectConfig = reconnectConfig;
	}

	public RestartConfig getRestartConfig() {
		return restartConfig;
	}

	public void setRestartConfig(RestartConfig restartConfig) {
		this.restartConfig = restartConfig;
	}

	public FreezeConfig getFreezeConfig() {
		return freezeConfig;
	}

	public void setFreezeConfig(FreezeConfig freezeConfig) {
		this.freezeConfig = freezeConfig;
	}

	public RecoverConfig getRecoverConfig() {
		return recoverConfig;
	}

	public void setRecoverConfig(RecoverConfig recoverConfig) {
		this.recoverConfig = recoverConfig;
	}

	@Override
	public List<String> getFilesNeeded() {
		List<String> list = new LinkedList<>();
		add(list, freezeConfig, app);
		return list;
	}

	public SavedState getSavedStateForNode(int nodeIndex, int totalNodes) {
		List<SavedState> all = Stream.of(Collections.singletonList(startSavedState), startSavedStates)
				.filter(Objects::nonNull)
				.flatMap(Collection::stream)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		for (SavedState ss : all) {
			if (ss.getNodeIdentifier().isNodeInGroup(nodeIndex, totalNodes)) {
				return ss;
			}
		}

		return null;
	}
}
