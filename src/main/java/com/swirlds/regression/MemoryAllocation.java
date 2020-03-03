package com.swirlds.regression;

import java.util.Objects;

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

	public int getRawMemoryAmount() {
		return memoryAmount;
	}

	public double getAdjustedMemoryAmount(MemoryType requestedMemoryType){
		double multipler = CalculateMultiplier(requestedMemoryType,this.memoryType);

		return this.memoryAmount * multipler;
	}

	double CalculateMultiplier(MemoryType requestedMemoryType, MemoryType memoryType) {
		int requestOrd = requestedMemoryType.ordinal();
		int currentOrd = memoryType.ordinal();
		int difference = currentOrd - requestOrd;
		return Math.pow(1024,Integer.valueOf(difference).doubleValue());
	}

	public double getMemoryAmount(){
		return getAdjustedMemoryAmount(this.memoryType);
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MemoryAllocation that = (MemoryAllocation) o;
		/* if memory types are different compare the amount of memory for the memory type of this object to confirm they
		are the same.
		 */
		if(memoryType != that.memoryType){
			return getAdjustedMemoryAmount(memoryType) == that.getAdjustedMemoryAmount(memoryType);
		}
		/* if memory types are the same return direct comparison of memory amount and memory type */
		return memoryAmount == that.memoryAmount &&
				memoryType == that.memoryType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(memoryAmount, memoryType);
	}
}

