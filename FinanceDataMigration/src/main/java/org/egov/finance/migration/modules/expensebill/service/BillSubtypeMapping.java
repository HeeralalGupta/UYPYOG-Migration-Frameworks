package org.egov.finance.migration.modules.expensebill.service;

import java.util.HashMap;
import java.util.Map;

public class BillSubtypeMapping {

	private static final Map<String, Long> BILL_SUBTYPE_MAP = new HashMap<>();

	static {

		BILL_SUBTYPE_MAP.put("Contingent", 1L);
		BILL_SUBTYPE_MAP.put("Salary", 2L);
		BILL_SUBTYPE_MAP.put("Pension", 3L);
		BILL_SUBTYPE_MAP.put("Works", 4L);
		BILL_SUBTYPE_MAP.put("Supplies", 5L);
		BILL_SUBTYPE_MAP.put("Recovery", 6L);
		BILL_SUBTYPE_MAP.put("Deposit", 7L);
		BILL_SUBTYPE_MAP.put("Advance", 8L);
		BILL_SUBTYPE_MAP.put("GPF", 9L);
		BILL_SUBTYPE_MAP.put("Others", 10L);
		BILL_SUBTYPE_MAP.put("Expense", 11L);
	}

	public static Long getBillSubTypeId(String billSubtype) {

		if (billSubtype == null || billSubtype.trim().isEmpty()) {
			return null;
		}

		return BILL_SUBTYPE_MAP.get(billSubtype.trim());
	}
}