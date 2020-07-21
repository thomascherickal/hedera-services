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
	 * Constructor for node memory, receives total memory to base all calculations on.
	 * @param totalMem
	 * 		- passed in parameter must be in the format amount, MemoryType ie. 8GB, 8192MB,  8388608KB
	 * @throws IllegalArgumentException
	 * 		- anything not in memory type format, or having a memory type that is not contained in the MemoryType
	 * 		enum will cause this function to throw.
	 */
	public NodeMemory(String totalMem) throws IllegalArgumentException {
		this(new MemoryAllocation(totalMem));
	}

	/**
	 * Constructor for node memory, receives total memory to base all calculations on.
	 * @param memory = Memory Allocation object representing total memory of node
	 */
	public NodeMemory(MemoryAllocation memory) {
		this.totalMemory = memory;
		setPostgresDefaultValues();
		calculateHugePages();
		calculateJVMMemory();
		calculatePostgresMemory();
	}

	/**
	 * Sets postgres default values from Regression Utilities to make sure all postgres values
	 *     are set before calculations are made
	 */
	private void setPostgresDefaultValues() {
		postgresWorkMem = new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_WORK_MEM);
		postgresMaxPreparedTransaction = RegressionUtilities.POSTGRES_DEFAULT_MAX_PREPARED_TRANSACTIONS;
		postgresTempBuffers = new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_TEMP_BUFFERS);
		postgresSharedBuffers = new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_SHARED_BUFFERS);
	}

	/**
	 * Calculates the number of Huge pages needed based on total memory
	 */
	private void calculateHugePages() {
		MemoryType calculationType = MemoryType.KB; // hugePage sizes are stored in sysctl.conf using KB
		double workingTotalMemory = totalMemory.getAdjustedMemoryAmount(calculationType);
		/* Machines less than the lowest tier we plan for will receive 2GB of huge pages */
		if(workingTotalMemory < LOWEST_TIER_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)){
			workingTotalMemory = new MemoryAllocation("2GB").getAdjustedMemoryAmount(calculationType);
		}
		/* use our standard calculation for machine with in planned for parameters */
		else if(workingTotalMemory >= LOWEST_TIER_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)
				&& workingTotalMemory < MAIN_NET_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)) {
			workingTotalMemory = workingTotalMemory * HUGEPAGE_PERCENT_OF_TOTAL_MEMORY;
		}
		/* max out memory based on HIGHEST_TIER_MACHINE_SIZE to make sure more memory than main net is not used */
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

	/**
	 * Calculate the maximum JVM memory based on total memory size of node.
	 */
	private void calculateJVMMemory() {
		/* since the OS already has 2GB outside of the hugePages it is not used in this calculation. Postgres will use
		huge pages though, so we need to calculate how much it might take and round that up to the nearest GB to make
		sure the JVM and postgres play nice with memory */
		MemoryType calculationType = MemoryType.GB; // JVM is called with GB for memory
		double workingTotalMemory = totalMemory.getAdjustedMemoryAmount(calculationType);
		/* If the node is smaller than the lowest planed for node size still use 1GB for maximum java size */
		if(workingTotalMemory < LOWEST_TIER_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)){
			workingTotalMemory = new MemoryAllocation("1GB").getAdjustedMemoryAmount(calculationType);
		}
		/* use normal calcuations ofr nodes of expected sizes */
		else if(workingTotalMemory >= LOWEST_TIER_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)
				&& workingTotalMemory < MAIN_NET_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType)) {
			workingTotalMemory = workingTotalMemory * JVM_PERCENT_OF_TOTAL_MEMORY;
		}
		/* max out memory based on HIGHEST_TIER_MACHINE_SIZE to make sure more memory than main net is not used */
		else {
			workingTotalMemory = MAIN_NET_MACHINE_SIZE.getAdjustedMemoryAmount(calculationType) * JVM_PERCENT_OF_TOTAL_MEMORY;
		}
		/* round JVMs allocation down to nearest GB */
		jvmMemory = new MemoryAllocation((int) Math.floor(workingTotalMemory), calculationType);
	}

	/**
	 *  Set up postgres variables based on total memory size, chart bellow outlines the calculations
	 *
	 *    Setting        |   R < 8GB   |   8GB <= R < 64GB   |   R >= 160GB
	 *-------------------|-------------|---------------------|-------------
	 *    temp_buffers   |    64 MB    |        256 MB       |     4 GB
	 *    prep_trans     |    100      |        100          |     500
	 *    work_mem       |    64 MB    |        256 MB       |     4 GB
	 *    maint_work_mem |    32 MB    |        128 MB       |     2 GB
	 *    autov_work_mem |    32 MB    |        128 MB       |     2 GB
	 */
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

}

