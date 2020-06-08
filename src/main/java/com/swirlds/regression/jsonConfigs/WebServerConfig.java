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

public class WebServerConfig {
	private boolean useWebServer = false;
	private String webServerPort = "80";
	private String webServerAddress = "localhost";

	public boolean isUseWebServer() {
		return useWebServer;
	}

	public void setUseWebServer(boolean useWebServer) {
		this.useWebServer = useWebServer;
	}

	public String getWebServerPort() {
		return webServerPort;
	}

	public void setWebServerPort(String webServerPort) {
		this.webServerPort = webServerPort;
	}

	public String getWebServerAddress() {
		return webServerAddress;
	}

	public void setWebServerAddress(String webServerAddress) {
		this.webServerAddress = webServerAddress;
	}



}
