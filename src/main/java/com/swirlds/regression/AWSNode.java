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
	private AmazonEC2 ec2;
	private String region;
	private int totalNodes;
	private int totalPreexistingInstances;
	private RunInstancesRequest runInstanceRequest;
	private ArrayList<Instance> instances;
	private ArrayList<String> instanceIDs;

	private ArrayList<String> publicIPList;
	private ArrayList<String> privateIPList;
	private ArrayList<String> publicIPTestClientList;
	private ArrayList<String> privateIPTestClientList;
	private boolean isExistingInstance = false;
	/**
	 * Boolean to set to true if the node is a test client node, which is by default false
	 */
	private boolean isTestClientNode = false;

	public AWSNode(RegionList rl) {
		new AWSNode(rl, false);
	}

	public AWSNode(RegionList rl, boolean isTestClientNode) throws NullPointerException {
		if (rl == null) {
			throw new NullPointerException("RegionList was null");
		}
		this.region = rl.getRegion();
		this.ec2 = AmazonEC2ClientBuilder.standard().withRegion(this.region).build();
		this.isTestClientNode = isTestClientNode;
		instanceIDs = new ArrayList<>();
		if (!isTestClientNode) {
			this.totalNodes = rl.getNumberOfNodes();
		} else {
			this.totalNodes = rl.getNumberOfTestClientNodes();
		}
		if (rl.getInstanceList() != null) {
			instanceIDs.addAll(Arrays.asList(rl.getInstanceList()));
			totalPreexistingInstances = rl.getInstanceList().length;
			isExistingInstance = true;
		}

		if (this.isTestClientNode && rl.getTestClientInstanceList() != null) {
			instanceIDs.addAll(Arrays.asList(rl.getTestClientInstanceList()));
			// TODO check if totalPreexistingInstances should be different
			totalPreexistingInstances = rl.getTestClientInstanceList().length;
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

	public int getTotalPreexistingInstances() {
		return totalPreexistingInstances;
	}

	public RunInstancesRequest getRunInstanceRequest() {
		return runInstanceRequest;
	}

	public void setRunInstanceRequest(RunInstancesRequest runInstanceRequest) {
		this.runInstanceRequest = runInstanceRequest;
	}

	public boolean hasInstances() {
		return (instances != null && instances.size() > 0 && hasInstanceIDs());
	}

	public boolean hasInstanceIDs() {
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

	/* returns all non-preexisting nodes that need to be shutdown at the end of the regression run. */
	public ArrayList<String> getInstanceIDsToDelete() {
		ArrayList<String> returnList = new ArrayList<String>();
		returnList.addAll(instanceIDs.subList(totalPreexistingInstances, instanceIDs.size()));
		return returnList;
	}

	public void setInstanceIDs(ArrayList<String> instanceIDs) {
		this.instanceIDs.addAll(instanceIDs);
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

	public boolean isExistingInstance() {
		return isExistingInstance;
	}

	public boolean isTestClientNode() {
		return isTestClientNode;
	}

	public void setTestClientNode(boolean testClientNode) {
		isTestClientNode = testClientNode;
	}

	public ArrayList<String> getPublicIPTestClientList() {
		return publicIPTestClientList;
	}

	public void setPublicIPTestClientList(ArrayList<String> publicIPTestClientList) {
		this.publicIPTestClientList = publicIPTestClientList;
	}

	public ArrayList<String> getPrivateIPTestClientList() {
		return privateIPTestClientList;
	}

	public void setPrivateIPTestClientList(ArrayList<String> privateIPTestClientList) {
		this.privateIPTestClientList = privateIPTestClientList;
	}

}
