/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.dao.APIManager;
import com.tms.hrdc.dao.CurrentUser;
import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.util.B2CUtil;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
//import com.tms.hrdc.util.Constants.APS.CUSTYPE;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.HttpUtil;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.sql.DataSource;
//import org.joget.api.model.JSONOrderedObject;
import org.apache.http.HttpHeaders;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.PluginThread;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class RegisterEmpAPS extends DefaultApplicationPlugin{

    @Override
    public String getName() {
        return "HRDC - EMPM - Register Employer to APS";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "To register employer to APS";
    }

    @Override
    public String getLabel() {
        return getName();
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        String json = "[{\n"
        + "    title : '"+getLabel()+"',\n"
        + "    properties : [{\n" 
        + "        name: 'type', " 
        + "        label: 'Process Type', " 
        + "         type: 'radio', " 
        + "         options : [\n" 
        + "             {value: 'registration', label : 'Registration Process'}," 
        + "             {value: 'deregistration', label : 'Deregistration Process'}" 
        + "            ]\n" 
        + "     }]\n"
        + "}]";
        return json;
    }
    
    public static final String B2C_DOMAIN_TEST = "hrdcorpb2cdev.onmicrosoft.com";

    PluginManager pm = null;
    WorkflowManager wm = null;
    WorkflowAssignment wfa = null;

    static String url_scheme = "";
    static String url_serverName = "";
    static String url_serverPort = "";
    static String url_contextPath = "";
    
    @Override
    public Object execute(Map props) {
        LogUtil.info("RegisterEmpAPS default plugin: ","start");
//        WorkflowAssignment wfa = (WorkflowAssignment) props.get("workflowAssignment");
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");

//        PluginManager pm = (PluginManager) props.get("pluginManager");
//        WorkflowManager wm = (WorkflowManager) pm.getBean("workflowManager");
        String type = (String) props.get("type");

        wfa = (WorkflowAssignment) props.get("workflowAssignment");
        pm = (PluginManager) props.get("pluginManager");
        wm = (WorkflowManager) pm.getBean("workflowManager");

        final String id = appService.getOriginProcessId(wfa.getProcessId());

        url_scheme = getPropertyString("url_scheme");
        url_serverName = getPropertyString("url_serverName");
        url_serverPort = getPropertyString("url_serverPort");
        url_contextPath = getPropertyString("url_contextPath");
        
        Thread checkingThread = new PluginThread(new Runnable() {
            @Override
            public void run() {
                sendUpdateToAPS(id,type);
            }
        });
        checkingThread.setDaemon(true);
        checkingThread.start();
        
//        sendUpdateToAPS(id,type);
        return null;
    }
    
    public void sendUpdateToAPS(String id, String type) {
        DBHandler db = new DBHandler();
        APIManager mgr = new APIManager(APIManager.APIType.APS);
        
        try{
            db.openConnection();
            String empId = "";
            switch(type){
                case "registration":
                    empId = CommonUtils.getEmpId_empReg(db, id);
                    if(empId.isEmpty()){
                        empId = CommonUtils.getEmplId_DeregWD(db, id);
                    }

                    completeQueryAct(db, id);
                break;

                case "deregistration":
                    empId = CommonUtils.getEmpId_empDereg(db, id);
                break;
            }

            JSONObject jsonBody = buildJSONbody(db, empId, id);
            HashMap header = new HashMap();
            header.put("api_id", mgr.getApi_id());
            header.put("api_key", mgr.getApi_key());
            
            HttpUtil http = new HttpUtil();
            http.setHeader(header);
            http.setBody(jsonBody);
            http.sendPostRequest(Constants.APS.APS_PROFILE_REG);
            
            JSONObject resp_data = http.getJSONResponse();
            int statusCode = http.getStatusCode();
            
            LogUtil.info("DATA SENT TO APS", 
                    "Status Code :"+Integer.toString(statusCode)
                    + ", Response: "+resp_data.toString());
            
            B2CUtil b2c = new B2CUtil(db, http, mgr);
        
            if(type.equals("registration")){
                jsonBody = buildB2CJSONbody(db, empId, id);

                b2c.createB2CUser(jsonBody);
            }else{
                //b2c.deleteB2CUser(); //FOR TESTING
                //moved to DeregisterTool.java - changed to scheduler
            }
            
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }
    }

    public JSONObject buildJSONbody(DBHandler db, String regAppId, String id) throws JSONException {
        
        EmpmObj emp = new EmpmObj(db, EmpmObj.BY_ID, regAppId);
        
        JSONObject profile_service = new JSONObject();
        JSONObject profile = new JSONObject();
        
        HashMap contactPerson = emp.getOneContactPerson();
        
        String cpName = "";
        String cpEmail = "";
        String cpDesg = "";
        String cpTel = "";
        
        if(contactPerson !=null){
            cpName = contactPerson.get("c_name")==null?"":contactPerson.get("c_name").toString();
            cpDesg = contactPerson.get("c_designation")==null?"":contactPerson.get("c_designation").toString();
            cpEmail = contactPerson.get("c_email")==null?"":contactPerson.get("c_email").toString();
            cpTel = contactPerson.get("c_tel_no")==null?"":contactPerson.get("c_tel_no").toString();
        }
        String status = "";
        String cessation_date = "";
        String remarks = "";
        String transaction_type = "";

        switch(getPropertyString("type")){
                    
            case "registration":
                status = Constants.APS.ACTIVE;
                cessation_date = "" ;
                remarks = Constants.APS.TXNTYPE.CREATE;
                transaction_type = Constants.APS.TXNTYPE.CREATE;
            break;

            case "deregistration":
                String deregId = id;
                status = Constants.APS.INACTIVE;
                cessation_date = getCessationDate(deregId);
                remarks = Constants.APS.TXNTYPE.CLOSE;
                transaction_type = Constants.APS.TXNTYPE.CLOSE;
            break;
        }
//        LogUtil.info("status, cessation_date, remarks, transaction type", status+", "+cessation_date+", "+remarks+ ", "+transaction_type);

        profile.put("status", status );
        profile.put("customer_type", Constants.APS.CUSTYPE.EMPR );
        profile.put("my_co_id", emp.getMycoid());
        profile.put("ic_number", "");
        profile.put("country", emp.getBusinessCountryName().equalsIgnoreCase("MALAYSIA")?
                                "MYS":
                                emp.getBusinessCountryName()
        );
        profile.put("name", emp.getCompName());
        profile.put("address1", emp.getBusinessAddress1());
        profile.put("address2", emp.getBusinessAddress2());
        profile.put("address3", emp.getBusinessAddress3());
        profile.put("city", emp.getBusinessCityName());
        profile.put("state", emp.getBusinessStateName());
        profile.put("postcode", emp.getBusinessPostcode());
        profile.put("email", emp.getPrimaryEmail());
        profile.put("tel_num", 
                emp.getField("c_empl_tel_no_pri")
                        .replace("-", "")
                        .replace(" ", "")
                        .replace("+", "")
        );
        profile.put("fax_num", "");
        profile.put("occupation", cpDesg);
        profile.put("payment_mode", "");
        profile.put("bank_swift_code", "");
        profile.put("bank_account_number", "");
        profile.put("contact_person_name", cpName);
        profile.put("contact_person_email", cpEmail);
        profile.put("contact_person_designation", cpDesg);
        profile.put("branch", (emp.isHQ()?"HQ":"BR"));
        profile.put("sector", emp.getSubSectorName());
        profile.put("liability_date", 
                CommonUtils.set_DT_ChangeDateFormatString(emp.getField("c_levy_liab_pymnt_dt"), 
                        "dd-MM-yyyy")
        );
        profile.put("cessation_date", 
                CommonUtils.set_DT_ChangeDateFormatString(cessation_date, 
                        "dd-MM-yyyy")
        );
        profile.put("remarks",remarks);
        profile.put("partner_my_co_id", "");
        profile.put("effective_date", 
                CommonUtils.set_DT_ChangeDateFormatString(
                emp.getField("dateCreated"), 
                        "dd-MM-yyyy")
        );
        profile.put("transaction_type", transaction_type);
        profile.put("int_app_id", "EMPM_"+
                AppUtil.processHashVariable("#date.yyyyMMdd#", null, null, null)+
                "_"+
                CommonUtils.getAPSITGCode(db)
                );
        
        profile.put("bank_modified_on_date", "");
        profile.put("bank_name", "");
        profile.put("sst_registered", "NO");
        profile.put("sst_reg_number", "");        
        
        profile_service.put("profile", profile);
        
        return new JSONObject().put("profile_service", profile_service);
    }

    private String getRefno(DBHandler db, String id) {
        String sql = "SELECT c_ref_no FROM app_fd_empm_regAppl WHERE id = ?";
        HashMap hm = db.selectOneRecord(sql, new String[]{id});
        
        String refno = "";
        if(hm!=null){
            refno = hm.get("c_ref_no").toString();
        }
        
        return refno;
    }
    
    private String getCessationDate(String deregId){
        String query = "select c_cease_ops_dt from app_fd_empm_dereg where id=?";
        DBHandler db = new DBHandler();
        String cessDate = ""; 
        try{
            db.openConnection((DataSource)AppUtil.getApplicationContext().getBean("setupDataSource"));
            HashMap<String, String> hm = db.selectOneRecord(query, new String[]{deregId});
            cessDate= hm.get("c_cease_ops_dt").toString();
            
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }
        
        return cessDate;
    
    }

    private JSONObject buildB2CJSONbody(DBHandler db, String empId, String id) throws JSONException {
        
        EmpmObj empObj = new EmpmObj(db, EmpmObj.BY_ID, empId);
        
        String mycoid = empObj.getMycoid();
        String hrdcno = empObj.getHrdcNo();
        String email = empObj.getPrimaryEmail();
        String tempLogin = empObj.getLoginEmail();
        String compName = empObj.getCompName();
        String telNo = empObj.getTelNo();
        
        String reqPw = new CurrentUser(tempLogin).getTempPwMetadata();
        
        JSONObject userPayload = new JSONObject();
        userPayload.put("accountEnabled", true);                        //true
        userPayload.put("displayName", mycoid);                         //mycoid
        userPayload.put("givenName", compName);                         //Company name
        userPayload.put("surName", hrdcno);                             //psmb no
        userPayload.put("mailNickName",
                email.contains("@")?email.split("@")[0]:email);          //email without the @domain part        
        if(!telNo.isEmpty()){
            userPayload.put("mobilePhone", telNo);                      //telno
        }
        userPayload.put("userPrincipalName", mycoid+"@"+B2C_DOMAIN_TEST);        //mycoid + '@hrdcorpb2cdev.onmicrosoft.com' (registerd b2c issuer donmain)
        
        JSONArray mailArray = new JSONArray();
        mailArray.put(email);
        userPayload.put("otherMails", mailArray);                       //mails.  object
        
        JSONObject pw = new JSONObject();
        pw.put("forceChangePasswordNextSignIn", false);
        pw.put("password", reqPw);                                      // the P
        
        userPayload.put("passwordProfile", pw);                         //passwordProfile obj
        
        JSONArray identities = new JSONArray();
        JSONObject identity = new JSONObject();
        identity.put("signInType", "userName");                         //type : userName for username type,  emailAddress if login by email
        identity.put("issuer", B2C_DOMAIN_TEST);                        //issuer domain - hrdcorpb2cdev.onmicrosoft.com
        identity.put("issuerAssignedId", mycoid);                       //login id
        
        identities.put(identity);
        
        userPayload.put("identities", identities);                      //important user logins object
                
        return userPayload;
    }

    private void completeQueryAct(DBHandler db, String regId) {

        String query = "SELECT \n" +
                "e.id, " +
                "r.c_mycoid, \n" +
                "e.dateCreated AS AppDate,\n" +
                "e.dateModified AS queryDate,\n" +
                "r.c_rem_count,\n" +
                "shk.ActivityId,\n" +
                "a.ProcessId,  a.PdefName, \n" +
                "r.c_q_templ, \n" +
                "r.c_empl_email_pri \n" +
                "from app_fd_empm_regAppl e\n" +
                "INNER JOIN app_fd_empm_reg r ON r.id = e.c_empl_fk\n" +
                "INNER JOIN wf_process_link wpl on e.id = wpl.originProcessId\n" +
                "INNER JOIN SHKAssignmentsTable shk on shk.ActivityProcessId = wpl.processId\n" +
                "INNER JOIN  SHKActivities a ON a.Id = shk.ActivityId \n" +
                "WHERE (shk.ActivityId like '%emp_registration_activity_query_2' \n" +
                "     or shk.ActivityId like '%emp_registration_activity_query_1'\n" +
                "    ) " +
                "AND e.id = ? " ;

        HashMap hm = db.selectOneRecord(query, new String[]{regId});

        String pDefId = "", pId = "", actId = "", mycoid = "";

        if(hm!=null) {
            actId = (hm.get("ActivityId") == null) ? "" : (String) hm.get("ActivityId");
            pDefId = (hm.get("PdefName") == null) ? "" : hm.getOrDefault("PdefName","").toString().replace("#",":");
            pId = (hm.get("ProcessId") == null) ? "" : (String) hm.get("ProcessId");
            mycoid = (hm.get("c_mycoid") == null) ? "" : (String) hm.get("c_mycoid");
            LogUtil.info(this.getClassName(),
                    "Unresponded Query for employer " + mycoid);

            wm.activityVariable(wfa.getActivityId(), "query_respond", "false");
            wm.activityVariable(wfa.getActivityId(), "pass_query_limit", "true");

            String encoding = Base64.getEncoder().encodeToString((Constants.API.JOGETAPI.MASTERAPILOGIN + ":" + Constants.API.JOGETAPI.MASTERAPIPW).getBytes());
            String params = "processDefId="+pDefId+"&processId="+pId+"&activityId="+actId;
            String endpoint = CommonUtils.getBaseURL()+"/jw/web/json/monitoring/running/activity/complete?"+params;
            HashMap headers = new HashMap();

            HttpUtil httpUtil = new HttpUtil();

//        LogUtil.info("Query Reminder ", "endpoint "+endpoint+", "+encoding);
            try {

                headers.put(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
                httpUtil.setHeader(headers);
                httpUtil.sendPostRequest(endpoint);

                JSONObject obj = httpUtil.getJSONResponse();

                LogUtil.info("Query Reminder","Completing current activity: "+actId+" content: "+
                        obj.toString() );

            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (NoSuchAlgorithmException ex) {
                ex.printStackTrace();
            } catch (KeyManagementException ex) {
                ex.printStackTrace();
            } catch (KeyStoreException ex) {
                ex.printStackTrace();
            } catch (JSONException ex) {
                ex.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
