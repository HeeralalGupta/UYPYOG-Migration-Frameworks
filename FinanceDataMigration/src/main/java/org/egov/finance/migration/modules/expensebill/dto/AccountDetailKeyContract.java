package org.egov.finance.migration.modules.expensebill.dto;
public class AccountDetailKeyContract {
	private Long id;

    private AccountDetailTypeContract accountDetailType;

    private Long key;
    
    private String detailName;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public AccountDetailTypeContract getAccountDetailType() {
        return accountDetailType;
    }

    public void setAccountDetailType(final AccountDetailTypeContract accountDetailType) {
        this.accountDetailType = accountDetailType;
    }

    public Long getKey() {
        return key;
    }

    public void setKey(final Long key) {
        this.key = key;
    }
    
	public String getDetailName() {
		return detailName;
	}

	public void setDetailName(String detailName) {
		this.detailName = detailName;
	}

	public AccountDetailKeyContract key(Integer detailKeyId) {
	this.setKey(Long.valueOf(detailKeyId));
	return this;
	
	}

}
