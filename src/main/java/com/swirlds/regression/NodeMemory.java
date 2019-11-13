package com.swirlds.regression;

public class NodeMemory {
	private MemoryAllocation totalMemory;
	private MemoryAllocation hugePagesMemory;
	private int hugePagesNumber;
	private MemoryAllocation postgresWorkMem;
	private int postgresMaxPreparedTransaction;
	private MemoryAllocation postgresTempBuffers;
	private MemoryAllocation postgresSharedBuffers = new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_SHARED_BUFFERS);
	private MemoryAllocation jvmMemory;
	private MemoryAllocation osReserve = new MemoryAllocation(RegressionUtilities.OS_RESERVE_MEMORY);


	/**
	 * @param totalMem
	 * 		- passed in parameter must be in the format amount, MemoryType ie. 8GB, 8192MB,  8388608KB
	 * @throws IllegalArgumentException
	 * 		- anything not in amount memorytype format, or having a memorytype that is not contained in the MemroyType
	 * 		enum
	 * 		will cause this funciton to throw.
	 */
	//TODO: call with MemoryAllocation totalMem, not string
	public NodeMemory(String totalMem) throws IllegalArgumentException {
		this.totalMemory = new MemoryAllocation(totalMem);

		setPostgresDefaultValues();
		calculateHugePages();
		calculateJVMMemory();
	}

	private void setPostgresDefaultValues() {
		postgresWorkMem = new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_WORK_MEM);
		postgresMaxPreparedTransaction = RegressionUtilities.POSTGRES_DEFAULT_MAX_PREPARED_TRANSACTIONS;
		postgresTempBuffers = new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_TEMP_BUFFERS);
	}

	private void calculateHugePages() {
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

	private void calculateJVMMemory() {
		/* since the OS already has 2GB outside of the hugePages it is not used in this calculation. Postgres will use
		huge pages though, so we need to calculate how much it might take and round that up to the nearest GB to make
		sure the JVM and postgres play nice with memory */
		MemoryType calculationType = MemoryType.GB; // JVM is called with GB for memory
		double workingTotalMemory = hugePagesMemory.getAdjustedMemoryAmount(calculationType);
		/* round postgres Mem up to nearest whole GB */
		workingTotalMemory -= Math.ceil(totalPostgresMemoryNeeds(calculationType));
		/* round JVMs allocation down to nearest GB */
		jvmMemory = new MemoryAllocation((int) Math.floor(workingTotalMemory), calculationType);
	}

	private double totalPostgresMemoryNeeds(MemoryType calculationType) {
		double total = 0.0;

		total += this.postgresSharedBuffers.getAdjustedMemoryAmount(calculationType);
		total += this.postgresTempBuffers.getAdjustedMemoryAmount(calculationType);
		total += this.postgresWorkMem.getAdjustedMemoryAmount(calculationType);

		return total;
	}

	public MemoryAllocation getTotalMemory() {
		return totalMemory;
	}

	public MemoryAllocation getHugePagesMemory() {
		return hugePagesMemory;
	}

	public int getHugePagesNumber() {
		return hugePagesNumber;
	}

	public MemoryAllocation getPostgresWorkMem() {
		return postgresWorkMem;
	}

	public int getPostgresMaxPreparedTransaction() {
		return postgresMaxPreparedTransaction;
	}

	public MemoryAllocation getPostgresTempBuffers() {
		return postgresTempBuffers;
	}

	public MemoryAllocation getPostgresSharedBuffers() {
		return postgresSharedBuffers;
	}

	public MemoryAllocation getJvmMemory() {
		return jvmMemory;
	}

	public MemoryAllocation getOsReserve() {
		return osReserve;
	}
}

