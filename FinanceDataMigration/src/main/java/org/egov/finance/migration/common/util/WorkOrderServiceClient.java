package org.egov.finance.migration.common.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.modules.contractorbill.dto.EgBillWorkItemsDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WorkOrderServiceClient {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	@Value("${finance.local.baseurl}")
	private String financeHost;

	@Value("${workorder.search}")
	private String workOrderSearch;

	public WorkOrderServiceClient(RestTemplate restTemplate) {

		this.restTemplate = restTemplate;
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Fetch all work items for a given Work Order number.
	 */
	public List<EgBillWorkItemsDTO> getWorkItemsByOrderNumber(String orderNumber, RequestInfo requestInfo,
			String tenantId) {

		/*
		 * ===================================================== VALIDATION
		 * =====================================================
		 */

		if (orderNumber == null || orderNumber.trim().isEmpty()) {
			throw new IllegalArgumentException("Work Order number is empty.");
		}

		if (requestInfo == null || requestInfo.getAuthToken() == null || requestInfo.getAuthToken().trim().isEmpty()) {
			throw new IllegalArgumentException("Auth token is missing while fetching Work Order.");
		}

		if (tenantId == null || tenantId.trim().isEmpty()) {
			throw new IllegalArgumentException("Tenant ID is missing while fetching Work Order.");
		}

		/*
		 * ===================================================== ENCODE QUERY PARAMETERS
		 * =====================================================
		 */

		String encodedTenantId = URLEncoder.encode(tenantId.trim(), StandardCharsets.UTF_8);
		String encodedToken = URLEncoder.encode(requestInfo.getAuthToken().trim(), StandardCharsets.UTF_8);
		String encodedOrderNumber = URLEncoder.encode(orderNumber.trim(), StandardCharsets.UTF_8);

		/*
		 * ===================================================== BUILD URL
		 * =====================================================
		 */

		String url = financeHost + workOrderSearch + "?tenantId=" + encodedTenantId + "&auth_token=" + encodedToken
				+ "&orderNumber=" + orderNumber;

		/*
		 * ===================================================== REQUEST BODY
		 * =====================================================
		 */

		WorkOrderRequest request = new WorkOrderRequest();
		request.setRequestInfo(requestInfo);

		/*
		 * ===================================================== HEADERS
		 * =====================================================
		 */

		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<WorkOrderRequest> entity = new HttpEntity<>(request, headers);

		/*
		 * ===================================================== LOG
		 * =====================================================
		 */

		System.out.println("====================================");
		System.out.println("WORK ORDER API CALL");
		System.out.println("URL : " + url);
		System.out.println("Order Number : " + orderNumber);
		System.out.println("Tenant : " + tenantId);
		System.out.println("Token Available : "
				+ (requestInfo.getAuthToken() != null && !requestInfo.getAuthToken().trim().isEmpty()));
		System.out.println("====================================");

		/*
		 * ===================================================== CALL API
		 * =====================================================
		 */

		ResponseEntity<String> response;

		try {
			response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

		} catch (Exception e) {
			throw new RuntimeException("Work Order API call failed for order number: " + orderNumber, e);
		}

		/*
		 * ===================================================== RESPONSE LOG
		 * =====================================================
		 */

		System.out.println("WORK ORDER API STATUS : " + response.getStatusCode());
		System.out.println("WORK ORDER API RESPONSE:");
		System.out.println(response.getBody());
		System.out.println("====================================");
		/*
		 * ===================================================== BASIC RESPONSE CHECK
		 * =====================================================
		 */

		String responseBody = response.getBody();

		if (responseBody == null || responseBody.trim().isEmpty()) {
			throw new IllegalArgumentException("Empty response received for Work Order: " + orderNumber);
		}

		/*
		 * ===================================================== PARSE JSON
		 * =====================================================
		 */

		try {

			JsonNode root = objectMapper.readTree(responseBody);

			/*
			 * Find the work-items array anywhere in the response.
			 */
			JsonNode itemsNode = findWorkItemsArray(root);
			if (itemsNode == null || !itemsNode.isArray()) {
				throw new IllegalArgumentException("No work items found in Work Order response for: " + orderNumber);
			}

			/*
			 * ================================================= MAP ALL ITEMS
			 * =================================================
			 */

			List<EgBillWorkItemsDTO> workItems = new ArrayList<>();

			for (JsonNode item : itemsNode) {
				EgBillWorkItemsDTO workItem = mapWorkItem(item);
				if (workItem != null) {
					workItems.add(workItem);
				}
			}

			/*
			 * ================================================= FINAL VALIDATION
			 * =================================================
			 */

			if (workItems.isEmpty()) {

				throw new IllegalArgumentException(
						"Work Order found but no valid work items were returned for: " + orderNumber);
			}

			System.out.println("TOTAL WORK ITEMS : " + workItems.size());
			return workItems;

		} catch (IllegalArgumentException e) {
			throw e;

		} catch (Exception e) {
			throw new RuntimeException("Unable to parse Work Order response for: " + orderNumber, e);
		}
	}

	/**
	 * Search recursively for an array named:
	 *
	 * workItems workitems workOrderItems egBillWorkItemsDTO items
	 */
	private JsonNode findWorkItemsArray(JsonNode node) {

		if (node == null) {
			return null;
		}

		/*
		 * If current node itself is an object, inspect field names.
		 */

		if (node.isObject()) {

			Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> field = fields.next();
				String fieldName = field.getKey();
				JsonNode value = field.getValue();
				String normalized = fieldName.replace("_", "").replace("-", "").toLowerCase();

				/*
				 * Known possible work-item field names.
				 */

				if (normalized.equals("workitems") || normalized.equals("workorderitems")
						|| normalized.equals("egbillworkitemsdto") || normalized.equals("items")) {

					if (value.isArray()) {
						return value;
					}
				}

				/*
				 * Search recursively.
				 */

				JsonNode result = findWorkItemsArray(value);

				if (result != null) {
					return result;
				}
			}
		}

		/*
		 * Arrays can also contain nested objects.
		 */

		if (node.isArray()) {
			for (JsonNode child : node) {
				JsonNode result = findWorkItemsArray(child);
				if (result != null) {
					return result;
				}
			}
		}

		return null;
	}

	/**
	 * Convert one JSON work-item object into EgBillWorkItemsDTO.
	 */
	private EgBillWorkItemsDTO mapWorkItem(JsonNode item) {

		if (item == null || !item.isObject()) {
			return null;
		}

		EgBillWorkItemsDTO dto = new EgBillWorkItemsDTO();

		/*
		 * itemId
		 */
		dto.setItemId(getLong(item, "id"));

		/*
		 * itemCode
		 */
		dto.setItemCode(getString(item, "itemCode", "itemcode"));

		/*
		 * unitRate
		 */
		dto.setUnitRate(getLong(item, "unitRate", "unitrate"));

		/*
		 * billedQuantity
		 */
		dto.setBilledQuantity(getLong(item, "billedQuantity", "billedquantity"));

		/*
		 * unitValueWithGst
		 */
		dto.setUnitValueWithGst(getLong(item, "unitValueWithGst", "unitvaluewithgst"));

		/*
		 * quantity
		 */
		dto.setQuantity(getLong(item, "quantity"));

		/*
		 * amount
		 */
		dto.setAmount(getLong(item, "amount"));

		return dto;
	}

	/**
	 * Read String from possible JSON field names.
	 */
	private String getString(JsonNode node, String... fieldNames) {

		for (String fieldName : fieldNames) {
			JsonNode value = node.get(fieldName);
			if (value != null && !value.isNull()) {
				return value.asText().trim();
			}
		}

		return null;
	}

	/**
	 * Read Long from possible JSON field names.
	 */
	private Long getLong(JsonNode node, String... fieldNames) {

		for (String fieldName : fieldNames) {
			JsonNode value = node.get(fieldName);
			if (value == null || value.isNull()) {
				continue;
			}

			if (value.isNumber()) {
				return value.longValue();
			}

			String text = value.asText();

			if (text == null || text.trim().isEmpty()) {
				continue;
			}

			try {
				return new java.math.BigDecimal(text.trim()).longValueExact();

			} catch (Exception ignored) {

				// Try next field.
			}
		}

		return null;
	}
}