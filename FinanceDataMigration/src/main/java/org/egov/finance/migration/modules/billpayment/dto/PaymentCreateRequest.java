package org.egov.finance.migration.modules.billpayment.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateRequest {

	@NotBlank
	@Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/\\d{4}$")
	private String paymentVoucherDate;

	@NotBlank
	@Size(max = 100)
	private String fund;

	@NotBlank
	@Size(max = 150)
	private String function;

	@NotBlank
	@Size(max = 100)
	private String department;

	@NotBlank
	@Pattern(regexp = "(?i)^(CHEQUE|NEFT|RTGS)$")
	private String paymentMode;

	@NotBlank
	@Size(max = 150)
	private String bankName;

	@NotBlank
	@Size(max = 150)
	private String bankBranchName;

	@NotBlank
	@Size(max = 150)
	private String ifscCode;

	@NotBlank
	@Size(max = 50)
	private String bankAccountNumber;

	@Size(max = 1000)
	private String description;

	@NotEmpty
	@Valid
	@Builder.Default
	private List<PaymentBillRequest> bills = new ArrayList<>();

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PaymentBillRequest {

		@NotBlank
		@Size(max = 100)
		private String billNumber;

		@NotNull
		@DecimalMin(value = "0.01")
		@Digits(integer = 15, fraction = 2)
		private BigDecimal paymentAmount;
	}
}
