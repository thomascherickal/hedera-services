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

public class CloudConfig {
	private String service;
	private String securityGroup;
	private String securityGroupID;
	private String instanceName;
	private String instanceType;
	private String instanceKey;
	private String keyLocation;
	private String login;
	private List<RegionList> regionList;
	boolean isValid = false;

	boolean isValidConfig() {
		if (isValid) {
			return isValid;
		}
		if (this.service == null || this.service.isEmpty()
				|| this.securityGroup == null || this.securityGroup.isEmpty()
				|| this.securityGroupID == null || this.securityGroupID.isEmpty()
				|| this.instanceName == null || this.instanceName.isEmpty()
				|| this.instanceType == null || this.instanceType.isEmpty()
				|| this.instanceKey == null || this.instanceKey.isEmpty()
				|| this.keyLocation == null || this.keyLocation.isEmpty()
				|| this.login == null || this.login.isEmpty()
				|| this.regionList == null || this.regionList.size() == 0
		) {
			return isValid;
		}
		for(RegionList rl : this.regionList){
			if(!rl.isValid){
				return isValid;
			}
		}
		isValid = true;
		return isValid;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getSecurityGroup() {
		return securityGroup;
	}

	public void setSecurityGroup(String securityGroup) {
		this.securityGroup = securityGroup;
	}

	public String getSecurityGroupID() {
		return securityGroupID;
	}

	public void setSecurityGroupID(String securityGroupID) {
		this.securityGroupID = securityGroupID;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public String getInstanceType() {
		return instanceType;
	}

	public void setInstanceType(String instanceType) {
		this.instanceType = instanceType;
	}

	public String getInstanceKey() {
		return instanceKey;
	}

	public void setInstanceKey(String instanceKey) {
		this.instanceKey = instanceKey;
	}

	public String getKeyLocation() {
		return keyLocation;
	}

	public void setKeyLocation(String keyLocation) {
		this.keyLocation = keyLocation;
	}

	public List<RegionList> getRegionList() {
		return regionList;
	}

	public void setRegionList(List<RegionList> regionList) {
		this.regionList = regionList;
	}
}
