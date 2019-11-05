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
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.ImportKeyPairRequest;
import com.amazonaws.services.ec2.model.ImportKeyPairResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.ResourceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.util.Base64;
import com.swirlds.regression.jsonConfigs.CloudConfig;
import com.swirlds.regression.jsonConfigs.RegionList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

class CloudService {
	private static final Logger log = LogManager.getLogger(Experiment.class);
	private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

	CloudConfig cloudConfig;
	RunInstancesRequest runInstanceRequest;
	ArrayList<Instance> instances;
	ArrayList<AWSNode> ec2List;

	boolean instancesRunning = false;

	CloudService(CloudConfig cloudConfig) {
		this.cloudConfig = cloudConfig;
		instances = new ArrayList<>();
		runInstanceRequest = new RunInstancesRequest();
		ec2List = new ArrayList<>();

		for (RegionList reg : cloudConfig.getRegionList()) {
			if (reg.getInstanceList() != null || reg.getNumberOfNodes() >= 0) {
				log.info(MARKER, "Getting existing nodes {}", reg.getNumberOfNodes());
				ec2List.add(new AWSNode(reg));
			} else {
				log.error(ERROR, "Cloud config must have either number of nodes or an array of instances");
				System.exit(0);
			}
		}
	}

	/**
	 * prints out details about instances that were created for this cloud service object
	 */
	void reportInstanceDetails() {
		for (Instance instance : instances) {
			log.info(MARKER, "Instance:{}\t Status:{}\t public IP:{}\t private IP:{}", instance.getInstanceId()
					, instance.getState().getName()
					, instance.getPublicIpAddress()
					, instance.getPrivateIpAddress()
			);
		}
	}

	void startService( String nameTag) {
		populateInstances(nameTag);
		reportInstanceDetails();
	}

	/**
	 * Take the public key identified in the regression configuration and reads it in. then it based64 encodes it so
	 * cloud service can register the key
	 *
	 * @return base64encoded public key
	 */
	String readInPublicKey() {

		String returnStr = Base64.encodeAsString(readInPublicKeyString().getBytes());
		return returnStr;
	}

	/**
	 * Take the public key identified in the regression configuration and reads it in.
	 *
	 * @return plain text public key
	 */

	String readInPublicKeyString(){
		StringBuilder strBuilder = new StringBuilder();
		try (Stream<String> stream = Files.lines(Paths.get(cloudConfig.getKeyLocation() + ".pub"),
				StandardCharsets.UTF_8)) {
			stream.forEach(s -> {
				if (!s.contains("-----")) {
					strBuilder.append(s);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
//		String returnStr = Base64.encodeAsString(strBuilder.toString().getBytes());

		String returnStr = strBuilder.toString();
		return returnStr;
	}

	/**
	 * Create a new key in the cloud service to be used when connecting to any instances
	 *
	 * @param region
	 * 		- region key is used for
	 * @param keyName
	 * 		- name of the key (this is set in the regression configuratoin file)
	 * @return true if key was accepted by cloud service, false if not.
	 */
	boolean createNewKey(String region, String keyName) {
		AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion(region).build();

		ImportKeyPairRequest keyRequest = new ImportKeyPairRequest(keyName, readInPublicKey());
		ImportKeyPairResult keyResult = ec2.importKeyPair(keyRequest);

		printAvailableKeyPairs();

		return keyResult.getKeyName().equals(keyName);
	}

	/**
	 * checks all created instances to find out if instances are up and running
	 *
	 * @return true if all instances are running, false if not
	 */
	boolean isInstanceReady() {
		if (instancesRunning) {
			return true;
		}
		if (instances.size() == 0) {
			return false;
		} else {
			for (AWSNode node : ec2List) {
				ArrayList<String> publicIPList = new ArrayList<>();
				ArrayList<String> privateIPList = new ArrayList<>();
				log.info(MARKER, "instance IDs: {}", node.getInstanceIDs().toArray());
				for (Reservation rez : getInstanceDescriptions(node)) {
					for (Instance inst : rez.getInstances()) {
						if (!inst.getState().getName().equalsIgnoreCase(InstanceStateName.Running.toString())) {
							log.info(MARKER,
									"instance state:{}\trunning name:{}\trunning string:{}", inst.getState().getName(),
									InstanceStateName.Running.name(), InstanceStateName.Running.toString());
							return false;
						} else {
							log.info(MARKER,
									"instance state:{}\trunning string:{}\tpublic ip:{}", inst.getState().getName(),
									InstanceStateName.Running.toString(),
									inst.getPublicIpAddress());
							publicIPList.add(inst.getPublicIpAddress());
							privateIPList.add(inst.getPrivateIpAddress());
						}
					}
				}
				node.setPublicIPList(publicIPList);
				node.setPrivateIPList(privateIPList);
			}
		}
		instancesRunning = true;
		return true;
	}

	/**
	 * Looks through list of keys that cloud service has listed for region to find if the key passed in is available
	 *
	 * @param keyResponce
	 * 		- list of keys cloud serice has on record
	 * @param keyName
	 * 		- name of key to be used to create instances
	 * @return true if key exists, false if not
	 */
	private boolean isKeyAvailable(DescribeKeyPairsResult keyResponce, String keyName) {
		for (KeyPairInfo keyPair : keyResponce.getKeyPairs()) {
			if (keyName.equals(keyPair.getKeyName())) {
				log.trace(MARKER, "Key was found {} matches {}", keyName, keyPair.getKeyName());
				return true;
			}
		}
		return false;
	}

	/**
	 * Check all regions in regression config file to see if the key is registered. create key if the key is not
	 * found in
	 * a specific region
	 *
	 * @param keyName
	 * 		- name of key to be used in instance creation
	 * @return true if keys existed or could be created in all regions, false if key was not found and could not be
	 * 		created
	 * 		in a region.
	 */
	boolean checkKeyPair(String keyName) {
		for (AWSNode node : ec2List) {
			AmazonEC2 ec2 = node.getEc2();
			String region = node.getRegion();

			DescribeKeyPairsResult keyResponce = ec2.describeKeyPairs();
			if (!isKeyAvailable(keyResponce, keyName)) {
				if (!createNewKey(region, keyName)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * prints out all keys available in the regions indicated in the region config file
	 */
	void printAvailableKeyPairs() {
		for (AWSNode node : ec2List) {
			AmazonEC2 ec2 = node.getEc2();
			String region = node.getRegion();

			DescribeKeyPairsResult keyResponce = ec2.describeKeyPairs();

			log.trace(MARKER, "Region: {}", region);
			for (KeyPairInfo keyPair : keyResponce.getKeyPairs()) {
				log.trace(MARKER,
						"Found key pair with name {} and fingerprint {}",
						keyPair.getKeyName(),
						keyPair.getKeyFingerprint());
			}
		}
	}

	private List<Reservation> getInstanceDescriptions(AWSNode node) {
		DescribeInstancesRequest instRequest = new DescribeInstancesRequest().withInstanceIds(
				node.getInstanceIDs());
		DescribeInstancesResult instResult = node.getEc2().describeInstances(instRequest);

		return instResult.getReservations();
	}

	void getExistingInstance(AWSNode node, String nameTag) {
		for (Reservation rez : getInstanceDescriptions(node)) {
			for (Instance inst : rez.getInstances()) {
				instances.add(inst);
			}
		/*	if (instances.size() != node.getTotalNodes()) {
				startInstance(node,nameTag);
			} */
		}
	}

	void populateInstances(String nameTag) {
		for (AWSNode node : ec2List) {
			if (node.hasInstanceIDs()) {
				log.trace(MARKER, "Specific instances were requested");
				getExistingInstance(node,nameTag);
			} else {
				log.trace(MARKER, "New instances requested");
				startInstance(node,nameTag);
			}
		}
	}

	/**
	 * start instances in each region according to the number of nodes each region has set in the regression config file
	 */
	void startInstance(AWSNode node, String nameTag) {
		ArrayList<String> instanceIDs = new ArrayList<>();
		AmazonEC2 ec2 = node.getEc2();
		int numberOfNodes = node.getTotalNodes();
		checkKeyPair(cloudConfig.getInstanceKey());

		String instanceID = getSwirldsAAMIID(ec2, cloudConfig.getInstanceName());
		if (instanceID == null || instanceID.isEmpty()) {
			// TODO if no AMI in region find a region that has it and copy it to current region.
			log.error(ERROR, "AMI doesn't exist in {}. Exiting program.", node.getRegion());
			destroyInstances();
			System.exit(-1);
		}
		List<Tag> tags = new ArrayList<>();
		tags.add(new Tag("Name", nameTag));
		TagSpecification tag = new TagSpecification();
		tag.setTags(tags);
		tag.setResourceType(ResourceType.Instance);

		try {
			runInstanceRequest = new RunInstancesRequest();
				runInstanceRequest.withImageId(instanceID)
						.withInstanceType(InstanceType.valueOf(cloudConfig.getInstanceType()))
						.withMinCount(numberOfNodes)
						.withMaxCount(numberOfNodes)
						.withSecurityGroups(cloudConfig.getSecurityGroup())
						.withKeyName(cloudConfig.getInstanceKey())
						.withTagSpecifications(tag);
			RunInstancesResult runResult = ec2.runInstances(runInstanceRequest);
			for (Instance inst : runResult.getReservation().getInstances()) {
				instances.add(inst);
				instanceIDs.add(inst.getInstanceId());
			}
		} catch (AmazonEC2Exception e) {
			log.error(ERROR, "Amazon could not create instances", e);
			destroyInstances();
			System.exit(-3);
		}
		node.setInstanceIDs(instanceIDs);
	}

	/**
	 * destroy all instances set up by this service in all regions.
	 */
	void destroyInstances() {
		//TODO make autocloseable add to Cleaner

		for (AWSNode node : ec2List) {
			if(node.hasInstanceIDs()) {
				if (!node.isExistingInstance()) {
					AmazonEC2 ec2 = node.getEc2();
					TerminateInstancesRequest deleteRequest = new TerminateInstancesRequest();
					deleteRequest.setInstanceIds(node.getInstanceIDs());

					ec2.terminateInstances(deleteRequest);
				} else {
					setInstanceNames("RegressionNotRunning");
				}
			}
		}

	}

	/**
	 * look for security group in each region and if it isn't there create it
	 */
	void getOrCreateSecurityGroup() {
	}

	/**
	 * Get AMI ID for specific region
	 */
	String getSwirldsAAMIID(AmazonEC2 ec2, String amiName) {
		DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest().withFilters(
				new Filter().withName("name").withValues(amiName));
		DescribeImagesResult describeImagesResult = ec2.describeImages(describeImagesRequest);

		ArrayList<Image> images = (ArrayList<Image>) describeImagesResult.getImages();
		if (images == null || images.isEmpty()) {
			return null;
		}
		for (Image image : images) {
			log.trace(MARKER, "{} == {} == {}",
					image.getImageId(), image.getImageLocation(), image.getName());
		}
		return images.get(0).getImageId();

	}


	/**
	 * @return return list of public IPs of all nodes set up by this service
	 */
	public ArrayList<String> getPublicIPList() {
		ArrayList<String> publicIPList = new ArrayList<>();
		for (AWSNode node : ec2List) {
			publicIPList.addAll(node.getPublicIPList());
		}
		return publicIPList;
	}

	/**
	 * @return returns a list of private IPs of all nodes set up by this service
	 */
	public ArrayList<String> getPrivateIPList() {
		ArrayList<String> privateIPList = new ArrayList<>();
		for (AWSNode node : ec2List) {
			privateIPList.addAll(node.getPrivateIPList());
		}
		return privateIPList;
	}

	public void setInstanceNames(String name) {

		for (int i = 0; i < ec2List.size(); i++) {
			AWSNode node = ec2List.get(i);
			if (node.hasInstanceIDs()) {
				AmazonEC2 client = node.getEc2();
				CreateTagsRequest req = new CreateTagsRequest();
				req.withResources(node.getInstanceIDs());

				List<Tag> tags = new ArrayList<>();
				tags.add(new Tag("Name", name + "-" + i));
				req.setTags(tags);

				client.createTags(req);
				System.out.println(
						"TAGGED:" + Arrays.toString(node.getInstanceIDs().toArray()) + "  WITH NAME:" + name + "-" + i);
			}
		}
	}

	public static List<Reservation> getInstances(AmazonEC2 ec2){
			DescribeInstancesRequest instRequest = new DescribeInstancesRequest();
			DescribeInstancesResult instResult = ec2.describeInstances(instRequest);

			return instResult.getReservations();
	}
}
