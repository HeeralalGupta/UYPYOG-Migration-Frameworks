package org.egov.finance.migration.modules.expensebill.dto;

public class AccountDetailTypeContract {

	private Long id;
	private String name;
	private String description;
	private String tableName;

	private Boolean active;
	private String fullyQualifiedName;

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(final String tableName) {
		this.tableName = tableName;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(final Boolean active) {
		this.active = active;
	}

	public String getFullyQualifiedName() {
		return fullyQualifiedName;
	}

	public void setFullyQualifiedName(final String fullyQualifiedName) {
		this.fullyQualifiedName = fullyQualifiedName;
	}

	public AccountDetailTypeContract name(String name2) {
		this.setName(name2);
		return this;
	}
}
