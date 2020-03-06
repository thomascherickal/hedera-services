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

package com.swirlds.regression;


import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.swirlds.regression.jsonConfigs.RegionList;

import java.util.ArrayList;
import java.util.Arrays;

public class AWSNode {
	AmazonEC2 ec2;
	String region;
	int totalNodes;
	private RunInstancesRequest runInstanceRequest;
	private ArrayList<Instance> instances;
	private ArrayList<String> instanceIDs;
	private ArrayList<AmazonEC2> ec2List;

	private ArrayList<String> publicIPList;
	private ArrayList<String> privateIPList;
	private String amiid;
	private boolean isExistingInstance = false;
	private NodeMemory nodeMemory;

	public AWSNode(RegionList rl) throws NullPointerException {
		if (rl == null) {
			throw new NullPointerException("RegionList was null");
		}
		this.region = rl.getRegion();
		this.ec2 = AmazonEC2ClientBuilder.standard().withRegion(this.region).build();
		this.totalNodes = rl.getNumberOfNodes();
		if (rl.getInstanceList() != null) {
			instanceIDs = new ArrayList<>();
			instanceIDs.addAll(Arrays.asList(rl.getInstanceList()));
			isExistingInstance = true;
		}

	}

/*	public AWSNode(String region, int totalNodes, AmazonEC2 ec2) {
		this.region = region;
		this.totalNodes = totalNodes;
		this.ec2 = ec2;
	}

	public AWSNode(String region, String[] instanceList, AmazonEC2 ec2) {
		this.region = region;
		this.totalNodes = instanceList.length;
		this.ec2 = ec2;
		instanceIDs = new ArrayList<>();
		instanceIDs.addAll(Arrays.asList(instanceList));
		isExistingInstance = true;
	}*/

	public AmazonEC2 getEc2() {
		return ec2;
	}

	public void setEc2(AmazonEC2 ec2) {
		this.ec2 = ec2;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public int getTotalNodes() {
		return totalNodes;
	}

	public void setTotalNodes(int totalNodes) {
		this.totalNodes = totalNodes;
	}

	public RunInstancesRequest getRunInstanceRequest() {
		return runInstanceRequest;
	}

	public void setRunInstanceRequest(RunInstancesRequest runInstanceRequest) {
		this.runInstanceRequest = runInstanceRequest;
	}

	public boolean hasInstances() {
		return (instances != null && instances.size() > 0 && hasInstanceIDs() );
	}

	public boolean hasInstanceIDs(){
		return (instanceIDs != null && instanceIDs.size() > 0);
	}

	public ArrayList<Instance> getInstances() {
		return instances;
	}

	public void setInstances(ArrayList<Instance> instances) {
		this.instances = instances;
	}

	public ArrayList<String> getInstanceIDs() {
		return instanceIDs;
	}

	public void setInstanceIDs(ArrayList<String> instanceIDs) {
		this.instanceIDs = instanceIDs;
	}

	public ArrayList<AmazonEC2> getEc2List() {
		return ec2List;
	}

	public void setEc2List(ArrayList<AmazonEC2> ec2List) {
		this.ec2List = ec2List;
	}

	public ArrayList<String> getPublicIPList() {
		return publicIPList;
	}

	public void setPublicIPList(ArrayList<String> publicIPList) {
		this.publicIPList = publicIPList;
	}

	public ArrayList<String> getPrivateIPList() {
		return privateIPList;
	}

	public void setPrivateIPList(ArrayList<String> privateIPList) {
		this.privateIPList = privateIPList;
	}

	public String getAmiid() {
		return amiid;
	}

	public void setAmiid(String amiid) {
		this.amiid = amiid;
	}

	public boolean isExistingInstance() {
		return isExistingInstance;
	}
}
