package com.hedera.services.yahcli.config.domain;

/*-
 * ‌
 * Hedera Services Test Clients
 * ​
 * Copyright (C) 2018 - 2021 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.google.common.base.MoreObjects;
import com.hedera.services.yahcli.output.CommonMessages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

public class NetConfig {
	public static final Integer TRAD_DEFAULT_NODE_ACCOUNT = 3;

	private String defaultPayer;
	private Integer defaultNodeAccount = TRAD_DEFAULT_NODE_ACCOUNT;
	private List<NodeConfig> nodes;

	public String getDefaultPayer() {
		return defaultPayer;
	}

	public void setDefaultPayer(String defaultPayer) {
		this.defaultPayer = defaultPayer;
	}

	public Integer getDefaultNodeAccount() {
		return defaultNodeAccount;
	}

	public void setDefaultNodeAccount(Integer defaultNodeAccount) {
		this.defaultNodeAccount = defaultNodeAccount;
	}

	public List<NodeConfig> getNodes() {
		return nodes;
	}

	public void setNodes(List<NodeConfig> nodes) {
		this.nodes = nodes;
	}

	public String fqDefaultNodeAccount() {
		return CommonMessages.COMMON_MESSAGES.fq(defaultNodeAccount);
	}

	public Map<String, String> toSpecProperties() {
		Map<String, String> customProps = new HashMap<>();
		customProps.put("nodes", nodes.stream().map(NodeConfig::asNodesItem).collect(joining(",")));
		return customProps;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("defaultPayer", defaultPayer)
				.add("defaultNodeAccount", "0.0." + defaultNodeAccount)
				.add("nodes", nodes)
				.omitNullValues()
				.toString();
	}
}
