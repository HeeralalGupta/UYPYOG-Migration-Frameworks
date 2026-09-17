package org.egov.finance.migration.modules.expensebill.service;

import java.util.HashMap;
import java.util.Map;

public class BillSubtypeMapping {

	private static final Map<String, Integer> BILL_SUBTYPE_MAP = new HashMap<>();

	static {

		BILL_SUBTYPE_MAP.put("Contingent", 1);
		BILL_SUBTYPE_MAP.put("Salary", 2);
		BILL_SUBTYPE_MAP.put("Pension", 3);
		BILL_SUBTYPE_MAP.put("Works", 4);
		BILL_SUBTYPE_MAP.put("Supplies", 5);
		BILL_SUBTYPE_MAP.put("Recovery", 6);
		BILL_SUBTYPE_MAP.put("Deposit", 7);
		BILL_SUBTYPE_MAP.put("Advance", 8);
		BILL_SUBTYPE_MAP.put("GPF", 9);
		BILL_SUBTYPE_MAP.put("Others", 10);
		BILL_SUBTYPE_MAP.put("Expense", 11);
	}

	public static Integer getBillSubTypeId(String billSubtype) {

		if (billSubtype == null || billSubtype.trim().isEmpty()) {
			return null;
		}

		return BILL_SUBTYPE_MAP.get(billSubtype.trim());
	}
}