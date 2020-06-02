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

package com.swirlds.regression.slack;


import com.google.inject.Provides;
import com.hubspot.slack.client.SlackClient;
import com.hubspot.slack.client.SlackClientModule;
import com.hubspot.slack.client.SlackClientRuntimeConfig;
import com.hubspot.slack.client.SlackWebClient;

public class SlackModule extends com.google.inject.AbstractModule {
	private String token;

	public SlackModule(String token) {
		this.token = token;
	}

	@Override
	protected void configure() {
		install(new SlackClientModule());
	}

	@Provides
	public SlackClient providesClient(SlackWebClient.Factory factory) {
		return factory.build(
				SlackClientRuntimeConfig.builder()
						.setTokenSupplier(() -> token)
						.build()
		);
	}
}
