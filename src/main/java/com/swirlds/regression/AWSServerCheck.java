/*
 * (c) 2016-2018 Swirlds, Inc.
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
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.pricing.AWSPricingClient;
import com.amazonaws.services.pricing.AWSPricingClientBuilder;
import com.amazonaws.services.pricing.model.DescribeServicesRequest;
import com.amazonaws.services.pricing.model.Filter;
import com.amazonaws.services.pricing.model.GetProductsRequest;
import com.amazonaws.services.pricing.model.GetProductsResult;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swirlds.regression.slack.SlackNotifier;
import com.swirlds.regression.slack.SlackServerCheckMsg;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class AWSServerCheck {
	private static final Logger log = LogManager.getLogger(AWSServerCheck.class);
	private static final Marker MARKER = MarkerManager.getMarker("REGRESSION_TESTS");
	private static final Marker ERROR = MarkerManager.getMarker("EXCEPTION");

	public static final float SAFE_SPENDING_AMOUNT = 750.0f;
	public static final float WARNING_SPENDING_AMOUNT = 5000.0f;
	public static final float DANGEROUS_SPENDING_AMOUNT = 10000.0f;

	static final ArrayList<Pair<String, String>> regionsToCheck = AWSServerCheck.BuildRegionsToCheckMap();
	// This needs to be put somewhere eay to maintain and pull in on initialization
	static final List<String> KnownReservedInstances = List.of("i-03c90b3fdeed8edd7", "i-050d3f864b99b796f",
			"i-0611e2febc6a73d5a", "i-0cc227bff247a8a09", "i-0b210a6cb3e155cf3");

	private static ArrayList<Pair<String, String>> BuildRegionsToCheckMap() {
		ArrayList<Pair<String, String>> tempMap = new ArrayList<Pair<String, String>>();
		tempMap.add(Pair.of("us-east-1", "US East (N. Virginia)"));
		tempMap.add(Pair.of("us-east-2", "US East (Ohio)"));
		tempMap.add(Pair.of("ap-northeast-1", "Asia Pacific (Tokyo)"));
		tempMap.add(Pair.of("eu-west-3", "EU (Paris)"));
		tempMap.add(Pair.of("eu-west-1", "EU (Ireland)"));
		tempMap.add(Pair.of("ca-central-1", "Canada (Central)"));
		tempMap.add(Pair.of("us-west-1", "US West (N. California)"));
		tempMap.add(Pair.of("eu-west-2", "EU (London)"));
		tempMap.add(Pair.of("eu-north-1", "EU (Stockholm)"));
		tempMap.add(Pair.of("ap-south-1", "Asia Pacific (Mumbai)"));
		tempMap.add(Pair.of("ap-southeast-2", "Asia Pacific (Sydney)"));
		tempMap.add(Pair.of("us-west-2", "US West (Oregon)"));
		tempMap.add(Pair.of("eu-central-1", "EU (Frankfurt)"));
		tempMap.add(Pair.of("ap-southeast-1", "Asia Pacific (Singapore)"));
		tempMap.add(Pair.of("ap-northeast-2", "Asia Pacific (Seoul)"));
		tempMap.add(Pair.of("sa-east-1", "South America (Sao Paulo)"));

		return tempMap;
	}

	private SlackNotifier slacker;
	private SlackServerCheckMsg slackServerCheckMsg;
	ArrayList<StringBuilder> slackMsgArray;
	StringBuilder currentRegionSlackMsg;
	int totalInstances = 0; // global due to fact we are weeding out all but "running" instances

	public AWSServerCheck(String tokenStr, String channelName) {
		slackMsgArray = new ArrayList<>();
		slackServerCheckMsg = SlackServerCheckMsg.createSlackServerChk(tokenStr, channelName);
/*
		StringBuilder sbTemp = new StringBuilder();
		sbTemp.append("Testing slack send");
		slackServerCheckMsg.appendRegionInformation(sbTemp);
*/

		for (Pair<String, String> region : regionsToCheck) {
			currentRegionSlackMsg = new StringBuilder();
			AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion(region.getKey()).build();
			GetReservationsInRegion(ec2, region);
			if (!currentRegionSlackMsg.toString().isEmpty()) {
				slackMsgArray.add(currentRegionSlackMsg);
			}
			System.out.println(currentRegionSlackMsg);

		}
		slackServerCheckMsg.SetRegionInstanceInformation(slackMsgArray);

		slackServerCheckMsg.messageChannel();
	}

	/**
	 * Get all reservations in a given region
	 *
	 * @param ec2
	 * 		- Amazon ec2 class to allow getting information from specific region
	 * @param region
	 * 		- the short and long name strings of the region for use by ec2 and pricing API object
	 */
	private void GetReservationsInRegion(AmazonEC2 ec2, Pair<String, String> region) {
		/* get a list of all instances in region */
		List<Reservation> ReservationDetails = CloudService.getInstances(ec2);
		if (ReservationDetails.size() <= 0) {
			return;
		}
		currentRegionSlackMsg.append("`REGION: " + region.getValue() + "\t" + region.getKey() + "`\n");

		double totalForRegion = 0.0;
		totalInstances = 0;

		for (Reservation res : ReservationDetails) {
			List<Instance> inst = res.getInstances();
			totalForRegion += GetInstanceDetailsForReservation(inst, region);
		}
		currentRegionSlackMsg.insert(currentRegionSlackMsg.indexOf("\n"), "\nTotal Instances: " + totalInstances);
		String regionTotal = String.format("\t\tTOTAL:\t *$%.2f*", totalForRegion);
		currentRegionSlackMsg.insert(currentRegionSlackMsg.indexOf("\n"), regionTotal);
	}

	/**
	 * Queries the total time instance has been up and cost of the instance before calculating and returning the cost of
	 * the instance so far.
	 * Excludes reserved instances
	 *
	 * @param inst
	 * 		- instance id to look up additional details
	 * @param region
	 * 		- the short and long name strings of the region for use by ec2 and pricing API object
	 * @return current dollar amount the instance has cost to run so far
	 */
	private double GetInstanceDetailsForReservation(List<Instance> inst, Pair<String, String> region) {
		/* stores pricing for the same tpye servers once we have th e information */
		HashMap<String, Double> instanceTypePricing = new HashMap<String, Double>();
		double totalForRegion = 0.0;
		for (Instance instance : inst) {
			/* ignore instances that are terminated or stopped */
			if (instance.getState().getName().equals(("running")) && !KnownReservedInstances.contains(
					instance.getInstanceId())) {

				String instancetype = instance.getInstanceType();
				SimpleDateFormat formatter = new SimpleDateFormat("mm-dd-yyyy");
				/* Don't query amazon for pricing if we already have informaiton about a specific instance type */
				if (!instanceTypePricing.containsKey(instancetype)) {
					double instanceTypePrice = getPricingForRegion(instancetype, region.getValue());
					instanceTypePricing.put(instancetype, Double.valueOf(instanceTypePrice));
				}
				Duration duration = Duration.between(
						LocalDateTime.ofInstant(instance.getLaunchTime().toInstant(), ZoneId.systemDefault()),
						LocalDateTime.now());
				double instanceCost = Math.abs(duration.toHours()) * instanceTypePricing.get(instancetype);
				totalForRegion += instanceCost;
				totalInstances++;
				/*Get instance name is it's been set */
				String name = "<name not set>";
				for (Tag tag : instance.getTags()) {
					if (tag.getKey().equals("Name")) {
						name = tag.getValue();
						break;
					}
				}
				// todo add to stringBuilder array for use by slack messenger'
				String instacneDetails = String.format(
						"%s was launched: %s  \tCurrent cost: $%.2f \t%s\n",
						name,
						instance.getLaunchTime().toString(),
						instanceCost, instance.getKeyName());
				currentRegionSlackMsg.append(instacneDetails);
			}
		}
		return totalForRegion;
	}

	/**
	 * Get pricing details for specific non-reserved instance type and return hourly cost
	 *
	 * @param instanceType
	 * 		- String description of instancetype used by amazon pricing objects
	 * @param region
	 * 		- region we are checking the pricing for
	 * @return - hourly cost of the instance
	 */
	private double getPricingForRegion(String instanceType, String region) {
		double serverHourlyPrice = 0.0;
		/* pricing information can currently only be gotten from the us-east-1 region in America. This has nothing to
		do with the region we are trying to cehck the pricing for, but instead where we get the pricing from */
		AWSPricingClient ap = (AWSPricingClient) AWSPricingClientBuilder.standard().withRegion(
				"us-east-1").build();
		DescribeServicesRequest dsReq = new DescribeServicesRequest();

		/* Add filters to narrow the huge list of prices down to single type of server, in a single region. Still need
		 to parse the JSON to get non-reserved instance prices */


		Filter filter = new Filter();
		filter.setType("TERM_MATCH");
		filter.setField("operation");
		filter.setValue("RunInstances");

		Filter filter2 = new Filter();
		filter2.setType("TERM_MATCH");
		filter2.setField("capacitystatus");
		filter2.setValue("Used");

		Filter filter3 = new Filter();
		filter3.setType("TERM_MATCH");
		filter3.setField("instanceType");
		filter3.setValue(instanceType);

		Filter filter4 = new Filter();
		filter4.setType("TERM_MATCH");
		filter4.setField("operatingSystem");
		filter4.setValue("Linux");

		Filter filter5 = new Filter();
		filter5.setType("TERM_MATCH");
		filter5.setField("tenancy");
		filter5.setValue("Shared");

		Filter filter6 = new Filter();
		filter6.setType("TERM_MATCH");
		filter6.setField("location");
		filter6.setValue(region);


		Filter[] filters = { filter, filter2, filter3, filter4, filter5, filter6 };
		GetProductsRequest gpr = new GetProductsRequest().withServiceCode("AmazonEC2").withFilters(
				filters
		);
		GetProductsResult gpres = ap.getProducts(gpr);

		/* filter the three reserved instances for this specific instance type out of the list, leaving only on demand
		item */
		for (String serv : gpres.getPriceList()) {
			serverHourlyPrice = ParsePriceList(serv);
			return serverHourlyPrice;
		}
		return serverHourlyPrice;
	}

	/**
	 * Finds the onDemand server information and gets the price per hour from that and return it.
	 *
	 * @param serv
	 * 		- JSON string describing pricing information for a specific type of instance, will include one on demand
	 * 		listing
	 * 		and several reservered instance listings
	 * @return - hourly cost of running on demand instance of a specified type
	 */
	private double ParsePriceList(String serv) {
		double returnDouble = 0.0;

		JsonFactory factory = new JsonFactory();

		ObjectMapper mapper = new ObjectMapper(factory);
		JsonNode rootNode = null;
		/* Drill down JSON to the USD price of an onDemand instacnce */
		try {
			rootNode = mapper.readTree(serv);
			returnDouble = rootNode.findValue("OnDemand").findValue("pricePerUnit").get("USD").asDouble();
		} catch (IOException | NullPointerException e) {
			log.error(ERROR, "Failed to read AWS Pricing Sheet", e);
		}

		return returnDouble;
	}

	public static void main(String[] parameters) {

		String tokenString = parameters[0];
		String channelName = parameters[1];

		AWSServerCheck servrChk = new AWSServerCheck(tokenString, channelName);

	}

	private class InstanceDetails {
		String region;
		String name;
		Instant startTime;
		float currentCost;
		String keyName;

		public InstanceDetails(String region, String name, Instant startTime, float currentCost, String keyName) {
			this.region = region;
			this.name = name;
			this.startTime = startTime;
			this.currentCost = currentCost;
			this.keyName = keyName;
		}

		public String getRegion() {
			return region;
		}

		public String getName() {
			return name;
		}

		public Instant getStartTime() {
			return startTime;
		}

		public float getCurrentCost() {
			return currentCost;
		}

		public String getKeyName() {
			return keyName;
		}
	}
}

