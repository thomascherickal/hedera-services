/*
 * (c) 2016-2020 Swirlds, Inc.
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


public class NetworkErrorConfig {
	/** whether block network interface on a node */
	private boolean blockNetwork = false;

	/** whether random drop some packet */
	private boolean enablePktLoss = false;
	/** default percentage of packet loss  */
	private float packetLossPercentage = 0.1f;

	/** whether delay packet */
	private boolean enablePktDelay = false;
	/** default packet delay */
	private int packetDelayMS = 50;

	public boolean isBlockNetwork() {
		return blockNetwork;
	}

	public void setBlockNetwork(boolean blockNetwork) {
		this.blockNetwork = blockNetwork;
	}

	public boolean isEnablePktLoss() {
		return enablePktLoss;
	}

	public void setEnablePktLoss(boolean enablePktLoss) {
		this.enablePktLoss = enablePktLoss;
	}

	public float getPacketLossPercentage() {
		return packetLossPercentage;
	}

	public void setPacketLossPercentage(float packetLossPercentage) {
		this.packetLossPercentage = packetLossPercentage;
	}

	public boolean isEnablePktDelay() {
		return enablePktDelay;
	}

	public void setEnablePktDelay(boolean enablePktDelay) {
		this.enablePktDelay = enablePktDelay;
	}

	public int getPacketDelayMS() {
		return packetDelayMS;
	}

	public void setPacketDelayMS(int packetDelayMS) {
		this.packetDelayMS = packetDelayMS;
	}
}
