package com.swirlds.regression;

import java.io.File;

public class NodeSetup {
	private NodeMemory nodeMemory;
	private File postgresConfig;
	private JVMConfig jvmConfig;

	public NodeSetup(NodeMemory nodeMemory) {
		this.nodeMemory = nodeMemory;
		jvmConfig = new JVMConfig(nodeMemory.getJvmMemory());
	}

	public String getJVMOptionsString(){
		return jvmConfig.getJVMOptionsString();
	}
}
