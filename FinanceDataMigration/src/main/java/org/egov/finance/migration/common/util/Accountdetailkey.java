package org.egov.finance.migration.common.util;
public class Accountdetailkey  {


    private Integer id;
    private Integer groupid;
    private String detailname;
    private Integer detailkey;
    private Accountdetailtype accountdetailtype;

    public Integer getDetailkey() {
        return detailkey;
    }

    public void setDetailkey(Integer detailkey) {
        this.detailkey = detailkey;
    }

    public String getDetailname() {
        return detailname;
    }

    public void setDetailname(String detailname) {
        this.detailname = detailname;
    }

    public Integer getGroupid() {
        return groupid;
    }

    public void setGroupid(Integer groupid) {
        this.groupid = groupid;
    }

    public Integer getId() {
        return id;
    }

    private void setId(Integer id) {
        this.id = id;
    }

    public Accountdetailtype getAccountdetailtype() {
        return accountdetailtype;
    }

    public void setAccountdetailtype(Accountdetailtype accountdetailtype) {
        this.accountdetailtype = accountdetailtype;
    }
}
