package com.swirlds.regression;

public class SavedStatePathInfo {
	private String fullPath;
	private long roundNumber;
	private long nodeId;
	private String swirldsName;
	private String mainClassName;

	public SavedStatePathInfo(String fullPath, long roundNumber, long nodeId, String swirldsName,
			String mainClassName) {
		this.fullPath = fullPath;
		this.roundNumber = roundNumber;
		this.nodeId = nodeId;
		this.swirldsName = swirldsName;
		this.mainClassName = mainClassName;
	}

	public String getFullPath() {
		return fullPath;
	}

	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}

	public long getRoundNumber() {
		return roundNumber;
	}

	public void setRoundNumber(long roundNumber) {
		this.roundNumber = roundNumber;
	}

	public long getNodeId() {
		return nodeId;
	}

	public void setNodeId(long nodeId) {
		this.nodeId = nodeId;
	}

	public String getSwirldsName() {
		return swirldsName;
	}

	public void setSwirldsName(String swirldsName) {
		this.swirldsName = swirldsName;
	}

	public String getMainClassName() {
		return mainClassName;
	}

	public void setMainClassName(String mainClassName) {
		this.mainClassName = mainClassName;
	}

	@Override
	public String toString() {
		return "SavedStatePathInfo{" +
				"fullPath='" + fullPath + '\'' +
				", roundNumber=" + roundNumber +
				", nodeId=" + nodeId +
				", swirldsName='" + swirldsName + '\'' +
				", mainClassName='" + mainClassName + '\'' +
				'}';
	}
}
