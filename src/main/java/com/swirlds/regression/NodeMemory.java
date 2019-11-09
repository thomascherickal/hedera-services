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
	}

	private void SetPostgresDefaultValues() {
		postgresWorkMem = new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_WORK_MEM);
		postgresMaxPreparedTransaction = RegressionUtilities.POSTGRES_DEFAULT_MAX_PREPARED_TRANSACTIONS;
		postgresTempBuffers = new MemoryAllocation(RegressionUtilities.POSTGRES_DEFAULT_TEMP_BUFFERS);
    }

    private void CalculateHugePages(){

	}

}

