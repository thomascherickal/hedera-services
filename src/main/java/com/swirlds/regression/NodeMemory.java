package com.swirlds.regression;

public class NodeMemory {
	MemoryAllocation totalMemory;
	MemoryAllocation hugePagesMemory;
	int hugePagesNumber;
	MemoryAllocation postgresWorkMem;
	int postgresMaxPreparedTransaction;
	MemoryAllocation postgresTempBuffers;
	MemoryAllocation postgresSharedBuffers = new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_SHARED_BUFFERS);
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
		CalculateJVMMemory();

	}

	private void SetPostgresDefaultValues() {
		postgresWorkMem = new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_WORK_MEM);
		postgresMaxPreparedTransaction = RegressionUtilities.POSTGRES_DEFAULT_MAX_PREPARED_TRANSACTIONS;
		postgresTempBuffers = new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_TEMP_BUFFERS);
	}

	private void CalculateHugePages() {
		MemoryType calculationType = MemoryType.KB; // hugePage sizes are stored in sysctl.conf using KB
		double workingTotalMemory = totalMemory.getAdjustedMemoryAmount(calculationType);
		workingTotalMemory -= osReserve.getAdjustedMemoryAmount(calculationType);
		/* huge page number is the number of 2MB chunks to be allocated */
		hugePagesNumber = (int) Math.floor(workingTotalMemory / RegressionUtilities.UBUNTU_HUGE_PAGE_SIZE_DIVISOR);
		/* using hugePageNumber instead of workingTotalMemory because it's an int. converting it back to KB before
		storing */
		hugePagesMemory = new MemoryAllocation(
				hugePagesNumber * RegressionUtilities.UBUNTU_HUGE_PAGE_SIZE_DIVISOR,
				MemoryType.KB);
	}

	private void CalculateJVMMemory() {
		/* since the OS already has 2GB outside of the hugePages it is not used in this calculation. Postgres will use
		huge pages though, so we need to calculate how much it might take and round that up to the nearest GB to make
		sure the JVM and postgres play nice with memory */
		MemoryType calculationType = MemoryType.GB; // JVM is called with GB for memory
		double workingTotalMemory = hugePagesMemory.getAdjustedMemoryAmount(calculationType);
		/* round postgres Mem up to nearest whole GB */
		workingTotalMemory -= Math.ceil(TotalPostgresMemoryNeeds(calculationType));
		/* round JVMs allocation down to nearest GB */
		jvmMemory = new MemoryAllocation((int) Math.floor(workingTotalMemory), calculationType);
	}

	private double TotalPostgresMemoryNeeds(MemoryType calculationType) {
		double total = 0.0;

		total += this.postgresSharedBuffers.getAdjustedMemoryAmount(calculationType);
		total += this.postgresTempBuffers.getAdjustedMemoryAmount(calculationType);
		total += this.postgresWorkMem.getAdjustedMemoryAmount(calculationType);

		return total;
	}

}

