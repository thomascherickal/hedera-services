package com.swirlds.regression;

public class MemoryAllocation {

	private int memoryAmount = 0;
	private MemoryType memoryType;

	public MemoryAllocation(int memoryAmount, MemoryType memoryType) {
		this.memoryAmount = memoryAmount;
		this.memoryType = memoryType;
	}

	public MemoryAllocation(String memory) throws IllegalArgumentException {
		/* remove all non alphanumeric character from string before evaluation */
		// TODO look into removing alpha that isn't [KMGB]
		String tempMemory = memory.replaceAll("[^a-zA-Z0-9]", "");
		String[] seperatedMemStr = tempMemory.split("(?<=\\d)(?=\\D)");
		try {
			this.memoryAmount = Integer.valueOf(seperatedMemStr[0]);
			this.memoryType = MemoryType.valueOf(seperatedMemStr[1]);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
					"passed in parameter must be in the format amount, MemoryType ie. 8GB, 8192MB,  8388608KB", e);
		}
	}

	public int getMemoryAmount() {
		return memoryAmount;
	}

	public void setMemoryAmount(int memoryAmount) {
		this.memoryAmount = memoryAmount;
	}

	public MemoryType getMemoryType() {
		return memoryType;
	}

	public void setMemoryType(MemoryType memoryType) {
		this.memoryType = memoryType;
	}

	@Override
	public String toString() {
		return memoryAmount + memoryType.getMemoryIdent();
	}

}

