package model;

public class Company {
    private final int companyId;
    private final String name;
    private final Company parentCompany;
    private final String description;

    public int getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public Company getParentCompany() {
        return parentCompany;
    }

    public String getDescription() {
        return description;
    }

    public Company(int companyId, String name, Company parentCompany, String description) {
        this.companyId = companyId;
        this.name = name;
        this.parentCompany = parentCompany;
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Company)) return false;

        Company company = (Company) o;

        if (companyId != company.companyId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return companyId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Company{");
        sb.append("companyId=").append(companyId);
        sb.append(", name='").append(name).append('\'');
        sb.append(", parentCompany=").append(parentCompany);
        sb.append(", description='").append(description).append('\'');
        sb.append("} ");
        sb.append(super.toString());

        return sb.toString();
    }
}
