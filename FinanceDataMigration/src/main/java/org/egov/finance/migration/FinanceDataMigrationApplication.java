package org.egov.finance.migration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FinanceDataMigrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinanceDataMigrationApplication.class, args);
		System.out.println("Running...");
	}

}
