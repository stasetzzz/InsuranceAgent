package model;

import model.clients.LegalPerson;
import model.clients.NaturalPerson;
import model.insurances.CompanyByInsuranceType;
import model.insurances.Insurance;
import model.insurances.InsuranceType;
import model.insurances.attributes.AttributeType;
import model.insurances.attributes.InsuranceAttribute;

import java.sql.*;
import java.util.Date;
import java.util.ArrayList;
import java.util.Locale;

public class ModelController {
    //region Static Members
    private static ModelController ourInstance;



    public static ModelController getInstance() {
        return ourInstance;
    }

    public static void initializeInstance(String connectionUrl, String username, String password) throws SQLException {
        // will throw exception, if fail to log in
        Locale.setDefault(Locale.ENGLISH);
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        UserType userType = UserType.UNAUTHORIZED;
        Integer dataId = null;

        PreparedStatement selectRole = conn.prepareStatement(
                "select Role from session_roles where role like 'INSURANCE_%'");
        ResultSet roleResultSet = selectRole.executeQuery();
        //Если есть нужная роль
        if (roleResultSet.next()) {
            String role = roleResultSet.getString("Role");
            userType = UserType.fromRoleName(role);

            if (userType == UserType.AGENT
                    ||userType == UserType.MANAGER
                    ||userType == UserType.LEGAL
                    ||userType == UserType.NATURAL)
            {
                PreparedStatement selectType = conn.prepareStatement(
                        "SELECT dataId,userType from USERS_AND_USERDATA" +
                                " where userId = SYS_CONTEXT ('USERENV', 'SESSION_USERID')");
                ResultSet selectTypeRSet = selectType.executeQuery();

                if (selectTypeRSet.next())
                    dataId = selectTypeRSet.getInt("dataId");
                else
                    userType = UserType.UNAUTHORIZED;

                selectType.close();
            }
        }

        ourInstance = new  ModelController(connectionUrl, username, password, userType, dataId);
        selectRole.close();
        conn.close();
    }
    //endregion

    //region Fields
    private final String connectionUrl;
    private final String username;
    private final String password;
    private final UserType userType;
    private final Integer dataId;
    //endregion

    //region Getters
    public UserType getUserType() {
        return userType;
    }
    //endregion

    //region Constructors
    public ModelController(String connectionUrl, String username, String password, UserType userType, Integer dataId) {
        this.connectionUrl = connectionUrl;
        this.username = username;
        this.password = password;
        this.userType = userType;
        this.dataId = dataId;
    }
    //endregion



    public Object getUserObject() throws SQLException {
        switch (userType) {
            case AGENT:
            case MANAGER:
                return getAgent(dataId);
            case NATURAL:
                return getNaturalPerson(dataId);
            case LEGAL:
                return getLegalPerson(dataId);
            default:
                return null;
        }
    }



    //region Factory Methods
    //region Generic Factories
    private <T> T getObject(ObjectFactory<T> factory, String sqlQuery) throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(sqlQuery);
        ResultSet rSet = stmt.executeQuery();

        T result = rSet.next()
                ? factory.createObject(rSet)
                : null;

        stmt.close();
        conn.close();

        return result;
    }

    private <T> ArrayList<T> getObjects(ObjectFactory<T> factory, String sqlQuery) throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(sqlQuery);
        ResultSet rSet = stmt.executeQuery();

        ArrayList<T> list = new ArrayList<T>();
        while (rSet.next())
            list.add(factory.createObject(rSet));

        stmt.close();
        conn.close();

        return list;
    }

    private void executeSQL(String sqlQuery)  throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(sqlQuery);
        stmt.executeUpdate();

        stmt.close();
        conn.close();

    }
    //endregion

    //region NaturalPerson Factories
    public NaturalPerson getNaturalPerson(int NaturalPersonID) throws SQLException {
        String sql = "SELECT NaturalPersonID, FirstName, SecondName, LastName, DateOfBirth, AgentID"
                        + " FROM NATURAL_PERSONS"
                        + " WHERE NaturalPersonID = " + NaturalPersonID;

        return getObject(NaturalPersonFactory.getInstance(), sql);
    }

    public ArrayList<NaturalPerson> getNaturalPersons() throws SQLException {
        String sql = "SELECT NaturalPersonID, FirstName, SecondName, LastName, DateOfBirth, AgentID"
                        + " FROM NATURAL_PERSONS";
        return getObjects(NaturalPersonFactory.getInstance(), sql);
    }

    public ArrayList<NaturalPerson> getNaturalPersons(Agent agent) throws SQLException {
        String sql = "SELECT NaturalPersonID, FirstName, SecondName, LastName, DateOfBirth, AgentID"
                        + " FROM NATURAL_PERSONS"
                        + " WHERE AgentID = " + agent.getAgentId();
        return getObjects(NaturalPersonFactory.getInstance(), sql);
    }

    public void updateNaturalPerson(NaturalPerson naturalPerson)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE NATURAL_PERSONS"
                        + " SET FirstName = ?,"
                        +" SecondName = ?,"
                        +" LastName = ?,"
                        +" DateOfBirth = ?,"
                        +" AgentID = ?"
                        +" WHERE NaturalPersonID = ?");
        stmt.setString(1,naturalPerson.getFirstName());
        stmt.setString(2,naturalPerson.getSecondName());
        stmt.setString(3,naturalPerson.getLastName());
        stmt.setDate(4,new java.sql.Date(naturalPerson.getDateOfBirth().getTime()));
        stmt.setInt(5, naturalPerson.getAgentId());
        stmt.setInt(6,naturalPerson.getClientId());
        stmt.executeUpdate();

        stmt.close();
        conn.close();
    }

    public NaturalPerson createNaturalPerson(String firstName, String secondName,String lastName, Date dateOfBirth, int agentId)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);
        PreparedStatement selectSequence = conn.prepareStatement("select natural_persons_s.nextval as seqVal from dual");
        ResultSet resultSet = selectSequence.executeQuery();
        //TODO return dat shit or the whole object
        int naturalPersonId = resultSet.getInt("seqVal");
        resultSet.close();
        selectSequence.close();

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO NATURAL_PERSONS (NaturalPersonID,FirstName, SecondName, LastName, DateOfBirth, AgentID)"
                        + " VALUES (?, ?,?,?,?,?)");
        stmt.setInt(1,naturalPersonId);
        stmt.setString(2,firstName);
        stmt.setString(3,secondName);
        stmt.setString(4,lastName);
        stmt.setDate(5,new java.sql.Date(dateOfBirth.getTime()));
        stmt.setInt(6,agentId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();
        return new NaturalPerson(naturalPersonId,agentId,firstName,secondName,lastName,dateOfBirth);

    }

    public void  deleteNaturalPerson(int ClientId)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM NATURAL_PERSONS"
                        + " WHERE NaturalPersonID = ?");
        stmt.setInt(1, ClientId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();
        //TODO delete cascade?
    }
    //endregion

    //region LegalPerson Factories
    public LegalPerson getLegalPerson(int clientId) throws SQLException {
        String sql = "SELECT LegalPersonID, LegalName, Address, vatin, AgentID"
                + " FROM Legal_PERSONS"
                + " WHERE LegalPersonID = " + clientId;
        return getObject(LegalPersonFactory.getInstance(), sql);
    }

    public ArrayList<LegalPerson> getLegalPersons() throws SQLException {
        String sql = "SELECT LegalPersonID, LegalName, Address, vatin, AgentID"
                        + " FROM LEGAL_PERSONS";
        return getObjects(LegalPersonFactory.getInstance(), sql);
    }

    public ArrayList<LegalPerson> getLegalPersons(Agent agent) throws SQLException {
        String sql = "SELECT LegalPersonID, LegalName, Address, vatin, AgentID"
                + " FROM LEGAL_PERSONS"
                + " WHERE AgentID = " + agent.getAgentId();
        return getObjects(LegalPersonFactory.getInstance(), sql);
    }

    public void updateLegalPerson(LegalPerson legalPerson)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE Legal_PERSONS"
                        + " SET LegalName = ?,"
                        +" Address = ?,"
                        +" vatin = ?,"
                        +" AgentID = ?"
                        + " wHERE LegalPersonID = ? ");
        stmt.setString(1, legalPerson.getName());
        stmt.setString(2,legalPerson.getAddress());
        stmt.setString(3,legalPerson.getVatin());
        stmt.setInt(4, legalPerson.getAgentId());
        stmt.setInt(5, legalPerson.getClientId());
        stmt.executeUpdate();

        stmt.close();
        conn.close();
    }

    public LegalPerson createLegalPerson(String legalName,String address, String vatin, int agentId)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);
        PreparedStatement selectSequence = conn.prepareStatement("select legal_persons_s.nextval as seqVal from dual");
        ResultSet resultSet = selectSequence.executeQuery();
        //TODO return dat shit or the whole object
        int legalPersonId = resultSet.getInt("seqVal");
        resultSet.close();
        selectSequence.close();

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Legal_PERSONS (LegalName, Address, vatin, AgentID,legalPersonId)"
                    + " VALUES (?,?,?,?,?)");
        stmt.setString(1,legalName);
        stmt.setString(2,address);
        stmt.setString(3,vatin);
        stmt.setInt(4, agentId);
        stmt.setInt(5,legalPersonId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();

        return new LegalPerson(legalPersonId,agentId,legalName,vatin,address);
    }

    public void  deleteLegalPerson(int ClientId)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM Legal_PERSONS"
                        + " WHERE LegalPersonID = ?");
        stmt.setInt(1, ClientId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();
        //TODO delete cascade?
    }
    //endregion

    //region Agent Factories
    public Agent getAgent(int agentId) throws SQLException {
        String sql = "SELECT AgentId,FirstName,SecondName,LastName,HiringDate,QuitDate"
                + " FROM AGENTS"
                + " WHERE AgentId = " +  agentId;
        return getObject(AgentFactory.getInstance(), sql);
    }

    public ArrayList<Agent> getAgents() throws SQLException {
        String sql = "SELECT AgentId,FirstName,SecondName,LastName,HiringDate,QuitDate"
                + " FROM AGENTS";
        return getObjects(AgentFactory.getInstance(), sql);
    }

    public void updateAgent(Agent agent)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE AGENTS"
                        + " SET FirstName = ?,"
                        +"SecondName = ?,"
                        +"LastName = ?,"
                        +"HiringDate = ?,"
                        +"QuitDate = ?"
                        + "WHERE AgentId = ?");
        stmt.setString(1,agent.getFirstName());
        stmt.setString(2,agent.getSecondName());
        stmt.setString(3,agent.getLastName());
        stmt.setDate(4, new java.sql.Date(agent.getHiringDate().getTime()));
        stmt.setDate(5,new java.sql.Date(agent.getQuitDate().getTime()));
        stmt.setInt(6,agent.getAgentId());
        stmt.executeUpdate();

        stmt.close();
        conn.close();
    }

    public Agent createAgent(String firstName, String secondName, String lastName, Date hiringDate, Date quitDate)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);
        PreparedStatement selectSequence = conn.prepareStatement("select agents_s.nextval as seqVal from dual");
        ResultSet resultSet = selectSequence.executeQuery();
        //TODO return dat shit or the whole object
        int agentId = resultSet.getInt("seqVal");
        resultSet.close();
        selectSequence.close();

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO AGENTS (FirstName,SecondName,LastName,HiringDate,QuitDate,AgentID)"
                + "VALUES (?,?,?,?,?,?)");
        stmt.setString(1,firstName);
        stmt.setString(2,secondName);
        stmt.setString(3,lastName);
        stmt.setDate(4, new java.sql.Date(hiringDate.getTime()));
        stmt.setDate(5, new java.sql.Date(quitDate.getTime()));
        stmt.setInt(6,agentId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();

        return new Agent(agentId, firstName,secondName,lastName,hiringDate,quitDate);
    }

    public void  deleteAgent(int AgentId)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM AGENTS"
                        + " WHERE AgentId = ? ");
        stmt.setInt(1, AgentId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();
    }
    //endregion

    //region Company Factories
    public Company getCompany(int companyId) throws SQLException {

        String sql = "SELECT CompanyID, CompanyName,ParentCompanyId,CompanyDescription "
                + " FROM COMPANIES"
                + " WHERE CompanyId = " + companyId;
        return getObject(CompanyFactory.getInstance(), sql);
    }

    public ArrayList<Company> getCompanies() throws SQLException {
        String sql = "SELECT CompanyID, CompanyName,ParentCompanyId,CompanyDescription "
                + " FROM COMPANIES";
        return getObjects(CompanyFactory.getInstance(), sql);
    }
    public void updateCompany(Company company)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE COMPANIES"
                        + " SET CompanyName = ?,"
                        +" ParentCompanyId = ?,"
                        +" CompanyDescription = ?"
                        + " WHERE CompanyId = ?");
        stmt.setString(1,company.getName());
        stmt.setInt(2, company.getParentCompanyId());
        stmt.setString(3,company.getDescription());
        stmt.setInt(4,company.getCompanyId());
        stmt.executeUpdate();

        stmt.close();
        conn.close();
    }

    public Company createCompany(String companyName, int parentCompanyId, String companyDescription)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);
        PreparedStatement selectSequence = conn.prepareStatement("select companies_s.nextval as seqVal from dual");
        ResultSet resultSet = selectSequence.executeQuery();
        //TODO return dat shit or the whole object
        int companyId = resultSet.getInt("seqVal");
        resultSet.close();
        selectSequence.close();

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO COMPANIES (CompanyName,ParentCompanyId,CompanyDescription,CompanyID)"
                        + " VALUES (?,?,?,?)");
        stmt.setString(1,companyName);
        stmt.setInt(2, parentCompanyId);
        stmt.setString(3, companyDescription);
        stmt.setInt(4,companyId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();

        return new Company(companyId,companyName,parentCompanyId,companyDescription);
    }

    public void  deleteCompany(int companyId)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM COMPANIES"
                        + " WHERE CompanyId = ?");
        stmt.setInt(1,companyId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();
        //TODO delete cascade?
    }
    //endregion

    //region CompanyByInsuranceType Factories
    public CompanyByInsuranceType getCompanyByInsuranceType(int companyByInsTypeId) throws SQLException {
        String sql = "SELECT companyByInsuranceTypeID, companyID, insuranceTypeID"
                + " FROM companies_by_insurance_type"
                + " WHERE companyByInsuranceTypeID = " + companyByInsTypeId;
        return getObject(CompanyByInsuranceTypeFactory.getInstance(), sql);
    }

    public ArrayList<CompanyByInsuranceType> getCompaniesByInsuranceTypes() throws SQLException {
        String sql = "SELECT companyByInsuranceTypeID, companyID, insuranceTypeID"
                + " FROM companies_by_insurance_type";
        return getObjects(CompanyByInsuranceTypeFactory.getInstance(), sql);
    }

    public ArrayList<CompanyByInsuranceType> getCompaniesByInsuranceTypes(Company company) throws SQLException {
        String sql = "SELECT companyByInsuranceTypeID, companyID, insuranceTypeID"
                + " FROM companies_by_insurance_type"
                + " WHERE companyID = " + company.getCompanyId();
        return getObjects(CompanyByInsuranceTypeFactory.getInstance(), sql);
    }
    public void updateCompanyByInsuranceType(CompanyByInsuranceType companyByInsuranceType)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE companies_by_insurance_type"
                        + " SET companyID = ?,"
                        +" insuranceTypeID = ?"
                        + " where companyByInsuranceTypeID = ?");
        stmt.setInt(1, companyByInsuranceType.getCompanyId());
        stmt.setInt(2,companyByInsuranceType.getInsuranceTypeId());
        stmt.setInt(3, companyByInsuranceType.getCompanyByInsuranceTypeId());
        stmt.executeUpdate();

        stmt.close();
        conn.close();

    }

    public CompanyByInsuranceType createCompanyByInsuranceType(int companyId, int insuranceId)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);
        PreparedStatement selectSequence = conn.prepareStatement("select companies_by_insurance_types_s.nextval as seqVal from dual");
        ResultSet resultSet = selectSequence.executeQuery();
        //TODO return dat shit or the whole object
        int cbitId = resultSet.getInt("seqVal");
        resultSet.close();
        selectSequence.close();

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO companies_by_insurance_type (companyID, insuranceTypeID,CompanyByInsuranceTypeID)"
                        + " VALUES (?,?,?)");
        stmt.setInt(1,companyId);
        stmt.setInt(2,insuranceId);
        stmt.setInt(3,cbitId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();
        return new CompanyByInsuranceType(cbitId,companyId,insuranceId);
    }

    public void  deleteCompanyByInsuranceType(int companyByInsuranceTypeId)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM companies_by_insurance_type"
                        + " WHERE companyByInsuranceTypeID = ?");
        stmt.setInt(1,companyByInsuranceTypeId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();
        //TODO delete cascade?
    }
    //endregion

    //region Insurance Factories

    public  Insurance getInsurance(int insuranceId) throws SQLException{
       String sql="SELECT ins.InsuranceID as InsuranceID," +
                " ins.ClientID as ClientID," +
                " ins.ClientType as ClientType," +
                " ins.companybyinsurancetypeid as companybyinsurancetypeid,"+
                " ins.AgentId as AgentId,"+
                " ins.BaseValue as BaseValue,"+
                " comp.companyName as companyName,"+
                " comp.insuranceTypeName as insuranceTypeName,"+
                " agents.firstName||' '||agents.secondName ||' '|| agents.lastName as agentName,"+
                " CASE WHEN ClientType like 'NATURAL' THEN natural_persons.firstname||' '||natural_persons.secondName||' '||natural_persons.lastname"+
                     " when ClientType like 'LEGAL' then legal_persons.legalname"+
                " end as ClientName"+
        " FROM INSURANCES ins"+
        " where ins.InsuranceID = "+ insuranceId+
        " inner join (select companies.companyname as companyName,"+
                " companies_by_insurance_type.companybyinsurancetypeid as companybyinsurancetypeid,"+
        " insurance_types.insurancetypename as insurancetypename"+
        " from companies_by_insurance_type"+
        " left outer join companies"+
        " on companies.companyid = companies_by_insurance_type.companyid"+
        " left outer join insurance_types"+
        " on insurance_types.insurancetypeid=companies_by_insurance_type.insurancetypeid) comp"+
        " on ins.companybyinsurancetypeid = comp.companybyinsurancetypeid"+
        " inner join agents"+
        " on agents.agentId=ins.agentid"+
        " left outer join legal_persons"+
        " on legal_persons.legalpersonid = ins.clientid"+
        " left outer join natural_persons"+
        " on natural_persons.naturalpersonid = ins.clientid";

        return getObject(InsuranceFactory.getInstance(), sql);
    }

    public ArrayList<Insurance> getInsurances() throws SQLException {
        String sql = "SELECT ins.InsuranceID as InsuranceID," +
                " ins.ClientID as ClientID," +
                " ins.ClientType as ClientType," +
                " ins.companybyinsurancetypeid as companybyinsurancetypeid,"+
                " ins.AgentId as AgentId,"+
                " ins.BaseValue as BaseValue,"+
                " comp.companyName as companyName,"+
                " comp.insuranceTypeName as insuranceTypeName,"+
                " agents.firstName||' '||agents.secondName ||' '|| agents.lastName as agentName,"+
                " CASE WHEN ClientType like 'NATURAL' THEN natural_persons.firstname||' '||natural_persons.secondName||' '||natural_persons.lastname"+
                " when ClientType like 'LEGAL' then legal_persons.legalname"+
                " end as ClientName"+
                " FROM INSURANCES ins"+
                " inner join (select companies.companyname as companyName,"+
                " companies_by_insurance_type.companybyinsurancetypeid as companybyinsurancetypeid,"+
                " insurance_types.insurancetypename as insurancetypename"+
                " from companies_by_insurance_type"+
                " left outer join companies"+
                " on companies.companyid = companies_by_insurance_type.companyid"+
                " left outer join insurance_types"+
                " on insurance_types.insurancetypeid=companies_by_insurance_type.insurancetypeid) comp"+
                " on ins.companybyinsurancetypeid = comp.companybyinsurancetypeid"+
                " inner join agents"+
                " on agents.agentId=ins.agentid"+
                " left outer join legal_persons"+
                " on legal_persons.legalpersonid = ins.clientid"+
                " left outer join natural_persons"+
                " on natural_persons.naturalpersonid = ins.clientid";
        return getObjects(InsuranceFactory.getInstance(), sql);
    }

    public ArrayList<Insurance> getInsurances(NaturalPerson person) throws SQLException {
        String sql = "SELECT ins.InsuranceID as InsuranceID," +
                " ins.ClientID as ClientID," +
                " ins.ClientType as ClientType," +
                " ins.companybyinsurancetypeid as companybyinsurancetypeid,"+
                " ins.AgentId as AgentId,"+
                " ins.BaseValue as BaseValue,"+
                " comp.companyName as companyName,"+
                " comp.insuranceTypeName as insuranceTypeName,"+
                " agents.firstName||' '||agents.secondName ||' '|| agents.lastName as agentName,"+
                " CASE WHEN ClientType like 'NATURAL' THEN natural_persons.firstname||' '||natural_persons.secondName||' '||natural_persons.lastname"+
                " when ClientType like 'LEGAL' then legal_persons.legalname"+
                " end as ClientName"+
                " FROM INSURANCES ins"+
                " WHERE ClientID = " + person.getClientId()  +
                " AND ClientType = 'NATURAL'"+
                " inner join (select companies.companyname as companyName,"+
                " companies_by_insurance_type.companybyinsurancetypeid as companybyinsurancetypeid,"+
                " insurance_types.insurancetypename as insurancetypename"+
                " from companies_by_insurance_type"+
                " left outer join companies"+
                " on companies.companyid = companies_by_insurance_type.companyid"+
                " left outer join insurance_types"+
                " on insurance_types.insurancetypeid=companies_by_insurance_type.insurancetypeid) comp"+
                " on ins.companybyinsurancetypeid = comp.companybyinsurancetypeid"+
                " inner join agents"+
                " on agents.agentId=ins.agentid"+
                " left outer join legal_persons"+
                " on legal_persons.legalpersonid = ins.clientid"+
                " left outer join natural_persons"+
                " on natural_persons.naturalpersonid = ins.clientid";
        return getObjects(InsuranceFactory.getInstance(), sql);
    }

    public ArrayList<Insurance> getInsurances(LegalPerson person) throws SQLException {
        String sql = "SELECT ins.InsuranceID as InsuranceID," +
                " ins.ClientID as ClientID," +
                " ins.ClientType as ClientType," +
                " ins.companybyinsurancetypeid as companybyinsurancetypeid,"+
                " ins.AgentId as AgentId,"+
                " ins.BaseValue as BaseValue,"+
                " comp.companyName as companyName,"+
                " comp.insuranceTypeName as insuranceTypeName,"+
                " agents.firstName||' '||agents.secondName ||' '|| agents.lastName as agentName,"+
                " CASE WHEN ClientType like 'NATURAL' THEN natural_persons.firstname||' '||natural_persons.secondName||' '||natural_persons.lastname"+
                " when ClientType like 'LEGAL' then legal_persons.legalname"+
                " end as ClientName"+
                " FROM INSURANCES ins"+
                " WHERE ClientID = " + person.getClientId() +
                " AND ClientType = 'LEGAL'" +
                " inner join (select companies.companyname as companyName,"+
                " companies_by_insurance_type.companybyinsurancetypeid as companybyinsurancetypeid,"+
                " insurance_types.insurancetypename as insurancetypename"+
                " from companies_by_insurance_type"+
                " left outer join companies"+
                " on companies.companyid = companies_by_insurance_type.companyid"+
                " left outer join insurance_types"+
                " on insurance_types.insurancetypeid=companies_by_insurance_type.insurancetypeid) comp"+
                " on ins.companybyinsurancetypeid = comp.companybyinsurancetypeid"+
                " inner join agents"+
                " on agents.agentId=ins.agentid"+
                " left outer join legal_persons"+
                " on legal_persons.legalpersonid = ins.clientid"+
                " left outer join natural_persons"+
                " on natural_persons.naturalpersonid = ins.clientid";
        return getObjects(InsuranceFactory.getInstance(), sql);
    }

    public ArrayList<Insurance> getInsurances(Agent agent) throws SQLException {
        String sql = "SELECT ins.InsuranceID as InsuranceID," +
                " ins.ClientID as ClientID," +
                " ins.ClientType as ClientType," +
                " ins.companybyinsurancetypeid as companybyinsurancetypeid,"+
                " ins.AgentId as AgentId,"+
                " ins.BaseValue as BaseValue,"+
                " comp.companyName as companyName,"+
                " comp.insuranceTypeName as insuranceTypeName,"+
                " agents.firstName||' '||agents.secondName ||' '|| agents.lastName as agentName,"+
                " CASE WHEN ClientType like 'NATURAL' THEN natural_persons.firstname||' '||natural_persons.secondName||' '||natural_persons.lastname"+
                " when ClientType like 'LEGAL' then legal_persons.legalname"+
                " end as ClientName"+
                " FROM INSURANCES ins"+
                "  WHERE AgentID = " + agent.getAgentId() +
                " inner join (select companies.companyname as companyName,"+
                " companies_by_insurance_type.companybyinsurancetypeid as companybyinsurancetypeid,"+
                " insurance_types.insurancetypename as insurancetypename"+
                " from companies_by_insurance_type"+
                " left outer join companies"+
                " on companies.companyid = companies_by_insurance_type.companyid"+
                " left outer join insurance_types"+
                " on insurance_types.insurancetypeid=companies_by_insurance_type.insurancetypeid) comp"+
                " on ins.companybyinsurancetypeid = comp.companybyinsurancetypeid"+
                " inner join agents"+
                " on agents.agentId=ins.agentid"+
                " left outer join legal_persons"+
                " on legal_persons.legalpersonid = ins.clientid"+
                " left outer join natural_persons"+
                " on natural_persons.naturalpersonid = ins.clientid";
        return getObjects(InsuranceFactory.getInstance(), sql);
    }
    public void updateInsurance(Insurance insurance)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE INSURANCES"
                        + "SET ClientID = ?,"
                        +" ClientType = ?,"
                        +" CompanyByInsuranceTypeID = ?,"
                        + " AgentId = ?,"
                        + " BaseValue = ?"
                        + " Where InsuranceId = ?");
        stmt.setInt(1,insurance.getClientId());
        stmt.setString(2, insurance.getClientType());
        stmt.setInt(3,insurance.getCompanyByInsuranceTypeId());
        stmt.setInt(4,insurance.getAgentId());
        stmt.setDouble(5,insurance.getBaseValue());
        stmt.setInt(6,insurance.getInsuranceId());
        stmt.executeUpdate();

        stmt.close();
        conn.close();
    }

    public Insurance createInsurance(int clientId, String clientType, int cbitID,int agentId, double baseValue)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);
        PreparedStatement selectSequence = conn.prepareStatement("select insurances_s.nextval as seqVal from dual");
        ResultSet resultSet = selectSequence.executeQuery();
        int insuranceId = resultSet.getInt("seqVal");
        resultSet.close();
        selectSequence.close();

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO INSURANCES (ClientID, ClientType, CompanyByInsuranceTypeID, AgentId, BaseValue, InsuranceID)"
                        + " VALUES (?,?,?,?,?,?)");
        stmt.setInt(1,clientId);
        stmt.setString(2, clientType);
        stmt.setInt(3, cbitID);
        stmt.setInt(4,agentId);
        stmt.setDouble(5,baseValue);
        stmt.setInt(6,insuranceId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();
        return new Insurance(insuranceId,clientId,clientType,cbitID,agentId,baseValue);
    }

    public void  deleteInsurance(int insuranceId)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM INSURANCES"
                        + " WHERE insuranceID = ?");
        stmt.setInt(1,insuranceId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();
        //TODO delete cascade?
    }
    //endregion

    //region InsuranceType Factories
    public InsuranceType getInsuranceType(int insuranceTypeId) throws SQLException {
        String sql = "SELECT InsuranceTypeID, InsuranceTypeName,InsuranceTypeDescription"
                + " FROM insurance_types"
                + " WHERE InsuranceTypeId = " + insuranceTypeId;
        return getObject(InsuranceTypeFactory.getInstance(), sql);
    }

    public ArrayList<InsuranceType> getInsuranceTypes() throws SQLException {
        String sql = "SELECT InsuranceTypeID, InsuranceTypeName,InsuranceTypeDescription"
                + " FROM insurance_types";
        return getObjects(InsuranceTypeFactory.getInstance(), sql);
    }
    public void updateInsuranceType(InsuranceType insuranceType)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE insurance_types"
                        + " SET InsuranceTypeName = ?,"
                        +" InsuranceTypeDescription = ?"
                        +" Where InsuranceTypeId = ?");
        stmt.setString(1,insuranceType.getName());
        stmt.setString(2, insuranceType.getDescription());
        stmt.setInt(3,insuranceType.getInsuranceTypeId());
        stmt.executeUpdate();

        stmt.close();
        conn.close();
    }

    public InsuranceType createInsuranceType(String insuranceTypeName, String insuranceTypeDescription)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);
        PreparedStatement selectSequence = conn.prepareStatement("select insurance_types_s.nextval as seqVal from dual");
        ResultSet resultSet = selectSequence.executeQuery();
        //TODO return dat shit or the whole object
        int insuranceTypeId = resultSet.getInt("seqVal");
        resultSet.close();
        selectSequence.close();

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO insurance_types (InsuranceTypeName,InsuranceTypeDescription,InsuranceTypeID)"
                        + " VALUES (?,?,?)");
        stmt.setString(1,insuranceTypeName);
        stmt.setString(2, insuranceTypeDescription);
        stmt.setInt(3,insuranceTypeId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();

        return new InsuranceType(insuranceTypeId, insuranceTypeName,insuranceTypeDescription);
    }

    public void  deleteInsuranceType(int insuranceTypeId)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM insurance_types"
                        + "WHERE InsuranceTypeId = ?");
        stmt.setInt(1,insuranceTypeId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();
        //TODO delete cascade?
    }
    //endregion

    //region Attribute Factories
    public AttributeType getAttributeType(int attributeTypeId) throws SQLException {
        String sql = "SELECT AttributeTypeID,AttributeTypeName,AttributeTypeDescription"
                + " FROM ATTRIBUTE_TYPES"
                + " WHERE AttributeTypeID = " + attributeTypeId;
        return getObject(AttributeTypeFactory.getInstance(), sql);
    }

    public ArrayList<AttributeType> getAttributeTypes() throws SQLException {
        String sql = "SELECT AttributeTypeID,AttributeTypeName,AttributeTypeDescription"
                + " FROM ATTRIBUTE_TYPES";
        return getObjects(AttributeTypeFactory.getInstance(), sql);
    }
    public void updateAttributeType(AttributeType attributeType)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE ATTRIBUTE_TYPES"
                        + " SET AttributeTypeName = ?,"
                        +" AttributeTypeDescription = ?"
                        +" Where AttributeTypeId = ?");
        stmt.setString(1,attributeType.getName());
        stmt.setString(2,attributeType.getDescription());
        stmt.setInt(3,attributeType.getTypeId());
        stmt.executeUpdate();

        stmt.close();
        conn.close();
    }

    public AttributeType createAttributeType(String attributeTypeName, String attributeTypeDescription)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);
        PreparedStatement selectSequence = conn.prepareStatement("select attribute_types_s.nextval as seqVal from dual");
        ResultSet resultSet = selectSequence.executeQuery();
        //TODO return dat shit or the whole object
        int attributeTypeId = resultSet.getInt("seqVal");
        resultSet.close();
        selectSequence.close();

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO ATTRIBUTE_TYPES (AttributeTypeName,AttributeTypeDescription,AttributeTypeID)"
                        + " VALUES (?,?,?)");
        stmt.setString(1,attributeTypeName);
        stmt.setString(2,attributeTypeDescription);
        stmt.setInt(3,attributeTypeId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();

        return new AttributeType(attributeTypeId,attributeTypeName,attributeTypeDescription);
    }

    public void  deleteAttributeType(int attributeTypeId)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM ATTRIBUTE_TYPES"
                        + " WHERE AttributeTypeId = ?");
        stmt.setInt(1,attributeTypeId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();
        //TODO delete cascade?
    }

    public InsuranceAttribute getInsuranceAttribute(int insuranceAttributeId) throws SQLException {
        String sql = "SELECT AttributeID, AttributeTypeId,AttributeValue,InsuranceId "
                + " FROM INSURANCE_ATTRIBUTES"
                + " WHERE AttributeId = " + insuranceAttributeId;
        return getObject(InsuranceAttributeFactory.getInstance(), sql);
    }

    public ArrayList<InsuranceAttribute> getInsuranceAttributes() throws SQLException {
        String sql = "SELECT AttributeID, AttributeTypeId,AttributeValue,InsuranceId "
                + " FROM INSURANCE_ATTRIBUTES";
        return getObjects(InsuranceAttributeFactory.getInstance(), sql);
    }

    public ArrayList<InsuranceAttribute> getInsuranceAttributes(Insurance insurance) throws SQLException {
        String sql = "SELECT AttributeID, AttributeTypeId,AttributeValue,InsuranceId "
                + " FROM INSURANCE_ATTRIBUTES"
                + " WHERE InsuranceId = " + insurance.getInsuranceId();
        return getObjects(InsuranceAttributeFactory.getInstance(), sql);
    }
    public void updateInsuranceAttribute(InsuranceAttribute insuranceAttribute)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "UPDATE INSURANCE_ATTRIBUTES"
                        + " SET AttributeTypeId = ?," + insuranceAttribute.getAttributeTypeId()
                        +" AttributeValue = ?," + insuranceAttribute.getAttributeValue()
                        +" InsuranceId = ?" +insuranceAttribute.getInsuranceId()
                        + " where AttributeId = ?");
        stmt.setInt(1,insuranceAttribute.getTypeId());
        stmt.setString(2,insuranceAttribute.getAttributeValue());
        stmt.setInt(3,insuranceAttribute.getInsuranceId());
        stmt.setInt(4,insuranceAttribute.getAttributeId());
        stmt.executeUpdate();

        stmt.close();
        conn.close();
    }

    public InsuranceAttribute createInsuranceAttribute(int attributeTypeId,String attributeValue,int insuranceId)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);
        PreparedStatement selectSequence = conn.prepareStatement("select insurance_attributes_s.nextval as seqVal from dual");
        ResultSet resultSet = selectSequence.executeQuery();
        //TODO return dat shit or the whole object
        int attributeId = resultSet.getInt("seqVal");
        resultSet.close();
        selectSequence.close();

        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO INSURANCE_ATTRIBUTES (AttributeTypeId,AttributeValue,InsuranceId,AttributeID)"
                        + " VALUES (?,?,?,?)");
        stmt.setInt(1,attributeTypeId);
        stmt.setString(2, attributeValue);
        stmt.setInt(3, insuranceId);
        stmt.setInt(4,attributeId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();

        return new InsuranceAttribute(attributeId,insuranceId,attributeValue,attributeTypeId);
    }

    public void  deleteInsuranceAttribute(int insuranceAttributeId)      throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl, username, password);

        PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM INSURANCE_ATTRIBUTES"
                        + " WHERE AttributeId = ?");
        stmt.setInt(1,insuranceAttributeId);
        stmt.executeUpdate();

        stmt.close();
        conn.close();
        //TODO delete cascade?
    }
    //endregion
    //endregion

    private static abstract class ObjectFactory<T> {
        public abstract T createObject(ResultSet resultSet) throws SQLException;
    }

    private static class NaturalPersonFactory extends ObjectFactory<NaturalPerson> {
        private static NaturalPersonFactory instance = new NaturalPersonFactory();

        public static NaturalPersonFactory getInstance() {
            return instance;
        }

        @Override
        public NaturalPerson createObject(ResultSet rSet) throws SQLException {
            return new NaturalPerson(rSet.getInt("NaturalPersonID"),
                    rSet.getInt("AgentId"),
                    rSet.getString("FirstName"),
                    rSet.getString("SecondName"),
                    rSet.getString("LastName"),
                    rSet.getDate("DateOfBirth"));
        }
    }

    private static class LegalPersonFactory extends ObjectFactory<LegalPerson> {
        private static LegalPersonFactory instance = new LegalPersonFactory();

        public static LegalPersonFactory getInstance() {
            return instance;
        }

        @Override
        public LegalPerson createObject(ResultSet rSet) throws SQLException {
            return new LegalPerson(rSet.getInt("LegalPersonID"),
                    rSet.getInt("AgentId"),
                    rSet.getString("LegalName"),
                    rSet.getString("vatin"),
                    rSet.getString("Address"));
        }
    }

    private static class AgentFactory extends ObjectFactory<Agent> {
        private static AgentFactory instance = new AgentFactory();

        public static AgentFactory getInstance() {
            return instance;
        }

        @Override
        public Agent createObject(ResultSet rSet) throws SQLException {
            return new Agent(rSet.getInt("AgentId"),
                    rSet.getString("FirstName"),
                    rSet.getString("SecondName"),
                    rSet.getString("LastName"),
                    rSet.getDate("HiringDate"),
                    rSet.getDate("QuitDate"));
        }
    }

    private static class CompanyFactory extends ObjectFactory<Company> {
        private static CompanyFactory instance = new CompanyFactory();

        public static CompanyFactory getInstance() {
            return instance;
        }

        @Override
        public Company createObject(ResultSet rSet) throws SQLException {
            return new Company(rSet.getInt("CompanyID"),
                    rSet.getString("CompanyName"),
                    rSet.getInt("ParentCompanyId"),
                    rSet.getString("CompanyDescription"));
        }
    }

    private static class CompanyByInsuranceTypeFactory extends ObjectFactory<CompanyByInsuranceType> {
        private static CompanyByInsuranceTypeFactory instance = new CompanyByInsuranceTypeFactory();

        public static CompanyByInsuranceTypeFactory getInstance() {
            return instance;
        }

        @Override
        public CompanyByInsuranceType createObject(ResultSet rSet) throws SQLException {
            return new CompanyByInsuranceType(rSet.getInt("CompanyByInsuranceTypeID"),
                    rSet.getInt("CompanyID"),
                    rSet.getInt("InsuranceTypeID"));
        }
    }

    private static class InsuranceFactory extends ObjectFactory<Insurance> {
        private static InsuranceFactory instance = new InsuranceFactory();

        public static InsuranceFactory getInstance() {
            return instance;
        }

        @Override
        public Insurance createObject(ResultSet rSet) throws SQLException {
            return new Insurance(rSet.getInt("InsuranceID"),
                    rSet.getInt("ClientID"),
                    rSet.getString("ClientType"),
                    rSet.getInt("CompanyByInsuranceTypeID"),
                    rSet.getInt("AgentId"),
                    rSet.getDouble("BaseValue"),
                    rSet.getString("agentName"),
                    rSet.getString("ClientName"),
                    rSet.getString("companyName"),
                    rSet.getString("insuranceTypeName"));
        }
    }

    private static class InsuranceTypeFactory extends ObjectFactory<InsuranceType> {
        private static InsuranceTypeFactory instance = new InsuranceTypeFactory();

        public static InsuranceTypeFactory getInstance() {
            return instance;
        }

        @Override
        public InsuranceType createObject(ResultSet rSet) throws SQLException {
            return new InsuranceType(rSet.getInt("InsuranceTypeID"),
                    rSet.getString("InsuranceTypeName"),
                    rSet.getString("InsuranceTypeDescription"));
        }
    }

    private static class AttributeTypeFactory extends ObjectFactory<AttributeType> {
        private static AttributeTypeFactory instance = new AttributeTypeFactory();

        public static AttributeTypeFactory getInstance() {
            return instance;
        }

        @Override
        public AttributeType createObject(ResultSet rSet) throws SQLException {
            return new AttributeType(rSet.getInt("AttributeTypeID"),
                    rSet.getString("AttributeTypeName"),
                    rSet.getString("AttributeDescription"));
        }
    }

    private static class InsuranceAttributeFactory extends ObjectFactory<InsuranceAttribute> {
        private static InsuranceAttributeFactory instance = new InsuranceAttributeFactory();

        public static InsuranceAttributeFactory getInstance() {
            return instance;
        }

        @Override
        public InsuranceAttribute createObject(ResultSet rSet) throws SQLException {
            return new InsuranceAttribute(rSet.getInt("AttributeID"),
                    rSet.getInt("AttributeTypeId"),
                    rSet.getString("AttributeValue"),
                    rSet.getInt("InsuranceId"));
        }
    }
      //TODO delete when debug done and crear package list
    public static void main(String[] args) {
        try {
            // will throw exception if fail to log in
            ModelController.initializeInstance("jdbc:oracle:thin:@5.19.237.145:65432:xe",
                    "system",
                    "1234");
            ModelController instance = ModelController.getInstance();

            //TODO make nullable ints (like in base)
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
