package org.egov.finance.migration.modules.journalvoucher.service;

import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.journalvoucher.dto.VoucherRequest;
import org.egov.finance.migration.modules.journalvoucher.response.VoucherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class JournalVoucherApiClient {

	private final RestTemplate restTemplate;
	private final AuthenticationService authenticationService;

	@Value("${finance.voucher.create-url}")
	private String voucherCreateUrl;

	public JournalVoucherApiClient(RestTemplate restTemplate, AuthenticationService authenticationService) {

		this.restTemplate = restTemplate;
		this.authenticationService = authenticationService;
	}

	public VoucherResponse createVoucher(VoucherRequest request) {

		String token = authenticationService.getToken();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(token);

		HttpEntity<VoucherRequest> entity = new HttpEntity<>(request, headers);

		ResponseEntity<VoucherResponse> response = restTemplate.exchange(voucherCreateUrl, HttpMethod.POST, entity,
				VoucherResponse.class);

		System.out.println("JV API STATUS : " + response.getStatusCode());

		return response.getBody();
	}
}
