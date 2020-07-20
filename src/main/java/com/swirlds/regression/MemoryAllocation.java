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

package com.swirlds.regression;

import java.util.Objects;

public class MemoryAllocation {

	private int memoryAmount = 0;
	private MemoryType memoryType;

	/**
	 * Constructor for MemoryAllocation object
	 * @param memoryAmount - integer amount of memory
	 * @param memoryType - Memory type of the memoryAmount passed in (GB,MB,KB)
	 */
	public MemoryAllocation(int memoryAmount, MemoryType memoryType) {
		this.memoryAmount = memoryAmount;
		this.memoryType = memoryType;
	}

	/**
	 * Constructor for MemoryAllocation object that accepts string representation of memory 100GB, 10kb, 32MB
	 * @param memory - String representing memory (100GB, 10kb, 32MB)
	 * @throws IllegalArgumentException - thrown if stirng doesn't contain a number followed by an acceptable memory type
	 */
	public MemoryAllocation(String memory) throws IllegalArgumentException {
		/* remove all non alphanumeric character from string before evaluation */
		// TODO look into removing alpha that isn't [KMGB]
		String tempMemory = memory.replaceAll("[^a-zA-Z0-9]", "");
		/* split string into digit and non-digit pieces */
		String[] seperatedMemStr = tempMemory.split("(?<=\\d)(?=\\D)");
		try {
			this.memoryAmount = Integer.valueOf(seperatedMemStr[0]);
			this.memoryType = MemoryType.valueOf(seperatedMemStr[1]);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
					"Passed in parameter must be in the format amount, MemoryType ie. 8GB, 8192MB,  8388608KB", e);
		}
	}

	/**
	 * Get integer representing memory amount in the default memory type this was constructed with.
	 * @return - Integer representing memory amount in the default memory type this was constructed with
	 */
	public int getRawMemoryAmount() {
		return memoryAmount;
	}

	/**
	 * Get number representing memory amount in the memory type requested. This is a double in case a higher memory type
	 * than the default is requested.
	 * @param requestedMemoryType - Memory type to return the memory amount in
	 * @return - double representing memory amount based on memory type passed in.
	 */
	public double getAdjustedMemoryAmount(MemoryType requestedMemoryType){
		double multiplier = CalculateMultiplier(requestedMemoryType,this.memoryType);

		return this.memoryAmount * multiplier;
	}

	/**
	 * Calculates the amount to multiply multiply by to convert one memory type to another
	 * @param requestedMemoryType - Memory type to convert to
	 * @param memoryType - memory type that number is stored in
	 * @return - number to multiply by to convert memory type to requested memory type
	 */
	double CalculateMultiplier(MemoryType requestedMemoryType, MemoryType memoryType) {
		int requestOrd = requestedMemoryType.ordinal();
		int currentOrd = memoryType.ordinal();
		int difference = currentOrd - requestOrd;
		return Math.pow(1024,Integer.valueOf(difference).doubleValue());
	}

	/**
	 * Get integer representing memory amount in the default memory type this was constructed with.
	 * @return - Integer representing memory amount in the default memory type this was constructed with
	 */
	public double getMemoryAmount(){
		return getAdjustedMemoryAmount(this.memoryType);
	}


	/**
	 * Set memory amount this object represents
	 * @param memoryAmount - memory amount this object represents based on memory type
	 */
	public void setMemoryAmount(int memoryAmount) {
		this.memoryAmount = memoryAmount;
	}

	/**
	 * Get MemoryType object representing memory type the memory amount is stored in.
	 * @return - memory type the memory amount is stored in
	 */
	public MemoryType getMemoryType() {
		return memoryType;
	}

	/**
	 * Set default memory type of this object
	 * @param memoryType - memory type that the memory amount is represented in
	 */
	public void setMemoryType(MemoryType memoryType) {
		this.memoryType = memoryType;
	}

	@Override
	/**
	 * Overridden toString so object returns both memory amount and memory type
	 * @return - memory string EX. 8GB, 8192MB
	 */
	public String toString() {
		return memoryAmount + memoryType.getMemoryIdent();
	}

	@Override
	/**
	 * Overridden to make sure object representing the same amount of memory with different memory types return a
	 * proper equals
	 *
	 * @return - Where the memory amounts represented are equal, even if the default memory types are different.
	 *     EX. 8GB = 8192MB
	 */
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

