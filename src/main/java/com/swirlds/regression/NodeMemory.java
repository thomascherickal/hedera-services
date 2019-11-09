package com.swirlds.regression;

public class NodeMemory {
	MemoryAllocation totalMemory;
	MemoryAllocation hugePagesMemory;
	int hugePagesNumber;
	MemoryAllocation postgresWorkMem;
	int postgresMaxPreparedTransaction;
	MemoryAllocation postgresTempBuffers;
	MemoryAllocation postgresSharedBuffers;
	MemoryAllocation jvmMemory;
	MemoryAllocation osReserve = new MemoryAllocation(RegressionUtilities.OS_RESERVE_MEMORY);


	/**
	 * @param totalMem
	 * 		- passed in parameter must be in the format amount, MemoryType ie. 8GB, 8192MB,  8388608KB
	 * @throws IllegalArgumentException
	 * 		- anything not in amount memorytype format, or having a memorytype that is not contained in the MemroyType
	 * 		enum
	 * 		will cause this funciton to throw.
	 */
	public NodeMemory(String totalMem) throws IllegalArgumentException {
		this.totalMemory = new MemoryAllocation(totalMem);

		SetPostgresDefaultValues();
		CalculateHugePages();
	}

	private void SetPostgresDefaultValues() {
		postgresWorkMem = new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_WORK_MEM);
		postgresMaxPreparedTransaction = RegressionUtilities.POSTGRES_DEFAULT_MAX_PREPARED_TRANSACTIONS;
		postgresTempBuffers = new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_TEMP_BUFFERS);
	}

	private void CalculateHugePages() {
		MemoryType calculationType = MemoryType.KB; // hugepage sizes are stored in sysctl.conf using KB
		double workingTotalMemory = totalMemory.getAdjustedMemoryAmount(calculationType);
		workingTotalMemory -= osReserve.getAdjustedMemoryAmount(calculationType);
		// huge page number is the number of 2MB chunks to be allocated
		hugePagesNumber = (int) Math.floor(workingTotalMemory / RegressionUtilities.UBUNTU_HUGE_PAGE_SIZE_DIVISOR);
		/* using hugePageNumber instead of workingTotalMemory because it's an int. converting it back to Bytes, and the
		 converting it yo MB for proper sotrage */
		/* TODO look into just adding Bytes to MemoryType to avoid dividing by MB here. */
		hugePagesMemory = new MemoryAllocation(
				hugePagesNumber * RegressionUtilities.UBUNTU_HUGE_PAGE_SIZE_DIVISOR / RegressionUtilities.MB,
				MemoryType.MB);
	}

}

