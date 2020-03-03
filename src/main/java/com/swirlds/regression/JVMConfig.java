package com.swirlds.regression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

public class JVMConfig {

    public static final String JAVA_MAX_MEMORY_OPTION = "-Xmx";
    public static final String JAVA_MIN_MEMORY_OPTION = "-Xms";
    private ArrayList<String> standardJVMOptions;
    private LinkedHashMap<String, MemoryAllocation> memoryBasedJVMOptions;

    public JVMConfig() {
        this(null, null, null);
    }

    public JVMConfig(MemoryAllocation maxMemory) {
        this(maxMemory, null, null);
    }

    public JVMConfig(MemoryAllocation maxMemory, ArrayList<String> standardJVMOptions) {
        this(maxMemory, null, standardJVMOptions);
    }

    public JVMConfig(MemoryAllocation maxMemory,
                     LinkedHashMap<String, MemoryAllocation> memoryBasedJVMOptions) {
        this(maxMemory, memoryBasedJVMOptions, null);
    }

    public JVMConfig(String jvmOptionString) {
        ArrayList<String> listOfStandardOptions = new ArrayList<>();
        LinkedHashMap<String, MemoryAllocation> listOfMemoryOptions = new LinkedHashMap<>();

        String[] optionsArray = jvmOptionString.split("[\\s]");

        for (String currentOption : optionsArray) {
            if (Pattern.compile("\\d+[gkmb]$").matcher(currentOption).find()) {
                String[] splitupMemoryOption = currentOption.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
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
            } else {
                listOfStandardOptions.add(currentOption);
            }
        }
        setupJVMParameterLists(null, listOfMemoryOptions, listOfStandardOptions);

    }

    public JVMConfig(MemoryAllocation jvmMemory, LinkedHashMap<String, MemoryAllocation> memoryBasedOptions, ArrayList<String> standardOptions) {

        setupJVMParameterLists(jvmMemory, memoryBasedOptions, standardOptions);
    }

    private void setupJVMParameterLists(MemoryAllocation jvmMemory, LinkedHashMap<String, MemoryAllocation> memoryBasedOptions, ArrayList<String> standardOptions) {
        memoryBasedJVMOptions = new LinkedHashMap<>();
        standardJVMOptions = new ArrayList<>();

        if (jvmMemory == null) {
            setupDefaultJVMemoryUsage(jvmMemory);

        } else {
            memoryBasedJVMOptions.put(JAVA_MAX_MEMORY_OPTION, jvmMemory);
            int jvmMinimumValue = (int) Math.ceil((double) jvmMemory.getRawMemoryAmount() / 10);
            MemoryAllocation jvmMinMemoryAlloc = new MemoryAllocation(jvmMinimumValue, jvmMemory.getMemoryType());
            memoryBasedJVMOptions.put(JAVA_MIN_MEMORY_OPTION, jvmMinMemoryAlloc);
        }

        if (memoryBasedOptions != null && !memoryBasedOptions.isEmpty()) {
            memoryBasedJVMOptions.putAll(memoryBasedOptions);
        } else {
            setupDefaultJVMMemoryOptions();
        }

        if (standardOptions != null && !standardOptions.isEmpty()) {
            standardJVMOptions.addAll(standardOptions);
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

    private void setupDefaultJVMemoryUsage(MemoryAllocation jvmMemory) {
        memoryBasedJVMOptions.put(JAVA_MAX_MEMORY_OPTION, new MemoryAllocation(100, MemoryType.GB));
        memoryBasedJVMOptions.put(JAVA_MIN_MEMORY_OPTION, new MemoryAllocation(8, MemoryType.GB));
    }

    public static final String JVM_OPTIONS_DEFAULT = "-Xmx100g -Xms8g -XX:+UnlockExperimentalVMOptions -XX:+UseZGC " +
            "-XX:ConcGCThreads=14 -XX:ZMarkStackSpaceLimit=16g -XX:+UseLargePages -XX:MaxDirectMemorySize=32g";

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

    private MemoryAllocation resizeMemoryAllocationIfTooBig(MemoryAllocation memoryAlloc, MemoryAllocation maxMem) {
        if (memoryAlloc.getAdjustedMemoryAmount(maxMem.getMemoryType()) <= maxMem.getRawMemoryAmount()) {
            return memoryAlloc;
        } else {
            MemoryAllocation returnAllocation = new MemoryAllocation((int) Math.floor((double) maxMem.getRawMemoryAmount() * 0.75), maxMem.getMemoryType());
            while(returnAllocation.getRawMemoryAmount() == 0 && !returnAllocation.getMemoryType().equals(MemoryType.KB)){
                //move memory type down one value GB -> MB -> KB
                int returnAllocationOrdinalValue = returnAllocation.getMemoryType().ordinal() - 1;
                returnAllocation.setMemoryType(MemoryType.values()[returnAllocationOrdinalValue - 1]);
            }
            return returnAllocation;
        }
    }

    private String convertToJavaMemoryOptionString(MemoryAllocation memoryAlloc) {
        int memory = (int)memoryAlloc.getMemoryAmount();
        MemoryType type = memoryAlloc.getMemoryType();

        String returnString = String.valueOf(memory) + type.name().substring(0, 1).toLowerCase();
        ;

        return returnString;
    }

}
