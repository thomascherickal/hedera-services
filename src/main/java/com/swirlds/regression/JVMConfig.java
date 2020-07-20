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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

public class JVMConfig {

    public static final String JAVA_MAX_MEMORY_OPTION = "-Xmx";
    public static final String JAVA_MIN_MEMORY_OPTION = "-Xms";
    private static final int JAVA_MIN_MEMORY_DEFAULT_DIVISOR = 10;
    private ArrayList<String> standardJVMOptions;
    private LinkedHashMap<String, MemoryAllocation> memoryBasedJVMOptions;

    /**
     * Standard constructor for JVMConfig
     */
    public JVMConfig() {
        this(null, null, null);
    }

    /**
     * Constructor for JVMConfig where max memory for the JVM is specifically set
     * @param maxMemory MemoryAllocation object set to maximum desired JVM memory
     */
    public JVMConfig(MemoryAllocation maxMemory) {
        this(maxMemory, null, null);
    }

    /**
     * Constructor for JVMConfig where max memory for the JVM is specifically set, and list of other desired JVM options
     * @param maxMemory - MemoryAllocation object set to maximum desired JVM memory
     * @param standardJVMOptions - list of strings that contain JVM options to launch each node with
     */
    public JVMConfig(MemoryAllocation maxMemory, ArrayList<String> standardJVMOptions) {
        this(maxMemory, null, standardJVMOptions);
    }

    /**
     * Constructor for JVMConfig where max memory for the JVM is specifically set, along with a list of MemoryAllocation
     *   for JVM memory based options (such as Xmx, Xms, MaxDirectMemorySize, etc)
     * @param maxMemory - MemoryAllocation object set to maximum desired JVM memory
     * @param memoryBasedJVMOptions - map of JVMoption name string key, memoryAllocation value
     */
    public JVMConfig(MemoryAllocation maxMemory,
                     LinkedHashMap<String, MemoryAllocation> memoryBasedJVMOptions) {
        this(maxMemory, memoryBasedJVMOptions, null);
    }

    /**
     * Constructor for JVMConfig where JVM options string is passed in parsed and then used to set up the JVMConfig
     *    object
     * @param jvmOptionString - String of jvm options to be used when starting node
     */
    public JVMConfig(String jvmOptionString) {
        ArrayList<String> listOfStandardOptions = new ArrayList<>();
        LinkedHashMap<String, MemoryAllocation> listOfMemoryOptions = new LinkedHashMap<>();

        /* split passed in string on white space */
        String[] optionsArray = jvmOptionString.split("[\\s]");

        for (String currentOption : optionsArray) {
            /* look for a pattern of number followed by memory indication (kb,mb,gb) at the end of current string
            *  expected string EX. -Xmx=120g*/
            if (Pattern.compile("\\d+[gkmb]$").matcher(currentOption).find()) {
                /* split current string on the digit making everything before the digit the first element,
                the digits itself the second element, and everything after the digit the third element.
                EX from above would be ["-Xmx=","120","g"]
                 */
                String[] splitupMemoryOption = currentOption.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
                /* use the third element to determine the base memory size to use for this option */
                if (splitupMemoryOption.length == 3) {
                    MemoryType currentMemOptionType;
                    switch (splitupMemoryOption[2]) {
                        case "g":
                        case "G":
                            currentMemOptionType = MemoryType.GB;
                            break;
                        case "m":
                        case "M":
                            currentMemOptionType = MemoryType.MB;
                            break;
                        case "k":
                        case "K":
                            currentMemOptionType = MemoryType.KB;
                            break;
                        default:
                            continue;
                    }
                    listOfMemoryOptions.put(splitupMemoryOption[0], new MemoryAllocation(Integer.valueOf(splitupMemoryOption[1]), currentMemOptionType));
                }
            }
            /* if this is not a memory related option, just put the whole thing into the list of standard options */
            else {
                listOfStandardOptions.add(currentOption);
            }
        }
        /* now that everything is parsed properly set up the parmeters lists so a proper JVM can be returned when asked
        for.
         */
        setupJVMParameterLists(null, listOfMemoryOptions, listOfStandardOptions);

    }

    /**
     * Constructor for JVMConfig where max memory for the JVM is specifically set, along with a list of MemoryAllocation
     *    for JVM memory based options (such as Xmx, Xms, MaxDirectMemorySize, etc)
     * @param maxMemory - MemoryAllocation object set to maximum desired JVM memory
     * @param memoryBasedJVMOptions - map of JVMoption name string key, memoryAllocation value
     * @param standardJVMOptions - list of strings that contain JVM options to launch each node with
     */
    public JVMConfig(MemoryAllocation maxMemory, LinkedHashMap<String, MemoryAllocation> memoryBasedJVMOptions, ArrayList<String> standardJVMOptions) {
        setupJVMParameterLists(maxMemory, memoryBasedJVMOptions, standardJVMOptions);
    }

    /**
     * Setup all options needed for a proper JVM option string to be returned when requested
     *
     * @param maxMemory - MemoryAllocation object set to maximum desired JVM memory
     * @param memoryBasedJVMOptions - map of JVMoption name string key, memoryAllocation value
     * @param standardJVMOptions - list of strings that contain JVM options to launch each node with
     */
    private void setupJVMParameterLists(MemoryAllocation maxMemory, LinkedHashMap<String, MemoryAllocation> memoryBasedJVMOptions, ArrayList<String> standardJVMOptions) {
        this.memoryBasedJVMOptions = new LinkedHashMap<>();
        this.standardJVMOptions = new ArrayList<>();

        if (maxMemory == null) {
            setupDefaultJVMemoryUsage();
        } else {
            this.memoryBasedJVMOptions.put(JAVA_MAX_MEMORY_OPTION, maxMemory);
            int jvmMinimumValue = (int) Math.ceil((double) maxMemory.getRawMemoryAmount() / JAVA_MIN_MEMORY_DEFAULT_DIVISOR);
            MemoryAllocation jvmMinMemoryAlloc = new MemoryAllocation(jvmMinimumValue, maxMemory.getMemoryType());
            this.memoryBasedJVMOptions.put(JAVA_MIN_MEMORY_OPTION, jvmMinMemoryAlloc);
        }

        if (memoryBasedJVMOptions != null && !memoryBasedJVMOptions.isEmpty()) {
            this.memoryBasedJVMOptions.putAll(memoryBasedJVMOptions);
        } else {
            setupDefaultJVMMemoryOptions();
        }

        if (standardJVMOptions != null && !standardJVMOptions.isEmpty()) {
            this.standardJVMOptions.addAll(standardJVMOptions);
        } else {
            setupDefaultJVMStandardOptions();
        }
    }

    private void setupDefaultJVMStandardOptions() {
        standardJVMOptions.add("-XX:+UnlockExperimentalVMOptions");
        standardJVMOptions.add("-XX:+UseZGC");
        standardJVMOptions.add("-XX:ConcGCThreads=14");
        standardJVMOptions.add("-XX:+UseLargePages");
    }

    private void setupDefaultJVMMemoryOptions() {
        memoryBasedJVMOptions.put("-XX:ZMarkStackSpaceLimit=", new MemoryAllocation(16, MemoryType.GB));
        memoryBasedJVMOptions.put("-XX:MaxDirectMemorySize=", new MemoryAllocation(32, MemoryType.GB));
    }

    private void setupDefaultJVMemoryUsage() {
        memoryBasedJVMOptions.put(JAVA_MAX_MEMORY_OPTION, new MemoryAllocation(100, MemoryType.GB));
        memoryBasedJVMOptions.put(JAVA_MIN_MEMORY_OPTION, new MemoryAllocation(8, MemoryType.GB));
    }

    /**
     * Used to get a string to be used when calling java to set up JVM options based on parameters set elsewhere in code.
     * @return - String formatted properly with JVM options to be accepted with a java call
     */
    public String getJVMOptionsString() {
        StringBuilder returnString = new StringBuilder();

        MemoryAllocation maxMem = memoryBasedJVMOptions.get(JAVA_MAX_MEMORY_OPTION);
        for (String currentOption : standardJVMOptions) {
            returnString.append(currentOption);
            returnString.append(" ");
        }

        memoryBasedJVMOptions.forEach((option, memoryAlloc) -> {
            if (option.equals(JAVA_MAX_MEMORY_OPTION) || option.equals(JAVA_MIN_MEMORY_OPTION)) {
                returnString.append(option + convertToJavaMemoryOptionString(memoryAlloc));
            } else {
                memoryAlloc = resizeMemoryAllocationIfTooBig(memoryAlloc, maxMem);
                if(memoryAlloc.getMemoryAmount() != 0) {
                    returnString.append(option + convertToJavaMemoryOptionString(memoryAlloc));
                }
            }

            returnString.append(" ");
        });

        return returnString.toString().trim();
    }

    /**
     * Function to check if a memory option is set to more than the maximum JVM memory. If it is resize it to 75% of the
     * max memory size.
     * @param memoryAlloc - memory to be checked
     * @param maxMem - maximum JVM memory
     * @return - memory allocation with the properly sized option
     */
    private MemoryAllocation resizeMemoryAllocationIfTooBig(MemoryAllocation memoryAlloc, MemoryAllocation maxMem) {
        /* make sure the memory option is not equal or greater than max JVM memory */
        if (memoryAlloc.getAdjustedMemoryAmount(maxMem.getMemoryType()) <= maxMem.getRawMemoryAmount()) {
            return memoryAlloc;
        } else {
            MemoryAllocation returnAllocation = new MemoryAllocation((int) Math.floor((double) maxMem.getRawMemoryAmount() * 0.75), maxMem.getMemoryType());
            /* in cases where max memory is low the new amount may end up being less than 1 of the default memory type
                  this while loop downgrades the default memory type for the allocation until it gets a number greater
                   than 0 OR gets to the lowest allocation possible (KB)  */
            while(returnAllocation.getRawMemoryAmount() == 0 && !returnAllocation.getMemoryType().equals(MemoryType.KB)){
                //move memory type down one value GB -> MB -> KB
                int returnAllocationOrdinalValue = returnAllocation.getMemoryType().ordinal() - 1;
                returnAllocation.setMemoryType(MemoryType.values()[returnAllocationOrdinalValue - 1]);
            }
            return returnAllocation;
        }
    }

    /**
     * Convert a MemoryAllocation object into a JVM parsable option string parameter
     * @param memoryAlloc - MemoryAllocation object to convert
     * @return - acceptable JVM option string
     */
    private String convertToJavaMemoryOptionString(MemoryAllocation memoryAlloc) {
        int memory = (int)memoryAlloc.getMemoryAmount();
        MemoryType type = memoryAlloc.getMemoryType();

        String returnString = memory + type.name().substring(0, 1).toLowerCase();

        return returnString;
    }

}
