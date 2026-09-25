package org.egov.finance.migration.modules.expensebill.service;

import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseBillCreateRequest;
import org.egov.finance.migration.modules.expensebill.response.ExpenseBillResponse;
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
public class ExpenseBillApiClient {

	private final RestTemplate restTemplate;
	private final AuthenticationService authenticationService;
	private final ObjectMapper objectMapper;

	@Value("${finance.local.baseurl}")
	private String financeServiceUrl;

	@Value("${finance.expense.bill.create-url}")
	private String expenseBillCreateUrl;

	public ExpenseBillApiClient(RestTemplate restTemplate, AuthenticationService authenticationService,
			ObjectMapper objectMapper) {

		this.restTemplate = requireObject(restTemplate, "RestTemplate");
		this.authenticationService = requireObject(authenticationService, "AuthenticationService");
		this.objectMapper = requireObject(objectMapper, "ObjectMapper");
	}

	/**
	 * Create Expense Bill.
	 */
	public ExpenseBillResponse createExpenseBill(ExpenseBillCreateRequest request) {

		validateRequest(request);
		String tenantId = request.getTenantId();
		String url = buildUrl(tenantId);
		String token = getAuthenticationToken(tenantId);
		validateRequestInfo(request);
		request.getRequestInfo().setAuthToken(token);

		HttpHeaders headers = buildHeaders(token);
		HttpEntity<ExpenseBillCreateRequest> entity = new HttpEntity<>(request, headers);

		logRequest(request, url, tenantId, token);
		ResponseEntity<ExpenseBillResponse> response = callExpenseBillApi(url, entity, tenantId);
		validateResponse(response, tenantId);
		return response.getBody();
	}

	private void validateRequest(ExpenseBillCreateRequest request) {

		if (request == null) {
			throw new IllegalArgumentException("ExpenseBillCreateRequest cannot be null.");
		}
		if (!hasText(request.getTenantId())) {
			throw new IllegalArgumentException("ExpenseBillCreateRequest tenantId " + "is required.");
		}
		if (request.getRequestInfo() == null) {
			throw new IllegalArgumentException("ExpenseBillCreateRequest RequestInfo " + "cannot be null.");
		}
		if (request.getExpenseBillRequest() == null) {
			throw new IllegalArgumentException("ExpenseBillCreateRequest expenseBillRequest " + "cannot be null.");
		}
		if (request.getExpenseBillRequest().getEgBillregister() == null) {
			throw new IllegalArgumentException("ExpenseBillRequest egBillregister " + "cannot be null.");
		}
	}

	private void validateRequestInfo(ExpenseBillCreateRequest request) {

		if (request.getRequestInfo() == null) {
			throw new IllegalArgumentException("RequestInfo cannot be null.");
		}

		if (!hasText(request.getRequestInfo().getApiId())) {

			/*
			 * Only keep this validation if apiId is mandatory in your RequestInfo DTO.
			 */
		}

		if (!hasText(request.getTenantId())) {
			throw new IllegalArgumentException("Tenant ID cannot be empty.");
		}
	}

	private String buildUrl(String tenantId) {

		if (!hasText(financeServiceUrl)) {
			throw new IllegalArgumentException("Property 'finance.local.baseurl' " + "is not configured.");
		}

		if (!hasText(expenseBillCreateUrl)) {
			throw new IllegalArgumentException("Property 'finance.expense.bill.create-url' " + "is not configured.");
		}

		String baseUrl = financeServiceUrl.trim();
		String createUrl = expenseBillCreateUrl.trim();
		String url;

		if (baseUrl.endsWith("/") && createUrl.startsWith("/")) {
			url = baseUrl.substring(0, baseUrl.length() - 1) + createUrl;
		} else if (!baseUrl.endsWith("/") && !createUrl.startsWith("/")) {
			url = baseUrl + "/" + createUrl;
		} else {
			url = baseUrl + createUrl;
		}

		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			throw new IllegalArgumentException("Invalid Finance API URL: " + url);
		}

		/*
		 * Add tenantId only when the configured URL does not already contain it.
		 */
		if (!url.contains("tenantId=")) {
			String separator = url.contains("?") ? "&" : "?";
			url = url + separator + "tenantId=" + tenantId;
		}

		return url;
	}

	private String getAuthenticationToken(String tenantId) {

		try {
			String token = authenticationService.getToken(tenantId);
			if (!hasText(token)) {
				throw new IllegalArgumentException("Authentication token is empty " + "for tenant: " + tenantId);
			}
			return token.trim();
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to obtain authentication token " + "for tenant '" + tenantId
					+ "': " + getExceptionMessage(e), e);
		}
	}

	private HttpHeaders buildHeaders(String token) {

		if (!hasText(token)) {
			throw new IllegalArgumentException(
					"Authentication token cannot be empty " + "while building HTTP headers.");
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.setBearerAuth(token);
		return headers;
	}

	private void logRequest(ExpenseBillCreateRequest request, String url, String tenantId, String token) {

		System.out.println("============================================");
		System.out.println("EXPENSE BILL CREATE API CALL");
		System.out.println("URL : " + url);
		System.out.println("Tenant : " + tenantId);
		System.out.println("Token Available : " + hasText(token));
		System.out.println("============================================");

		try {
			String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
			System.out.println("==========================================================");
			System.out.println("       COMPLETE EXPENSE BILL REQUEST JSON");
			System.out.println("==========================================================");
			System.out.println(requestJson);
			System.out.println("==========================================================");
		} catch (Exception e) {

			/*
			 * JSON logging failure should NOT stop the actual API call. The request itself
			 * is still valid.
			 */

			System.err.println("Unable to serialize Expense Bill request for logging: " + getExceptionMessage(e));
		}
	}

//	private ResponseEntity<ExpenseBillResponse> callExpenseBillApi(String url,
//			HttpEntity<ExpenseBillCreateRequest> entity, String tenantId) {
//
//		try {
//
//			ResponseEntity<ExpenseBillResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity,
//					ExpenseBillResponse.class);
//
//			if (response == null) {
//				throw new IllegalArgumentException(
//						"Expense Bill API returned null ResponseEntity " + "for tenant: " + tenantId);
//			}
//
//			System.out.println("EXPENSE BILL API STATUS : " + response.getStatusCode());
//			return response;
//
//		} catch (HttpStatusCodeException e) {
//			String responseBody = e.getResponseBodyAsString();
//			String message = "Expense Bill API returned HTTP " + e.getStatusCode().value() + " for tenant '" + tenantId
//					+ "'.";
//			if (hasText(responseBody)) {
//				message = message + " Response: " + responseBody;
//			}
//			throw new IllegalArgumentException(message, e);
//		} catch (ResourceAccessException e) {
//			throw new IllegalArgumentException("Unable to connect to Expense Bill API " + "for tenant '" + tenantId
//					+ "': " + getExceptionMessage(e), e);
//		} catch (RestClientException e) {
//			throw new IllegalArgumentException("Expense Bill API communication failed " + "for tenant '" + tenantId
//					+ "': " + getExceptionMessage(e), e);
//		} catch (IllegalArgumentException e) {
//			throw e;
//		} catch (Exception e) {
//			throw new IllegalArgumentException("Unexpected error while calling " + "Expense Bill API for tenant '"
//					+ tenantId + "': " + getExceptionMessage(e), e);
//		}
//	}

	private ResponseEntity<ExpenseBillResponse> callExpenseBillApi(String url,HttpEntity<ExpenseBillCreateRequest> entity, String tenantId) {

		try {

			ResponseEntity<ExpenseBillResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity,
					ExpenseBillResponse.class);

			if (response == null) {
				throw new IllegalArgumentException(	"Expense Bill API returned null ResponseEntity for tenant: " + tenantId);
			}
			System.out.println("EXPENSE BILL API STATUS : " + response.getStatusCode());
			return response;

		} catch (HttpStatusCodeException e) {
			String responseBody = e.getResponseBodyAsString();
//			String apiMessage = extractApiErrorMessage(responseBody);
//			String message = "Expense Bill API returned HTTP " + e.getStatusCode().value() + " for tenant '" + tenantId	+ "'. ";
//			if (hasText(apiMessage)) {
//				message = message + apiMessage;
//			}

			System.err.println("Expense Bill API validation/error response: " + responseBody);
			throw new IllegalArgumentException(responseBody, e);

		} catch (ResourceAccessException e) {
			throw new IllegalArgumentException(	"Unable to connect to Expense Bill API for tenant '" + tenantId + "': " + getExceptionMessage(e),
					e);

		} catch (RestClientException e) {
			throw new IllegalArgumentException(	"Expense Bill API communication failed for tenant '" + tenantId + "': " + getExceptionMessage(e),
					e);

		} catch (IllegalArgumentException e) {
			throw e;

		} catch (Exception e) {
			throw new IllegalArgumentException("Unexpected error while calling Expense Bill API for tenant '" + tenantId
					+ "': " + getExceptionMessage(e), e);
		}
	}

	private void validateResponse(ResponseEntity<ExpenseBillResponse> response, String tenantId) {

		if (response == null) {
			throw new IllegalArgumentException("Expense Bill API response is null " + "for tenant: " + tenantId);
		}
		if (response.getStatusCode() == null) {
			throw new IllegalArgumentException("Expense Bill API returned response " + "without HTTP status for tenant: " + tenantId);
		}
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new IllegalArgumentException("Expense Bill API returned HTTP status "
					+ response.getStatusCode().value() + " for tenant: " + tenantId);
		}
		if (response.getBody() == null) {
			throw new IllegalArgumentException(
					"Expense Bill API returned empty response body " + "for tenant: " + tenantId);
		}
	}

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

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private static <T> T requireObject(T object, String objectName) {
		if (object == null) {
			throw new IllegalArgumentException(objectName + " cannot be null.");
		}
		return object;
	}

//	private String extractApiErrorMessage(String responseBody) {
//
//		if (!hasText(responseBody)) {
//			return "Expense Bill API returned an empty error response.";
//		}
//
//		try {
//			ExpenseErrorResponse errorResponse = objectMapper.readValue(responseBody, ExpenseErrorResponse.class);
//
//			if (errorResponse != null) {
//
//				StringBuilder message = new StringBuilder();
//
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
//
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
//
//			System.err.println("Unable to parse Expense Bill API error response: " + getExceptionMessage(parseException));
//		}
//
//		/*
//		 * Fallback when the response is not in the expected ExpenseBillResponse format.
//		 */
//		return responseBody;
//	}
}
