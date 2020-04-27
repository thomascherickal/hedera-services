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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Singleton for getting a properly created threadpool whenever one is needed.
 */
public class ThreadPool {

    private static ThreadPool instance;
    private static ExecutorService es;

    private ThreadPool() {
        int poolSize = Runtime.getRuntime().availableProcessors() * 2;
        es = Executors.newFixedThreadPool(poolSize);
    }

    /**
     * Gets an instance of the ExecutorSevice, a new instance will be created if the thread pool has not been
     * instantiated
     * @return ExecutorService for use in threadpooling
     */
    public static ExecutorService getESInstance(){
        if(instance == null) {
            instance = new ThreadPool();
        }
        return es;
    }

    /**
     * Shutdown the Executor service, and uninstantiate the class, so a new executor will be created when called for.
     */
    public static void closeThreadPool() {
        if (es != null) {
            es.shutdown();
            instance = null;
        }
    }
}
