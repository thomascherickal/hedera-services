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

import java.util.List;

public class RegressionConfig {

	private String branch = "";
	private String commit = "";
	private DBConfig db;
	private LogConfig log;
	private ResultConfig result;
	private CloudConfig cloud = null;
	private LocalConfig local = null;
	private List<String> experiments;
	private SlackConfig slack;
	private JvmOptionParametersConfig jvmOptionParametersConfig = null;

	private String name;
	private int eventFilesWriters = 0;
	private boolean uploadToSharePoint = false;
	private boolean useThreadPool = false;
	private String jvmOptions = "";
	private boolean useLifecycleModel = false;


	/**
	 * Gets the total number of nodes set in this config in all regions, or local nodes if its a local test
	 *
	 * @return total number of nodes
	 */
	public int getTotalNumberOfNodes() {
		int total = 0;
		if (cloud != null && cloud.getRegionList() != null) {
			for (RegionList region : cloud.getRegionList()) {
				total += region.getNumberOfNodes();
			}
		}
		if (local != null) {
			total += local.getNumberOfNodes();
		}
		return total;
	}

	/**
	 * Gets the total number of regions
	 *
	 * @return total number of regions
	 */
	public int getTotalNumberOfRegions() {
		int total = 0;
		if (cloud != null && cloud.getRegionList() != null) {
			total = cloud.getRegionList().size();
		}
		return total;
	}

	// ------------ getters and setters

	public DBConfig getDb() {
		return db;
	}

	public void setDb(DBConfig db) {
		this.db = db;
	}

	public LogConfig getLog() {
		return log;
	}

	public void setLog(LogConfig log) {
		this.log = log;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public String getCommit() {
		return commit;
	}

	public void setCommit(String commit) {
		this.commit = commit;
	}

	public List<String> getExperiments() {
		return experiments;
	}

	public void setExperiments(List<String> experiments) {
		this.experiments = experiments;
	}

	public ResultConfig getResult() {
		return result;
	}

	public void setResult(ResultConfig result) {
		this.result = result;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getEventFilesWriters() {
		return eventFilesWriters;
	}

	public void setEventFilesWriters(int eventFilesWriters) {
		this.eventFilesWriters = eventFilesWriters;
	}

	public CloudConfig getCloud() {
		return cloud;
	}

	public void setCloud(CloudConfig cloud) {
		this.cloud = cloud;
	}

	public LocalConfig getLocal() {
		return local;
	}

	public void setLocal(LocalConfig local) {
		this.local = local;
	}

	public SlackConfig getSlack() {
		return slack;
	}

	public void setSlack(SlackConfig slack) {
		this.slack = slack;
	}

	public boolean isUploadToSharePoint() {
		return uploadToSharePoint;
	}

	public void setUploadToSharePoint(boolean uploadToSharePoint) {
		this.uploadToSharePoint = uploadToSharePoint;
	}

	public String getJvmOptions() {
		return jvmOptions;
	}

	public void setJvmOptions(String jvmOptions) {
		this.jvmOptions = jvmOptions;
	}

	public JvmOptionParametersConfig getJvmOptionParametersConfig() {
		return jvmOptionParametersConfig;
	}

	public void setJvmOptionParametersConfig(JvmOptionParametersConfig jvmOptionParametersConfig) {
		this.jvmOptionParametersConfig = jvmOptionParametersConfig;
	}

	public boolean isUseThreadPool() {
		return useThreadPool;
	}

	public void setUseThreadPool(boolean useThreadPool) {
		this.useThreadPool = useThreadPool;
	}
	public boolean isUseLifecycleModel() {
		return useLifecycleModel;
	}

	public void setUseLifecycleModel(boolean useLifecycleModel) {
		this.useLifecycleModel = useLifecycleModel;
	}

}
