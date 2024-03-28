/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.dao.OtherContactsUser;
import com.tms.hrdc.datalistAction.DeleteUserAndRecord;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.MasterSetupData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.PluginThread;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.User;
import org.joget.directory.model.service.DirectoryManager;
import org.joget.plugin.base.DefaultApplicationPlugin;

/**
 *
 * @author faizr
 */
public class SchedulerArchiveDataImport extends DefaultApplicationPlugin {
    
    @Override
    public String getName() {
        return this.getClassName();
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "Scheduler for Legacy Data Imports";
    }
    
    @Override
    public String getLabel() {
        return "HRDC - EMPM - Scheduler Archive Data Imports";
    }
    
    @Override
    public String getClassName() {
        return this.getClass().toString();
    }
    
    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/email_template_toolx.json", null, true, null);
    }
    
    private void msg(String msg){
        LogUtil.info(this.getClassName(), msg);
    }
    
    
    private int LIMIT_QUERY = 0;
    int MIGRATED_EMP_COUNT = 0;
    private String EMAIL = "faiz.rashidi@tmsasia.com";
    
    ArrayList ACTIVE_ACCOUNT = new ArrayList();
    
    private int LIMIT_ACCOUNT = 0;
    
    private void setAccounts(){
        ACTIVE_ACCOUNT.add("148712D");
        ACTIVE_ACCOUNT.add("130212D");
        ACTIVE_ACCOUNT.add("102650K");
        ACTIVE_ACCOUNT.add("00000076659T");
        ACTIVE_ACCOUNT.add("DBKK490504");        
        ACTIVE_ACCOUNT.add("13857V");
        ACTIVE_ACCOUNT.add("13857V_2");
//        ACTIVE_ACCOUNT.add("1026793K");
//        ACTIVE_ACCOUNT.add("001705W");
        LIMIT_ACCOUNT = ACTIVE_ACCOUNT.size();
    }
    
    private String START_TIME = "";
    private String END_TIME = "";
    private String ERROR = "";
    
    @Override
    public Object execute(Map props) {     
                
        setAccounts();
        Thread checkingThread = new PluginThread(new Runnable() {
            @Override
            public void run() {
                DBHandler db = new DBHandler();   
                
                try{
                    db.openConnection();
                    
                    START_TIME = CommonUtils.get_DT_CurrentDateTime("dd-MM-YYYY HH:mm:ss");
                    
                    msg("QUERYING ARCHIVE EMPLOYER_REGISTRATION... ");
                    ArrayList<HashMap<String, String>> arcEmpRegList = getArchiveEmployerData(db);
                    msg("QUERYING ARCHIVE EMPLOYER_REGISTRATION COMPLETE ... TOTAL SIZE "+Integer.toString(arcEmpRegList.size()));
                    
                    migrateIntoNCS(db, arcEmpRegList);
                    
                }catch(Exception e){
                    e.printStackTrace();
                }finally{
                    db.closeConnection();
                }
            }            
        });
        
        checkingThread.setDaemon(true);
        checkingThread.start();
        
        return null;
    }
    
    private ArrayList<HashMap<String, String>> getArchiveEmployerData(DBHandler db) {
        
        ArrayList<HashMap<String, String>> list = new ArrayList();
        
//        try{
//           db.openConnection();

String special = "'" 
                + ACTIVE_ACCOUNT.toString()
                    .replace("[", "")
                    .replace("]", "")
                    .replace(",", "','")
                +"'";
//        msg("special "+special.replace(" ", "")) ;  
            list = db.select(
                   "SELECT \n" +
                    "r.EMPLOYER_DTL_ID, r.EMPLOYER_ID,\n" +
                    "r.OLD_EMPLOYER_NO, r.MYCO_ID, \n" +
                    "r.PARENT_EMPLOYER_ID,\n" +
                    "r.COMPANY_NAME,\n" +
                    "r.CREATED_BY, r.CREATOR_NAME, r.CREATED_DATE, \n" +
                    "r.UPDATED_BY, r.UPDATER_NAME, r.UPDATED_DATE,\n" +
                    "r.COMPANY_CONTACT_EMAIL,r.USER_PASSWORD,\n" +
                    "r.APPLICATION_TYPE, \n" +
                    "r.ORGANIZATION_TYPE, r.ORG_TYPE_OTHERS,\n" +
                    "r.ORGANIZATION_LEVEL, \n" +
                    "r.OWNERSHIP_TYPE, r.OWNERSHIP_OTHERS,\n" +
                    "r.START_DATE, r.REGISTERED_YEAR, r.ATTAINING_DATE, r.EMPR_SUBMISSION_DT, r.EMPR_REGISTRATION_DT, r.LEVY_LIABILITY_DATE,\n" +
                    "r.WAGES_PREV_MONTH, \n" +
                    "r.IND_SECTOR_ID, r.IND_SECTOR_CODE, r.IND_SECTOR_DESC,\n" +
                    "r.IND_SUB_SECTOR_MST_ID, r.IND_SUB_SECTOR_CODE, r.IND_SUB_SECTOR_DESC,\n" +
                    "r.INDUSTRY_TYPE_NAME, r.INDUSTRY_OTHERS,\n" +
                    "r.EPF_NO, r.SOCSO_NO,\n" +
                    "r.TOTAL_EMP, r.TOTAL_EMPLOYEE, r.TOTAL_EMPLOYEE_PREV_MONTH,\n" +
                    "r.NO_MALAYSIAN_EMP, r.NO_NON_MALAYSIAN_EMP,\n" +
                    "r.SUBMISSION_OFFICER_PERSON_ID, r.SUBMISSION_OFFICER_NAME, r.SUBMISSION_OFFICER_IC_NO, r.SUBMISSION_OFFICER_DESIGNATION,\n" +
                    "r.IS_EMPR_ELIGIBLE, \n" +
                    "r.BUSINESS_TYPE_ID, r.BUSINESS_TYPE, r.BUSINESS_OWNER,\n" +
                    "r.CODE_CLASFCTN, \n" +
                    "r.UNDER_LEGAL, r.SOURCE_INFO,\n" +
                    "r.COMPANY_ADDRESS_PINCODE, \n" +
                    "r.COMPANY_ADDRESS_STATE_ID, r.COMPANY_ADDRESS_STATE_NAME,\n" +
                    "r.COMPANY_ADDRESS_COUNTRY_ID, r.COMPANY_ADDRESS_COUNTRY_NAME,\n" +
                    "r.COMPANY_ADDRESS_CITY_ID, r.COMPANY_ADDRESS_CITY_NAME,\n" +
                    "r.COMPANY_ADDRESS_ADDRESS_LINE_1, r.COMPANY_ADDRESS_ADDRESS_LINE_2, r.COMPANY_ADDRESS_ADDRESS_LINE_3, r.COMPANY_ADDRESS_ADDRESS_LINE_4,\n" +
                    "r.company_contact_ID,\n" +
                    "r.COMPANY_CONTACT_MOBILE, \n" +
                    "r.PAID_UP_CAPITAL,\n" +
                    "r.COMPANY_CONTACT_PERSON_ID,\n" +
                    "r.COMPANY_CONTACT_PERSON_NAME, r.COMPANY_CONTACT_PERSON_DESIGNATION, r.COMPANY_CONTACT_OFFICE_PHONE, r.COMPANY_CONTACT_EMAIL, r.COMPANY_CONTACT_CREATED_BY,\n" +
                    "r.COMPANY_CONTACT_CREATOR_NAME, r.COMPANY_CONTACT_CREATED_DATE, r.COMPANY_CONTACT_UPDATED_BY, r.COMPANY_CONTACT_UPDATED_DATE,\n" +
                    "r.STATUS, r.EMPLOYER_STATUS, s.SME_FLAG \n" +
                    "FROM archive.employer_registration r \n" +
                    "left join archive.sme_details s ON s.EMPLOYER_ID = r.EMPLOYER_ID \n" +
                    "WHERE not exists (\n" +
                    "   select ncs_r.id from app_fd_empm_reg ncs_r \n" +
                    "   WHERE ncs_r.c_mycoid = r.MYCO_ID COLLATE utf8mb4_unicode_ci " +
                    ") and r.STATUS IS NOT NULL and r.STATUS != '' " +
//                           "AND r.MYCO_ID IN ("+ special.replace(" ", "") +") " +
                    (LIMIT_QUERY==0?"":" limit "+Integer.toString(LIMIT_QUERY))
           );
           
//        }catch(Exception e){
//           e.printStackTrace();
//           msg("ERROR in getting archive ER");
//        }finally{
//           db.closeConnection();
//        }
        
        return list;
    }
    
    ArrayList<HashMap<String, String>> FORM1_COLUMNS = new ArrayList();
    
    private void migrateIntoNCS(DBHandler db, ArrayList<HashMap<String, String>> list) {
        
        int ACCOUNTS_CREATED = 0;
        ArrayList ACCOUNTS_USERNAME = new ArrayList();
            
//        try{
            
//            if(db.getConnection() == null){
//                db.openConnection();
//            }
//            db.openConnection();
            
            String sql =
                    "SELECT COLUMN_NAME, '' as COLUMN_VALUE\n" +
                            "FROM INFORMATION_SCHEMA.COLUMNS\n" +
                            "WHERE TABLE_NAME = ? "
                            + "AND TABLE_SCHEMA = DATABASE()";
            FORM1_COLUMNS =  db.select(sql, new String[]{Constants.TABLE.SUBMIT_TABLE_FORM1});

            String ocuName = "";
            String ocuDesg = "";
            String ocuTelNo = "";
            String ocuEmail = "";

            String empId = "";
            String mycoid = "";
            String status = "";
            String orgType = "";
            String compName = "";
            String subSectorCode = "";
            String hrdcNo = "";
            String companyEmail = "";
            String orgLevel = "";

            String ncsEmpStatus = "";
            String ncsDataStatus = "";
            String ncsLastMove = "";
            String oriMycoid = "";

            for(HashMap hm: list){
                HashMap newEmpHm = new HashMap();

                empId = hm.getOrDefault("EMPLOYER_ID", "").toString();
                mycoid = hm.getOrDefault("MYCO_ID", "").toString().trim();
                oriMycoid = mycoid;
                orgType = hm.getOrDefault("ORGANIZATION_TYPE", "").toString();
                compName = hm.getOrDefault("COMPANY_NAME", "").toString();
                hrdcNo = "PSMB-A-"+empId;
                companyEmail = hm.getOrDefault("COMPANY_CONTACT_EMAIL", "").toString();
                status = hm.getOrDefault("STATUS", "").toString();
                orgLevel = hm.getOrDefault("ORGANIZATION_LEVEL", "").toString().equals("BRANCH")?"Branch":"HQ";

                String[] mycoidArr = mycoid.split("_");

                if(mycoid.contains("_") && orgLevel.equalsIgnoreCase("Branch")
                        && mycoidArr.length>1){
                    // handle branch
//                    mycoid = mycoid.replace("_", "B00"+ (mycoidArr.length>1?mycoidArr[1]:"") );
                    try{
                        int bNo = Integer.parseInt(mycoidArr[1]);
                        mycoidArr[1] = String.format("%03d", bNo);
                    }catch(Exception e){
                        
                    }
                    mycoid = mycoidArr[0]+"B"+ mycoidArr[1];
                }

                if(status.equalsIgnoreCase("Active")){                
                    ncsEmpStatus = "ACTIVE";
                    ncsDataStatus = "REGISTER_APPROVED";
                    ncsLastMove = "FORM 1 APPROVED";                
                }else if(status.equalsIgnoreCase("DeActive")){
                    ncsEmpStatus = "INACTIVE";
                    ncsDataStatus = "DEREGISTER_APPROVED";
                    ncsLastMove = "FORM 4 APPROVED";   
                }else{
                    continue;
                }
                
                if(orgType.equals("ORG_OTHERS")){
                    orgType = "Others";
                }

                //joget
                newEmpHm.put("createdBy", hm.getOrDefault("CREATED_BY", "").toString());
                newEmpHm.put("createdByName", hm.getOrDefault("CREATOR_NAME", "").toString());
                newEmpHm.put("dateCreated", hm.getOrDefault("CREATED_DATE", "").toString());
                newEmpHm.put("modifiedBy", hm.getOrDefault("UPDATED_BY", "").toString());
                newEmpHm.put("modifiedByName", hm.getOrDefault("UPDATER_NAME", "").toString());
                newEmpHm.put("dateModified", hm.getOrDefault("UPDATED_DATE", "").toString());

                newEmpHm.put("hrdc_no", hrdcNo);
                newEmpHm.put("reg_type", "Employer Registration");
                newEmpHm.put("hq_id", hm.getOrDefault("PARENT_EMPLOYER_ID", "").toString());
                newEmpHm.put("emp_status", ncsEmpStatus);
                newEmpHm.put("data_status", ncsDataStatus);
                newEmpHm.put("last_move", ncsLastMove);

                //temp login
                newEmpHm.put("req_email", hm.getOrDefault("COMPANY_CONTACT_EMAIL", "").toString());
                newEmpHm.put("req_pw", hm.getOrDefault("USER_PASSWORD", "").toString());
                newEmpHm.put("req_pw_re", hm.getOrDefault("USER_PASSWORD", "").toString());

                //page 1                        
                newEmpHm.put("empl_org_type", orgType);
                newEmpHm.put("other_org_type", hm.getOrDefault("ORG_TYPE_OTHERS", "").toString());
                newEmpHm.put("empl_reg_type", orgLevel);
                newEmpHm.put("mycoid", mycoid);
                newEmpHm.put("mycoid_old", hm.getOrDefault("OLD_EMPLOYER_NO", "").toString());
                newEmpHm.put("comp_name", compName);
                newEmpHm.put("empl_email_pri", hm.getOrDefault("COMPANY_CONTACT_EMAIL", "").toString());     
                newEmpHm.put("empl_tel_no_pri", hm.getOrDefault("COMPANY_CONTACT_MOBILE", "").toString());                 

                String address1 = hm.getOrDefault("COMPANY_ADDRESS_ADDRESS_LINE_1", "").toString();
                String address2 = hm.getOrDefault("COMPANY_ADDRESS_ADDRESS_LINE_2", "").toString();
                String address3 = hm.getOrDefault("COMPANY_ADDRESS_ADDRESS_LINE_3", "").toString();
                String address4 = hm.getOrDefault("COMPANY_ADDRESS_ADDRESS_LINE_4", "").toString();

                address3 = address3+(address4.isEmpty()?"":", "+address4);

                String country = hm.getOrDefault("COMPANY_ADDRESS_COUNTRY_NAME", "").toString();
                String postcode = hm.getOrDefault("COMPANY_ADDRESS_PINCODE", "").toString();
                String state = hm.getOrDefault("COMPANY_ADDRESS_STATE_NAME", "").toString();
                String state_id = hm.getOrDefault("COMPANY_ADDRESS_STATE_ID", "").toString();
                String city = hm.getOrDefault("COMPANY_ADDRESS_CITY_NAME", "").toString();

                newEmpHm.put("bu_address1", address1);     
                newEmpHm.put("bu_address2", address2);     
                newEmpHm.put("bu_address3", address3);     

                newEmpHm.put("empl_address", address1);     
                newEmpHm.put("empl_address2", address2);     
                newEmpHm.put("empl_address3", address3);     

                newEmpHm = setLocation(db, newEmpHm, country, postcode, state, state_id, city);

                newEmpHm.put("empl_email_pic_pri", companyEmail);                 

                saveOtherContactDetails(empId, hm.getOrDefault("COMPANY_CONTACT_EMAIL", "").toString(), 
                        hm.getOrDefault("COMPANY_CONTACT_PERSON_NAME", "").toString(),
                        hm.getOrDefault("COMPANY_CONTACT_PERSON_DESIGNATION", "").toString(),
                        hm.getOrDefault("COMPANY_CONTACT_OFFICE_PHONE", "").toString());

                //page 2
                newEmpHm.put("dt_commence", hm.getOrDefault("REGISTERED_YEAR", "").toString()+"-04-01 00:00:00");            
                newEmpHm.put("ownership",getOwnershipId(db, hm.getOrDefault("OWNERSHIP_TYPE", "").toString()));
                newEmpHm.put("epf_no", hm.getOrDefault("EPF_NO", "").toString());
                newEmpHm.put("socso_no", hm.getOrDefault("SOCSO_NO", "").toString());
                newEmpHm.put("attain_date", hm.getOrDefault("ATTAINING_DATE", "").toString());

                subSectorCode = hm.getOrDefault("IND_SUB_SECTOR_CODE", "000000").toString();

                newEmpHm = setSectorData(db, newEmpHm, subSectorCode);
                
                String total_emp_prev_str = hm.getOrDefault("TOTAL_EMPLOYEE_PREV_MONTH", "0").toString();
                String total_emp_str = hm.getOrDefault("TOTAL_EMPLOYEE", "0").toString();
                String total_my_emp_str = hm.getOrDefault("NO_MALAYSIAN_EMP", "0").toString();
                String total_non_my_emp_str = hm.getOrDefault("NO_NON_MALAYSIAN_EMP", "0").toString();
                
                int total_emp_int = 0;
                int total_my_emp_int = 0;
                int total_non_my_emp_int = 0;

                try{
                    total_emp_int = Integer.parseInt(total_emp_str);
                    total_my_emp_int = Integer.parseInt(total_my_emp_str.isEmpty()? "0":total_my_emp_str);
                    total_non_my_emp_int = Integer.parseInt(total_non_my_emp_str);

                    if(total_my_emp_int == 0){
                        total_my_emp_int = total_emp_int - total_non_my_emp_int;
                    }

                    total_my_emp_str = Integer.toString(total_my_emp_int);
                }catch(Exception e){

                }

                newEmpHm.put("total_wages_my_empl_paid", hm.getOrDefault("WAGES_PREV_MONTH", "0").toString());
                newEmpHm.put("total_wages_my_empl_paid_prev", hm.getOrDefault("WAGES_PREV_MONTH", "0").toString());
                newEmpHm.put("total_non_my_empl", total_non_my_emp_str);
                newEmpHm.put("total_non_my_empl_prev", "0");
                newEmpHm.put("total_my_empl", total_my_emp_str);
                newEmpHm.put("total_my_empl_prev", total_emp_prev_str);                    // no data
                newEmpHm.put("total_empl", total_emp_str);
                newEmpHm.put("total_empl_prev", total_emp_prev_str);            

                // page 3
                newEmpHm.put("so_name", hm.getOrDefault("COMPANY_CONTACT_PERSON_NAME", "").toString());
                newEmpHm.put("so_desg", hm.getOrDefault("COMPANY_CONTACT_PERSON_DESIGNATION", "").toString());
                newEmpHm.put("so_tel_no", hm.getOrDefault("COMPANY_CONTACT_OFFICE_PHONE", "").toString());
                newEmpHm.put("so_ic_passport_no", hm.getOrDefault("COMPANY_CONTACT_PERSON_IC_NO", "").toString());
                newEmpHm.put("decl1", "agree");
                newEmpHm.put("decl2", "agree");

                String sme = hm.getOrDefault("SME_FLAG", "").toString();

                if(sme.equals("Y")){
                    sme = "Yes";
                }
                
                String src_info = getSourceInfoId(db, hm.getOrDefault("SOURCE_INFO", "").toString());
                String bu_own = getBUOwnerId(db, hm.getOrDefault("BUSINESS_OWNER", "").toString());
                String bu_type = getBUTypeId(db, hm.getOrDefault("BUSINESS_TYPE", "").toString());

                newEmpHm.put("register_dt", hm.getOrDefault("EMPR_REGISTRATION_DT", "").toString());
                newEmpHm.put("levy_liab_pymnt_dt", hm.getOrDefault("LEVY_LIABILITY_DATE", "").toString());
                newEmpHm.put("business_type", bu_type);
                newEmpHm.put("isSME", sme);
                newEmpHm.put("business_owner", bu_own);
                newEmpHm.put("source", src_info);

                empId = CommonUtils.saveUpdateForm2("",Constants.FORM_ID.EMP_MAIN_FORM, empId, newEmpHm);

                HashMap data = db.selectOneRecord("SELECT * FROM app_fd_empm_reg WHERE id = ?", new String[]{empId});
                //save form1
                saveCopyForm1(data, empId);

                //save regAppl HashMap
                HashMap regHm = new HashMap();
                regHm.put("createdBy", hm.getOrDefault("CREATED_BY", "").toString());
                regHm.put("createdByName", hm.getOrDefault("CREATOR_NAME", "").toString());
                regHm.put("dateCreated", hm.getOrDefault("CREATED_DATE", "").toString());
                regHm.put("modifiedBy", hm.getOrDefault("UPDATED_BY", "").toString());
                regHm.put("modifiedByName", hm.getOrDefault("UPDATER_NAME", "").toString());
                regHm.put("dateModified", hm.getOrDefault("UPDATED_DATE", "").toString());
                regHm.put("submit_mode", "ONLINE");
                regHm.put("form_type", "1");
                regHm.put("empl_fk", empId);
                regHm.put("approve_dt", hm.getOrDefault("EMPR_REGISTRATION_DT", "").toString());
                regHm.put("flow_status", "APPROVED");
                regHm.put("reg_type", "Employer Registration");

                CommonUtils.saveUpdateForm2("",Constants.FORM_ID.EMP_REGAPPL_FORM, "", regHm);

                //createAccount
                if(ACTIVE_ACCOUNT.contains(oriMycoid)){

                    if(ACCOUNTS_CREATED<=LIMIT_ACCOUNT && ncsEmpStatus.equals("ACTIVE")){
                        createJogetUser(db, hrdcNo, mycoid, compName, companyEmail, mycoid);
                        HashMap userHm = new HashMap();
                        userHm.put("userId", mycoid);
                        userHm.put("compId", empId);

                        CommonUtils.saveUpdateForm("","bridge_user", "", userHm);    
                        
//                        if(ncsEmpStatus.equals("INACTIVE")){
//                            DeleteUserAndRecord.disableUser(db, mycoid);
//                            mycoid=mycoid+"(DISABLED)";
//                        }

                        ACCOUNTS_CREATED++;
                        ACCOUNTS_USERNAME.add(mycoid);
                    }
                }

                MIGRATED_EMP_COUNT++;
                
                //new audit trail
            }
        
        
        END_TIME = CommonUtils.get_DT_CurrentDateTime("dd-MM-YYYY HH:mm:ss");
        
        String content = 
                "<br /> Start Time: "+START_TIME+" <br /> "
                +"End Time: "+END_TIME+" <br /> "
                +"Number Migrated: "+Integer.toString(MIGRATED_EMP_COUNT)+" <br /> "
                + "Accounts Created: "+Integer.toString(ACCOUNTS_CREATED)+" <br /> "
                + "Usernames List: "+ACCOUNTS_USERNAME.toString()+" <br /> "
                + "Errors: "+ERROR+" <br /><br /> ";
        CommonUtils.sendEmail(EMAIL, "", "MIGRATION RESULT", content, null, null);
    }
    
    public String createJogetUser(DBHandler db, String psmbNo,String userId_, String empName, String email, String pw) {
        
        UserDao ud = (UserDao) AppUtil.getApplicationContext().getBean("userDao");        
        
        String group = "", username = "", userId;
        
        userId = userId_; 
        username = userId_;
        
        boolean create = true;
        boolean changePw = false;
        
        changePw = true;
        group = 
                Constants.USER_GROUP.EMPLOYERS + ";" +
                Constants.USER_GROUP.VIEW_PERMIT_EMPM + ";" +
                Constants.USER_GROUP.VIEW_PERMIT_TP + ";" +
                Constants.USER_GROUP.VIEW_PERMIT_CLAIM + ";" +
//                    Constants.USER_GROUP.VIEW_PERMIT_EVENT + ";" +
                Constants.USER_GROUP.VIEW_PERMIT_GRANT + ";" +
                Constants.USER_GROUP.VIEW_PERMIT_LEVY ;
        
        if(create){
            CommonUtils.createJogetUser(userId, username, empName, "", email, pw, group);  
            
            if(!changePw){
                DirectoryManager dm = (DirectoryManager) AppUtil.getApplicationContext().getBean("directoryManager");
                User user = dm.getUserById(userId);
            }else{                               
                db.update(
                "UPDATE dir_user_extra SET requiredPasswordChange = 1 WHERE username = '"+userId+"'"
                );
            }
        }        
        return userId;            
    }
    
    private String getOwnershipId(DBHandler db, String label){
        switch(label){
            case "PRIVATE_LTD_COMPANY":
                label = "Private Limited Company";
            break;
            case "PUBLIC_LTD_COMPANY":
                label = "Public Limited Company";
            break;
            case "SOLE_PROPRIETORSHIP":
                label = "Sole Proprietorship";
            break;
            case "PARTNERSHIP":
                label = "Partnership";
            break;
            case "OWNERSHIP_OTHERS":
                label = "Others";
            break;
        }
        
        String id = db.selectOneValueFromTable("SELECT id FROM app_fd_stp_ownership_type WHERE c_ownership_type = ? LIMIT 1", new String[]{label});
        
        return id;
    }
    
    private HashMap setSectorData(DBHandler db, HashMap newEmpHm, String subSectorCode) {
        
//        String code = subSectorCode.replace(" ","").trim();
        
        String code = subSectorCode.replaceAll("[^a-zA-Z0-9]", "");  
        int code_int = 0;
        
        if(code.length()>1 ){            
            try{
                code_int = Integer.parseInt(code);
                if(code.length()<5){
                    code = String.format("%05d", code_int);
                }  
            }catch(Exception e){
                e.printStackTrace();
                return newEmpHm; 
            }            
        }else{
            return newEmpHm;
        }
        
        String id = "";
        String label = "";
//        msg("Sub Sector Code "+code+", size "+code.length());
        //sector sub
        HashMap hm = MasterSetupData.getSubSectorData(db, code);
        
        if(hm!=null){
            id = hm.get("id").toString();
            label = hm.get("label").toString();
        }else{
            msg("Industry Sector - Sector Code Not Found for Code "+code);
        }
        
        newEmpHm.put("sector_search_id", id);
        newEmpHm.put("sector_code", id);
        newEmpHm.put("sector_descr", label);
        
        // sector class        
        code = code.substring(0, 4);
//        msg("Class Code "+code+", size "+code.length());
        id = "";
        label = "";
        
        hm = MasterSetupData.getClassSectorData(db, code);
        
        if(hm!=null){
            id = hm.get("id").toString();
            label = hm.get("label").toString();
        }else{
            msg("Industry Sector - Class Code Not Found for Code "+code);
        }
        
        newEmpHm.put("class_code", id);
        newEmpHm.put("class_label", label);
        
        //sector main
        code = code.substring(0, 3);
//        msg("Main Code "+code+", size "+code.length());
        id = "";
        label = "";
        
        hm = MasterSetupData.getMainSectorData(db, code);
        
        if(hm!=null){
            id = hm.get("id").toString();
            label = hm.get("label").toString();
        }else{
            msg("Industry Sector - Main Sector Code Not Found for Code "+code);
        }
        
        newEmpHm.put("main_sector_code", id);
        newEmpHm.put("main_sector_label", label);
        
        //sector div
        code = code.substring(0, 2);
//        msg("Div Code "+code+", size "+code.length());
        id = "";
        label = "";
        
        hm = MasterSetupData.getDivSectorData(db, code);
        
        if(hm!=null){
            id = hm.get("id").toString();
            label = hm.get("label").toString();
        }else{
            msg("Industry Sector - Div Code Not Found for Code "+code);
        }
        
        newEmpHm.put("div", id);
        newEmpHm.put("div_label", label);
        
        //sector industry
        code = MasterSetupData.getIndustrySectorCodeFromDiv(db, code);       
//        msg("Sector Code "+code+", size "+code.length());
        hm = MasterSetupData.getIndustrySectorData(db, code);
        
        id = "";
        label = "";
        
        if(hm!=null){
            id = hm.get("id").toString();
            label = hm.get("label").toString();
        }else{
            msg("Industry Sector - Section Not Found for Code "+code);
        }
        
        newEmpHm.put("industry_sector", id);
        newEmpHm.put("industry_sector_label", label);
        
        return newEmpHm;
    }
        
    private void saveOtherContactDetails(String empId,String email, String name, String desig, String telno) {
        HashMap hm = new HashMap();
        
        hm.put("name", name);
        hm.put("tel_no", telno);
        hm.put("designation", desig);
        hm.put("email", email);
        hm.put("fk", empId);
        
        CommonUtils.saveUpdateForm2("", 
                Constants.FORM_ID.EMP_REG_SUBFORM_OTHERCONTACTS, "", hm);
    }

    private HashMap setLocation(DBHandler db, HashMap newEmpHm, String country, String postcode, String state, String state_id, String city) {
        
        String city_id = "";
//        String state_id = "";
        String country_id = "";
        
        String countryStp = "";
        String stateStp = "";
        String cityStp = "";
        
        if(country.equalsIgnoreCase("MALAYSIA")){
            newEmpHm.put("business_location", "In Malaysia");     
        }else{
            newEmpHm.put("business_location", "Outside of Malaysia");     
        }
        
        countryStp = MasterSetupData.getCountryByPostcode(db, postcode);
        stateStp = MasterSetupData.getStateByPostcode(db, postcode);
        cityStp = MasterSetupData.getCityByPostcode(db, postcode);
        
        
        //cityStp is empty = no data for postcode
        //cityStp is not empty && cityId no data
        String properCity = MasterSetupData.getCityId(db, city);
        if(cityStp.isEmpty() || properCity.isEmpty()){
            cityStp = insertNewLocation(db, postcode, state_id, city);
        }    
        
        String stateCode = getStateCode(db, stateStp);
        
        newEmpHm.put("state_code", stateCode);
        
        newEmpHm.put("empl_country", countryStp);
        newEmpHm.put("empl_state", stateStp);
        newEmpHm.put("empl_city", cityStp);
        newEmpHm.put("empl_postcode", postcode);
        
        newEmpHm.put("bu_country", countryStp);
        newEmpHm.put("bu_state", stateStp);
        newEmpHm.put("bu_city", cityStp);
        newEmpHm.put("bu_postcode", postcode);
        
        return newEmpHm;
    }
    
    private String getStateCode(DBHandler db, String stateId){
        HashMap hm = db.selectOneRecord("SELECT id, c_state_code FROM app_fd_empm_enf_st_cde_stp WHERE c_state = ?", 
                new String[]{stateId});
        
        if(hm!=null){
            return hm.getOrDefault("id", "").toString();
        }
        
        return "";
    }
    
    private String insertNewLocation(DBHandler db, String postcode, String state_id, String city) {
        HashMap hm = db.selectOneRecord(
                "select STATE_NAME, s.id, s.c_country \n" +
                "FROM archive.lookup_state l \n" +
                "INNER JOIN app_fd_stp_state s on \n" +
                "	(s.c_state = l.STATE_NAME COLLATE utf8mb4_unicode_ci )\n" +
                "       OR \n" +
                "	(CASE \n" +
                "	WHEN l.STATE_NAME = 'Labuan'\n" +
                "	THEN c_state = 'WP LABUAN'\n" +
                "	WHEN l.STATE_NAME = 'Kuala Lumpur'\n" +
                "	THEN c_state = 'WP KUALA LUMPUR'\n" +
                "	WHEN l.STATE_NAME = 'Putrajaya'\n" +
                "	THEN c_state = 'WP PUTRAJAYA'\n" +
                "	WHEN l.STATE_NAME = 'Penang'\n" +
                "	THEN c_state = 'PULAU PINANG'\n" +
                "	END) "+
                "where l.STATE_ID = ? ",
                new String[]{state_id}
        );
        
        String state_name = "";
        String state_id_ncs = "";
        String country_id_ncs = "";
        
        if(hm!=null){
            state_name = hm.getOrDefault("STATE_NAME", "").toString().trim().toUpperCase();
            state_id_ncs = hm.getOrDefault("id", "").toString().trim();
            country_id_ncs = hm.getOrDefault("c_country", "").toString().trim();
        }
        
        hm = new HashMap();        
        hm.put("state", state_id_ncs);
        hm.put("city", city);
        hm.put("status", "Active");
        return CommonUtils.saveUpdateForm2("master_setup","city", "", hm);        
    }

    private String saveCopyForm1(HashMap data, String empId) {

        ArrayList<HashMap<String, String>> colArr = FORM1_COLUMNS;
        
        String col = "c_ref_no";
//        String val = "'"+refno+"'";
        String id = "";
        String archiveForm = "";

        ArrayList<HashMap<String, String>> arcArr = new ArrayList();

        HashMap<String, String> arcData = new HashMap();

        for(HashMap colHm:colArr){
            String colName = colHm.get("COLUMN_NAME").toString();
            String value = "";

            if(colName.equals("id")){
//                id = UuidGenerator.getInstance().getUuid();
//                value = id;
                continue;
            }else if(colName.equals("c_fk")){
                value = (String) data.get("id");
            }else if(colName.equals("c_ref_no")){
                value =  "ARCHIVE-"+empId;
            }else if(colName.equals("c_form_type")){
                value =  "1";
            }else if(!data.containsKey(colName)){
                continue;
            }else{
                value = data.get(colName)==null?"":data.get(colName).toString();
            }

            colHm.put("COLUMN_VALUE", value);

            if(colName.startsWith("c_")){
                colName = colName.replaceFirst("c_", "");
            }

            arcData.put(colName, value);
        }

//        if(!arcData.keySet().stream()
//                .anyMatch(k -> k.equals("c_ref_no") )
//                && (table.equals(Constants.TABLE.SUBMIT_TABLE_FORM1A)
//                || table.equals(Constants.TABLE.SUBMIT_TABLE_FORM1)   )
//        )
//        {
            archiveForm = Constants.FORM_ID.ARCHIVE_FORM1;
            arcData.put("c_ref_no", "ARCHIVE-"+empId);
//        }

        id = CommonUtils.saveUpdateForm2("", Constants.FORM_ID.ARCHIVE_FORM1, "", arcData);

        return id;
    }

    private String getSourceInfoId(DBHandler db, String srcData) {
        HashMap data = db.selectOneRecord(
                "SELECT id, c_src_info FROM app_fd_empm_src_info_stp WHERE c_src_info = ?",
                new String[]{srcData.trim()}
        );
        
        if(data!=null){
            return data.getOrDefault("id", "").toString();
        }
        
        return "";
    }

    private String getBUOwnerId(DBHandler db, String buOwner) {
        HashMap data = db.selectOneRecord(
                "SELECT id, c_bu_owner FROM app_fd_empm_bu_owner_stp WHERE c_bu_owner = ?",
                new String[]{buOwner.trim()}
        );
        
        if(data!=null){
            return data.getOrDefault("id", "").toString();
        }
        
        return "";
    }

    private String getBUTypeId(DBHandler db, String buType) {
        HashMap data = db.selectOneRecord(
                "SELECT id, c_bu_type FROM app_fd_empm_bu_type_stp WHERE c_bu_type = ?",
                new String[]{buType.trim()}
        );
        
        if(data!=null){
            return data.getOrDefault("id", "").toString();
        }
        
        return "";
    }

    

   
    
}