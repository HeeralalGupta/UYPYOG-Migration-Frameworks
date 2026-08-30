package org.egov.finance.migration.modules.scheme.service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.egov.finance.migration.common.dto.Fund;
import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.RequestInfoBuilder;
import org.egov.finance.migration.common.dto.SchemeDto;
import org.egov.finance.migration.modules.scheme.dto.CreateSchemeRequest;
import org.egov.finance.migration.modules.scheme.dto.SchemeRecord;
import org.springframework.stereotype.Service;

@Service
public class SchemeRequestBuilder {

	private final RequestInfoBuilder requestInfoBuilder;

	public SchemeRequestBuilder(RequestInfoBuilder requestInfoBuilder) {
		this.requestInfoBuilder = requestInfoBuilder;
	}

	/**
	 * Build Finance API SchemeRequest from one Scheme record.
	 */
	public CreateSchemeRequest build(
			SchemeRecord record,
			MigrationRequest migrationRequest) {

		CreateSchemeRequest request = new CreateSchemeRequest();

		/*
		 * =========================================================
		 * TENANT
		 * =========================================================
		 */

		String tenantId = migrationRequest.getTenantId();

		request.setTenantId(tenantId);

		/*
		 * =========================================================
		 * REQUEST INFO
		 * =========================================================
		 */

		RequestInfo requestInfo =
				requestInfoBuilder.build(tenantId);

		request.setRequestInfo(requestInfo);

		/*
		 * =========================================================
		 * FUND NAME
		 * =========================================================
		 *
		 * Excel:
		 * Column D = Fund
		 *
		 * This is also available separately in CreateSchemeRequest.
		 */

		request.setFundName(record.getFundName());

		/*
		 * =========================================================
		 * BUILD ONE SCHEME
		 * =========================================================
		 */

		SchemeDto scheme = buildScheme(record);

		/*
		 * Add Scheme to request
		 */
		request.setSchemeDto(scheme);

		return request;
	}

	/**
	 * Convert migration SchemeRecord into Finance API SchemeDto.
	 */
	private SchemeDto buildScheme(SchemeRecord record) {

		SchemeDto scheme = new SchemeDto();

		/*
		 * =========================================================
		 * SCHEME NAME
		 * =========================================================
		 */

		scheme.setName(record.getSchemeName());

		/*
		 * =========================================================
		 * START DATE
		 * =========================================================
		 *
		 * Excel Start Date -> validFrom
		 */

		scheme.setValidFrom(
				parseDate(record.getValidFrom()));

		/*
		 * =========================================================
		 * END DATE
		 * =========================================================
		 *
		 * Excel End Date -> validTo
		 */

		scheme.setValidTo(
				parseDate(record.getValidTo()));

		/*
		 * =========================================================
		 * STATUS
		 * =========================================================
		 *
		 * Active / Inactive
		 * TRUE / FALSE
		 */

		scheme.setIsActive(
				parseStatus(record.getIsActive()));

		/*
		 * =========================================================
		 * DESCRIPTION
		 * =========================================================
		 */

		scheme.setDescription(
				record.getDescription());

		/*
		 * =========================================================
		 * FUND
		 * =========================================================
		 *
		 * Create Fund DTO from Excel Fund value.
		 *
		 * NOTE:
		 * If the Scheme API expects an existing Fund object
		 * with an ID/code, this part will need to be changed
		 * according to the actual Fund API response.
		 */

		Fund fund = new Fund();

		fund.setName(record.getFundName());

		scheme.setFund(fund);

		return scheme;
	}

	/**
	 * Convert Excel date into java.sql.Date.
	 *
	 * Supports:
	 *
	 * dd/MM/yyyy
	 * dd-MM-yyyy
	 * yyyy-MM-dd
	 * dd/MM/yy
	 * dd-MM-yy
	 */
	private Date parseDate(String value) {

		if (value == null || value.trim().isEmpty()) {
			return null;
		}

		String dateValue = value.trim();

		String[] dateFormats = {
				"dd/MM/yyyy",
				"dd-MM-yyyy",
				"yyyy-MM-dd",
				"dd/MM/yy",
				"dd-MM-yy",
				"d-M-yy",
				"d/MM/yy",
				"dd-M-yy"
		};

		for (String format : dateFormats) {

			try {

				DateTimeFormatter formatter =
						DateTimeFormatter.ofPattern(format);

				LocalDate localDate =
						LocalDate.parse(dateValue, formatter);

				return Date.valueOf(localDate);

			} catch (DateTimeParseException e) {
				// Try next format
			}
		}

		throw new IllegalArgumentException(
				"Invalid Scheme date: " + value);
	}

	/**
	 * Convert Excel Status into Boolean.
	 *
	 * Supported values:
	 *
	 * Active
	 * ACTIVE
	 * Yes
	 * YES
	 * Y
	 * True
	 * TRUE
	 * 1
	 *
	 * Everything else is treated as inactive.
	 */
	private Boolean parseStatus(String status) {

		if (status == null || status.trim().isEmpty()) {
			return false;
		}

		String value = status.trim();

		return value.equalsIgnoreCase("Active")
				|| value.equalsIgnoreCase("Yes")
				|| value.equalsIgnoreCase("Y")
				|| value.equalsIgnoreCase("True")
				|| value.equals("1");
	}
}