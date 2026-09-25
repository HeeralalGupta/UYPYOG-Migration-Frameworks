package org.egov.finance.migration.modules.supplierbill.service;

import java.util.Collections;

import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.supplierbill.dto.SupplierBillCreateRequest;
import org.egov.finance.migration.modules.supplierbill.response.SupplierBillResponse;
import org.egov.finance.migration.modules.journalvoucher.response.ExpenseErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import tools.jackson.databind.ObjectMapper;

@Service
public class SupplierBillApiClient {

	private final RestTemplate restTemplate;
	private final AuthenticationService authenticationService;
	private final ObjectMapper objectMapper;

	@Value("${finance.supplier-bill.create-url}")
	private String supplierBillCreateUrl;

	public SupplierBillApiClient(RestTemplate restTemplate, AuthenticationService authenticationService,
			ObjectMapper objectMapper) {

		this.restTemplate = requireObject(restTemplate, "RestTemplate");
		this.authenticationService = requireObject(authenticationService, "AuthenticationService");
		this.objectMapper = requireObject(objectMapper, "ObjectMapper");
	}

	/**
	 * Create Supplier Bill.
	 */
	public SupplierBillResponse createSupplierBill(SupplierBillCreateRequest request) {

		validateRequest(request);
		String tenantId = request.getTenantId();
		String url = buildUrl(tenantId);
		String token = getAuthenticationToken(tenantId);
		validateRequestInfo(request);

		/*
		 * Keep auth token inside RequestInfo as well if the Finance API expects it
		 * there.
		 */
		request.getRequestInfo().setAuthToken(token);
		HttpHeaders headers = buildHeaders(token);
		HttpEntity<SupplierBillCreateRequest> entity = new HttpEntity<>(request, headers);
		logRequest(request, url, tenantId, token);
		ResponseEntity<SupplierBillResponse> response = callSupplierBillApi(url, entity, tenantId);
		validateResponse(response, tenantId);
		return response.getBody();
	}

	/**
	 * Validate Supplier Bill request.
	 */
	private void validateRequest(SupplierBillCreateRequest request) {

		if (request == null) {
			throw new IllegalArgumentException("SupplierBillCreateRequest cannot be null.");
		}

		if (!hasText(request.getTenantId())) {
			throw new IllegalArgumentException("SupplierBillCreateRequest tenantId is required.");
		}

		if (request.getRequestInfo() == null) {
			throw new IllegalArgumentException("SupplierBillCreateRequest RequestInfo cannot be null.");
		}

		/*
		 * Change this getter if your actual DTO uses another name.
		 */
		if (request.getSupplierBillRequest() == null) {
			throw new IllegalArgumentException("SupplierBillCreateRequest supplierBillRequest cannot be null.");
		}

		if (request.getSupplierBillRequest().getEgBillregister() == null) {
			throw new IllegalArgumentException("SupplierBillRequest egBillregister cannot be null.");
		}
	}

	/**
	 * Validate RequestInfo.
	 */
	private void validateRequestInfo(SupplierBillCreateRequest request) {

		if (request.getRequestInfo() == null) {
			throw new IllegalArgumentException("RequestInfo cannot be null.");
		}

		if (!hasText(request.getTenantId())) {
			throw new IllegalArgumentException("Tenant ID cannot be empty.");
		}
	}

	/**
	 * Build Finance API URL.
	 */
	private String buildUrl(String tenantId) {

		if (!hasText(supplierBillCreateUrl)) {
			throw new IllegalArgumentException("Property 'finance.supplier-bill.create-url' is not configured.");
		}

		String url = supplierBillCreateUrl.trim();

		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			throw new IllegalArgumentException("Invalid Finance API URL: " + url);
		}

		/*
		 * Add tenantId only when it is not already configured in the URL.
		 */
		if (!url.contains("tenantId=")) {
			String separator = url.contains("?") ? "&" : "?";
			url = url + separator + "tenantId=" + tenantId;
		}

		return url;
	}

	/**
	 * Get authentication token for tenant.
	 */
	private String getAuthenticationToken(String tenantId) {

		try {

			String token = authenticationService.getToken(tenantId);

			if (!hasText(token)) {
				throw new IllegalArgumentException("Authentication token is empty for tenant: " + tenantId);
			}

			return token.trim();

		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to obtain authentication token for tenant '" + tenantId + "': " + getExceptionMessage(e),e);
		}
	}

	/**
	 * Build HTTP headers.
	 */
	private HttpHeaders buildHeaders(String token) {

		if (!hasText(token)) {
			throw new IllegalArgumentException("Authentication token cannot be empty while building HTTP headers.");
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.setBearerAuth(token);
		return headers;
	}

	/**
	 * Log request information without exposing authentication token.
	 */
	private void logRequest(SupplierBillCreateRequest request, String url, String tenantId, String token) {

		System.out.println("============================================");
		System.out.println("SUPPLIER BILL CREATE API CALL");
		System.out.println("URL : " + url);
		System.out.println("Tenant : " + tenantId);
		System.out.println("Token Available : " + hasText(token));
		System.out.println("============================================");

		try {

			String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);

			/*
			 * WARNING:
			 *
			 * The request object may contain the auth token because
			 * request.getRequestInfo().setAuthToken(token) is called before this method.
			 *
			 * Therefore, do NOT print the complete request JSON unless the authToken is
			 * masked.
			 */

			System.out.println("==========================================================");
			System.out.println("       COMPLETE SUPPLIER BILL REQUEST JSON");
			System.out.println("==========================================================");
			System.out.println(maskAuthToken(requestJson));
			System.out.println("==========================================================");

		} catch (Exception e) {

			/*
			 * Logging failure must not stop the actual API call.
			 */
			System.err.println("Unable to serialize Supplier Bill request for logging: " + getExceptionMessage(e));
		}
	}

	/**
	 * Call Supplier Bill API.
	 */
	private ResponseEntity<SupplierBillResponse> callSupplierBillApi(String url,
			HttpEntity<SupplierBillCreateRequest> entity, String tenantId) {

		try {

			ResponseEntity<SupplierBillResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity,
					SupplierBillResponse.class);

			if (response == null) {

				throw new IllegalArgumentException(
						"Supplier Bill API returned null ResponseEntity " + "for tenant: " + tenantId);
			}

			System.out.println("SUPPLIER BILL API STATUS : " + response.getStatusCode());

			return response;

		} catch (HttpStatusCodeException e) {

			String responseBody = e.getResponseBodyAsString();
//			String apiMessage = extractApiErrorMessage(responseBody);
//			String message = "Supplier Bill API returned HTTP " + e.getStatusCode().value() + " for tenant '" + tenantId
//					+ "'.";
//
//			if (hasText(apiMessage)) {
//				message = message + " " + apiMessage;
//			}

			System.err.println("Supplier Bill API validation/error response: " + responseBody);

			throw new IllegalArgumentException(responseBody, e);

		} catch (ResourceAccessException e) {

			throw new IllegalArgumentException(
					"Unable to connect to Supplier Bill API for tenant '" + tenantId + "': " + getExceptionMessage(e),
					e);

		} catch (RestClientException e) {

			throw new IllegalArgumentException(
					"Supplier Bill API communication failed for tenant '" + tenantId + "': " + getExceptionMessage(e),
					e);

		} catch (IllegalArgumentException e) {

			throw e;

		} catch (Exception e) {

			throw new IllegalArgumentException("Unexpected error while calling Supplier Bill API " + "for tenant '"
					+ tenantId + "': " + getExceptionMessage(e), e);
		}
	}

	/**
	 * Validate API response.
	 */
	private void validateResponse(ResponseEntity<SupplierBillResponse> response, String tenantId) {

		if (response == null) {

			throw new IllegalArgumentException("Supplier Bill API response is null " + "for tenant: " + tenantId);
		}

		if (response.getStatusCode() == null) {

			throw new IllegalArgumentException(
					"Supplier Bill API returned response " + "without HTTP status for tenant: " + tenantId);
		}

		if (!response.getStatusCode().is2xxSuccessful()) {

			throw new IllegalArgumentException("Supplier Bill API returned HTTP status "
					+ response.getStatusCode().value() + " for tenant: " + tenantId);
		}

		if (response.getBody() == null) {

			throw new IllegalArgumentException(
					"Supplier Bill API returned empty response body " + "for tenant: " + tenantId);
		}
	}

	/**
	 * Extract useful message from API error response.
	 */
//	private String extractApiErrorMessage(String responseBody) {
//
//		if (!hasText(responseBody)) {
//			return "Supplier Bill API returned an empty error response.";
//		}
//
//		try {
//
//			ExpenseErrorResponse errorResponse = objectMapper.readValue(responseBody, ExpenseErrorResponse.class);
//			if (errorResponse != null) {
//				StringBuilder message = new StringBuilder();
//				if (hasText(errorResponse.getMessage())) {
//					message.append(errorResponse.getMessage());
//				}
//
//				if (errorResponse.getErrors() != null && !errorResponse.getErrors().isEmpty()) {
//
//					if (message.length() > 0) {
//						message.append(" ");
//					}
//
//					message.append("Validation errors: ");
//
//					for (int i = 0; i < errorResponse.getErrors().size(); i++) {
//						if (i > 0) {
//							message.append(" | ");
//						}
//
//						message.append(errorResponse.getErrors().get(i));
//					}
//				}
//
//				if (message.length() > 0) {
//					return message.toString();
//				}
//			}
//
//		} catch (Exception parseException) {
//			System.err.println("Unable to parse Supplier Bill API error response: " + getExceptionMessage(parseException));
//		}
//		/*
//		 * Fallback when the API response does not match ExpenseErrorResponse structure.
//		 */
//		return responseBody;
//	}

	/**
	 * Mask authToken before printing request JSON.
	 */
	private String maskAuthToken(String requestJson) {

		if (!hasText(requestJson)) {
			return requestJson;
		}

		return requestJson.replaceAll("(\"authToken\"\\s*:\\s*\")[^\"]*(\")", "$1********$2");
	}

	/**
	 * Extract meaningful exception message.
	 */
	private String getExceptionMessage(Throwable exception) {

		if (exception == null) {
			return "Unknown error.";
		}

		if (hasText(exception.getMessage())) {
			return exception.getMessage();
		}

		Throwable cause = exception.getCause();

		if (cause != null && hasText(cause.getMessage())) {
			return cause.getMessage();
		}

		return exception.getClass().getSimpleName();
	}

	/**
	 * Check whether string contains text.
	 */
	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	/**
	 * Validate required dependency.
	 */
	private static <T> T requireObject(T object, String objectName) {

		if (object == null) {
			throw new IllegalArgumentException(objectName + " cannot be null.");
		}

		return object;
	}
}