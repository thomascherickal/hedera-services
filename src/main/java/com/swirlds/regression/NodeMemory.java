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

	private MemoryAllocation postgresMaintWorkMem;
	private MemoryAllocation postgresAutovWorkMem;


	private static final MemoryAllocation LOWEST_TIER_MACHINE_SIZE = new MemoryAllocation("8GB");
	private static final MemoryAllocation MID_TIER_MACHINE_SIZE = new MemoryAllocation("64GB");
	private static final MemoryAllocation HIGHEST_TIER_MACHINE_SIZE = new MemoryAllocation("128GB");
	private static final MemoryAllocation MAIN_NET_MACHINE_SIZE = new MemoryAllocation("160GB");

	private static final double JVM_PERCENT_OF_TOTAL_MEMORY = 0.625;
	private static final double HUGEPAGE_PERCENT_OF_TOTAL_MEMORY = 0.88;
	private static final double POSTGRES_SHARED_MEMORY_PERCENT_OF_TOTAL_MEMORY = 0.125;


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
		calculatePostgresMemory();
	}

	private void setPostgresDefaultValues() {
		postgresWorkMem = new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_WORK_MEM);
		postgresMaxPreparedTransaction = RegressionUtilities.POSTGRES_DEFAULT_MAX_PREPARED_TRANSACTIONS;
		postgresTempBuffers = new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_TEMP_BUFFERS);
		postgresSharedBuffers = new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_SHARED_BUFFERS);
	}

	private void calculateHugePages() {
		MemoryType calculationType = MemoryType.KB; // hugePage sizes are stored in sysctl.conf using KB
		double workingTotalMemory = totalMemory.getAdjustedMemoryAmount(calculationType);
		if(workingTotalMemory < LOWEST_TIER_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)){
			workingTotalMemory = new MemoryAllocation("2GB").getAdjustedMemoryAmount(calculationType);
		}
		else if(workingTotalMemory >= LOWEST_TIER_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)
				&& workingTotalMemory < MAIN_NET_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)) {
			workingTotalMemory = workingTotalMemory * HUGEPAGE_PERCENT_OF_TOTAL_MEMORY;
		}
		/* max out memory based on HIGHEst_TIER_MACHINE_SIZE to make sure more memory than main net is not used */
		else{
			workingTotalMemory = MAIN_NET_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType) * HUGEPAGE_PERCENT_OF_TOTAL_MEMORY;
		}

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
		double workingTotalMemory = totalMemory.getAdjustedMemoryAmount(calculationType);
		if(workingTotalMemory < LOWEST_TIER_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)){
			workingTotalMemory = new MemoryAllocation("1GB").getAdjustedMemoryAmount(calculationType);
		}
		else if(workingTotalMemory >= LOWEST_TIER_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)
				&& workingTotalMemory < MAIN_NET_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)) {
			workingTotalMemory = workingTotalMemory * JVM_PERCENT_OF_TOTAL_MEMORY;
		}
		/* max out memory based on HIGHEst_TIER_MACHINE_SIZE to make sure more memory than main net is not used */
		else {
			workingTotalMemory = MAIN_NET_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType) * JVM_PERCENT_OF_TOTAL_MEMORY;
		}
		/* round JVMs allocation down to nearest GB */
		jvmMemory = new MemoryAllocation((int) Math.floor(workingTotalMemory), calculationType);
	}

/*	Setting        |   R < 8GB   |   8GB <= R < 64GB   |   R >= 160GB
-------------------|-------------|---------------------|-------------
	temp_buffers   |    64 MB    |        256 MB       |     4 GB
	prep_trans     |    100      |        100          |     500
	work_mem       |    64 MB    |        256 MB       |     4 GB
	maint_work_mem |    32 MB    |        128 MB       |     2 GB
	autov_work_mem |    32 MB    |        128 MB       |     2 GB*/

	private void calculatePostgresMemory(){
		MemoryType calculationType = MemoryType.MB; // JVM is called with GB for memory


		double workingTotalMemory = totalMemory.getAdjustedMemoryAmount(calculationType);
		if(workingTotalMemory < LOWEST_TIER_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)){
			postgresTempBuffers = postgresWorkMem = new MemoryAllocation("64MB");
			postgresMaxPreparedTransaction = 100;
			postgresMaintWorkMem = postgresAutovWorkMem = new MemoryAllocation("32MB");
		}
		else if(workingTotalMemory < HIGHEST_TIER_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)){
			postgresTempBuffers = postgresWorkMem = new MemoryAllocation("256MB");
			postgresMaxPreparedTransaction = 100;
			postgresMaintWorkMem = postgresAutovWorkMem = new MemoryAllocation("128MB");
		}
		else if (workingTotalMemory >= HIGHEST_TIER_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)){
			postgresTempBuffers = postgresWorkMem = new MemoryAllocation("4GB");
			postgresMaxPreparedTransaction = 500;
			postgresMaintWorkMem = postgresAutovWorkMem = new MemoryAllocation("2GB");
		}

		/* if the machine size is smaller than LOWEST_TIER_MACHINE_SIZE keep the shared memory default */
		if(workingTotalMemory >= LOWEST_TIER_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)
		&& workingTotalMemory < MAIN_NET_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)) {
			workingTotalMemory = workingTotalMemory * POSTGRES_SHARED_MEMORY_PERCENT_OF_TOTAL_MEMORY;
			postgresSharedBuffers = new MemoryAllocation((int) Math.floor(workingTotalMemory), calculationType);
		}
		/* max out memory based on HIGHEst_TIER_MACHINE_SIZE to make sure more memory than main net is not used */
		else if(workingTotalMemory >= MAIN_NET_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)){
			workingTotalMemory = MAIN_NET_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType) * POSTGRES_SHARED_MEMORY_PERCENT_OF_TOTAL_MEMORY;
			postgresSharedBuffers = new MemoryAllocation((int) Math.floor(workingTotalMemory), calculationType);
		}
		// there is no else since the default memory for postgres will be used for anything less than 8GB
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

	public MemoryAllocation getPostgresMaintWorkMem() {
		return postgresMaintWorkMem;
	}

	public MemoryAllocation getPostgresAutovWorkMem() {
		return postgresAutovWorkMem;
	}

	public MemoryAllocation getOsReserve() {
		return osReserve;
	}
}

