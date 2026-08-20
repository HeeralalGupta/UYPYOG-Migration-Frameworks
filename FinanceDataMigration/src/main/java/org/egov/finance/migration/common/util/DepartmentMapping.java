package org.egov.finance.migration.common.util;

import java.util.HashMap;
import java.util.Map;

public class DepartmentMapping {

    private static final Map<String, String> DEPARTMENT_MAP = new HashMap<>();

    static {

        DEPARTMENT_MAP.put("Account Branch", "DEPT_1");
        DEPARTMENT_MAP.put("Legal Branch", "DEPT_2");
        DEPARTMENT_MAP.put("Property Tax Branch", "DEPT_3");
        DEPARTMENT_MAP.put("Building Inspection Branch", "DEPT_4");
        DEPARTMENT_MAP.put("Engineering Branch", "DEPT_5");
        DEPARTMENT_MAP.put("Administration", "DEPT_6");
        DEPARTMENT_MAP.put("Enforcement Branch", "DEPT_7");
        DEPARTMENT_MAP.put("Waste Management Branch", "DEPT_8");
        DEPARTMENT_MAP.put("Light / Electrical Branch", "DEPT_9");
        DEPARTMENT_MAP.put("Establishment Branch", "DEPT_10");
        DEPARTMENT_MAP.put("CA Firm", "DEPT_11");
        DEPARTMENT_MAP.put("Sanitation", "DEPT_12");
        DEPARTMENT_MAP.put("CMC Office", "DEPT_13");
        DEPARTMENT_MAP.put("JC Office", "DEPT_14");
        DEPARTMENT_MAP.put("CPO Branch", "DEPT_15");
        DEPARTMENT_MAP.put("Transport", "DEPT_16");
        DEPARTMENT_MAP.put("Mayor Office", "DEPT_17");
        DEPARTMENT_MAP.put("Planning", "DEPT_18");
        DEPARTMENT_MAP.put("Revenue", "DEPT_19");
        DEPARTMENT_MAP.put("DMC Office", "DEPT_20");
        DEPARTMENT_MAP.put("EE-Elect.", "DEPT_21");
        DEPARTMENT_MAP.put("EE-SBM", "DEPT_22");
        DEPARTMENT_MAP.put("EE-Hort.", "DEPT_23");
        DEPARTMENT_MAP.put("EE-HQ", "DEPT_24");
        DEPARTMENT_MAP.put("Enforcement", "DEPT_25");
        DEPARTMENT_MAP.put("Audit", "DEPT_26");
        DEPARTMENT_MAP.put("CFC", "DEPT_27");
        DEPARTMENT_MAP.put("DIV-1", "DEPT_28");

    }

    public static String getDepartmentCode(String departmentName) {
        return DEPARTMENT_MAP.getOrDefault(departmentName.trim(), departmentName);
    }

}
