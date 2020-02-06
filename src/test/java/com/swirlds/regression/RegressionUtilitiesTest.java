/*
 * (c) 2016-2019 Swirlds, Inc.
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

import com.swirlds.regression.jsonConfigs.JvmOptionParametersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class RegressionUtilitiesTest {
    String ExpectedString = "-Xmx32g -Xms4g -XX:+UnlockExperimentalVMOptions -XX:+UseZGC " +
            "-XX:ConcGCThreads=14 -XX:ZMarkStackSpaceLimit=16g -XX:+UseLargePages -XX:MaxDirectMemorySize=8g";

    @Test
    @DisplayName("Test building jvm options string based on parameters")
    public void testBuildParameterStringWithParameters() {
        int maxMemory = 32;
        int minMemory = 4;
        int maxDirectMemory = 8;

        assertTrue(ExpectedString.equals(RegressionUtilities.buildParameterString(maxMemory,minMemory,maxDirectMemory)));
    }

    @DisplayName("build options string with bad params")
    @ParameterizedTest
    @CsvSource({"0,-2,-32", "32,4,-1","32,-1,8","-1,4,8"})
    public void testBuildParameterStringWithBadParameters(int maxMemory,int minMemory, int maxDirectMemory ){
        assertTrue(RegressionUtilities.JVM_OPTIONS_DEFAULT.equals(RegressionUtilities.buildParameterString(maxMemory,minMemory,maxDirectMemory)));
    }

    @Test
    @DisplayName("Test building jvm options string based on json config")
    public void testBuildParametersStringWithConfig(){
        JvmOptionParametersConfig config = new JvmOptionParametersConfig();
        config.setMaxMemory(32);
        config.setMinMemory(4);
        config.setMaxDirectMemory(8);

        assertTrue(ExpectedString.equals(RegressionUtilities.buildParameterString(config)));
    }

    @Test
    @DisplayName("build JVM options string with Null JSON config")
    public void testBuildParametersStringWithNullConfig(){
        assertTrue(RegressionUtilities.JVM_OPTIONS_DEFAULT.equals(RegressionUtilities.buildParameterString(null)));
    }

    @ParameterizedTest
    @DisplayName("build options string with bad params")
    @CsvSource({"0,-2,-32", "32,4,-1","32,-1,8","-1,4,8"})
    public void testBuildParameterStringWithBadJson(int maxMemory,int minMemory, int maxDirectMemory ){
        JvmOptionParametersConfig config = new JvmOptionParametersConfig();
        config.setMaxMemory(maxMemory);
        config.setMinMemory(minMemory);
        config.setMaxDirectMemory(maxDirectMemory);
        assertTrue(RegressionUtilities.JVM_OPTIONS_DEFAULT.equals(RegressionUtilities.buildParameterString(maxMemory,minMemory,maxDirectMemory)));
    }


}
