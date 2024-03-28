/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.webservice.models;

import com.tms.hrdc.binder.ArchiveBinder;
import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.defaultPluginTool.FormManagerTool;
import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.HttpUtil;
import com.tms.hrdc.util.KeywordDictionary;
import com.tms.hrdc.util.MasterSetupData;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.User;
import org.joget.workflow.model.WorkflowProcess;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *
 * @author faizr
 */
public class EmpData {

    final String INACTIVE = "INACTIVE";
    final String ACTIVE = "ACTIVE";

    final String UUID = "uuid";
    final String COL = "column";
    final String VAL = "value";
    final String PREV_VALUE = "previous_value";
    final String CURR_VALUE = "current_value";
    final String FIELD_NAME = "fld_name";
    final String DATEMODIFIED = "date_modified";
    final String MODIFIEDBY = "modified_by_id";
    final String MODIFIEDBYNAME = "modified_by_name";
    final String VALUE_CHGE_LIST = "audit_trail_list";
    final String AUDIT_FK = "audit_fk";
    final String STATUS = "status";
    final String REMARK = "remark";

    HttpServletRequest request;
    DBHandler db;

    private void msg(String msg){
        LogUtil.info(this.getClass().getName(), msg);
    }
    
    class EmployerAPIException extends Exception
    {
        public EmployerAPIException(String message)
        {
            super(message);
        }
    }
    
    public EmpData(DBHandler d, HttpServletRequest req) throws SQLException {
        db = d;
        request = req;
    }

    

    public JSONObject getIfUserPermit() throws JSONException{
        String userId = request.getParameter("userId") == null?"":request.getParameter("userId");

        HashMap hm = db.selectOneRecord("SELECT c_compId FROM app_fd_empm_usermap WHERE c_userId = ?",
                new String[]{userId});

        boolean permit = false;
        String msg = "";

        JSONObject jo = new JSONObject();

        if(hm==null){
            msg = "User not a registered employer";
            jo.put("msg", msg);
            jo.put("permit", permit);

            return jo;
        }

        String empId = hm.get("c_compId").toString();

        hm = db.selectOneRecord(
                "SELECT c_data_status, c_emp_status FROM "+Constants.TABLE.EMPREG
                +" WHERE id = ? ",
                new String[]{empId}
        );

        if(hm==null){
            msg = "No data";
            jo.put("msg", msg);
            jo.put("permit", permit);

            return jo;
        }

        String currStatus = hm.get("c_data_status").toString();
        String empStatus = hm.get("c_emp_status").toString();
        String url_to_grant_view = CommonUtils.getBaseURL()+"/jw/web/userview/empm/shareable_view/_/grant_shared_view?embed=true&id="+empId;
        String url_to_profile_view = CommonUtils.getBaseURL()+"/jw/web/userview/empm/shareable_view/_/employer_profile?embed=true&id="+empId;

        jo.put("empl_prev_link", url_to_grant_view);
        jo.put("empl_profile", url_to_profile_view);

        if(empStatus.equals("ACTIVE") && currStatus.equals("DEREGISTERING")){
            msg = "Employer is deregistering";
            jo.put("msg", msg);
            jo.put("permit", permit);

            return jo;
        }else

        if(empStatus.equals("INACTIVE")){
            msg = "Employer is deregistered";
            jo.put("msg", msg);
            jo.put("permit", permit);

            return jo;
        } else {
            msg = "Registered Employer";
            jo.put("msg", msg);
            jo.put("permit", true);

            return jo;
        }
    }

    public JSONObject getEmplDataForReg() throws JSONException{
        String regno = request.getParameter("mycoid");
        String query = "SELECT * FROM app_fd_empm_reg WHERE "
                        + "c_mycoid = ? "
                        + " AND (c_data_status != 'DEREG_COMPLETED' "
                        + " OR c_emp_status != 'INACTIVE' )"
                        + " AND c_empl_reg_type = 'HQ' ";

        String[] cond = {regno};
        ArrayList<HashMap<String,String>> result = db.select(query, cond);

        JSONObject empl = new JSONObject();

        if(result.size()<1){
            empl.put("reg_no", "");
            empl.put("emp_status", "");
            empl.put("data_status", "");
            empl.put("permit", "YES");
        }else{
            //means hq already exist/underway 
            for(HashMap resultHm:result){
                empl.put("reg_no", resultHm.get("c_mycoid"));
                empl.put("data_status", resultHm.get("c_data_status"));
                empl.put("emp_status", resultHm.get("c_emp_status"));
                empl.put("permit", "NO");
            }
        }

        return empl;
    }

    public JSONObject checkExistingEmail() throws JSONException{

        String email = request.getParameter("email") == null?"":request.getParameter("email");
        JSONArray array = new JSONArray();
        String query = "select * from app_fd_empm_usermap m\n" +
                        "inner join dir_user d on d.id = m.c_userId\n" +
                        "where username = ?";
        HashMap hm = db.selectOneRecord(query, new String[]{email});
        if(hm!=null){
            array.put(new JSONObject().put("Exist", "true"));
        }else{
            array.put(new JSONObject().put("Exist", "false"));
        }

        return new JSONObject().put("dataArray", array);
    }

    JSONObject modifiedJson = new JSONObject();
    
    public JSONObject getEmplDeregData() throws JSONException, NumberFormatException, EmployerAPIException{
        String myCoID = request.getParameter("mycoid") == null?"":request.getParameter("mycoid");
        String psmb_no = request.getParameter("psmbno") == null?"":request.getParameter("psmbno");
        String usrId = request.getParameter("byCurrentId") == null?"":request.getParameter("byCurrentId");
        String size = request.getParameter("size") == null?"":request.getParameter("size");
        String page = request.getParameter("page") == null?"":request.getParameter("page");       
        
        String empId = "";
        
        if(request.getParameter("byCurrentId") != null //&& !psmb_no.isEmpty()
                ){
            empId = db.selectOneValueFromTable(
                    "SELECT c_compId FROM app_fd_empm_usermap WHERE c_userId = ?",
                    new String[]{usrId}
            );
        }

        ArrayList<HashMap<String,String>> result;
        
        String query = "SELECT r.c_emp_status, r.c_data_status, d.c_effective_dt "
                + "FROM app_fd_empm_reg r "
                + "INNER JOIN "+Constants.TABLE.DEREG+" d ON r.id = d.c_dreg_emp_id "
                + "WHERE r.c_data_status = 'INACTIVE' ";
        
        if(request.getParameter("mycoid") != null //&& !psmb_no.isEmpty()
                ){
            query+=!query.contains("WHERE")?" WHERE ":" AND ";
            query+= "( r.c_mycoid = '"+myCoID+"' OR  r.c_hq_mycoid = '"+myCoID+"' )";
        }
        if(request.getParameter("psmbno") != null //&& !myCoID.isEmpty()
                ){
            query+=!query.contains("WHERE")?" WHERE ":" AND ";
            query+= " r.c_hrdc_no = '"+psmb_no+"'";
        }
        if(!empId.isEmpty()
                ){
            query+=!query.contains("WHERE")?" WHERE ":" AND ";
            query+= " r.id = '"+empId+"'";
        }
        
        query+=" ORDER BY d.dateCreated desc ";        
        query = addPagination(query, size, page);
        msg("query "+query);
        result = db.select(query);
        JSONArray array = new JSONArray();
        
        if(result.size()>0){

            for(HashMap resultHm:result){
                JSONObject empl = CommonUtils.getArrangedJson();
                String empStatus = resultHm.get("c_emp_status").toString();
                String statusRemarks = resultHm.get("c_data_status").toString();
                String effectiveDt = resultHm.get("c_effective_dt").toString();               

                empl.put("emp_status", empStatus);
                empl.put("status_remarks", statusRemarks);
                empl.put("effective_dt", effectiveDt);

                array.put(empl);
            }
        }

        return new JSONObject().put("dataArray", array);
    }

    public JSONObject getEmplData() throws JSONException, NumberFormatException, EmployerAPIException{

        String empId = request.getParameter("empId") == null?"":request.getParameter("empId");
        String myCoID = request.getParameter("mycoid") == null?"":request.getParameter("mycoid");
        String psmb_no = request.getParameter("psmbno") == null?"":request.getParameter("psmbno");
        String reg_type = request.getParameter("reg_type") == null?"":request.getParameter("reg_type");
        String loginEmail = request.getParameter("loginEmail") == null?"":request.getParameter("loginEmail");
        String regId = request.getParameter("regId") == null?"":request.getParameter("regId");
        String byCurrentId = request.getParameter("byCurrentId") == null?"":request.getParameter("byCurrentId");
        String byStatus = request.getParameter("status") == null?"":request.getParameter("status");
        String isSME = request.getParameter("isSME") == null?"":request.getParameter("isSME");

        String searchComp = request.getParameter("sComp") == null?"":request.getParameter("sComp");
        String searchMycoid = request.getParameter("sMycoid") == null?"":request.getParameter("sMycoid");
        String currSearcher = request.getParameter("sCurr") == null?"":request.getParameter("sCurr");

        String size = request.getParameter("size") == null?"":request.getParameter("size");
        String page = request.getParameter("page") == null?"":request.getParameter("page");
        
        JSONObject retMsg = new JSONObject();
        
        String query = "SELECT r.* FROM app_fd_empm_reg r ";

        ArrayList<HashMap<String,String>> result;

        if(request.getParameter("regId") != null //&& !loginEmail.isEmpty()
        ){
            query+= " INNER JOIN app_fd_empm_regAppl a on r.id = a.c_empl_fk"
                    + " WHERE a.id = '"+regId+"'";
        }

        if(request.getParameter("empId") != null //&& !psmb_no.isEmpty()
        ){
            query+=!query.contains("WHERE")?" WHERE ":" AND ";
            query+= " r.id = '"+empId+"'";
        }

        if(request.getParameter("mycoid") != null //&& !psmb_no.isEmpty()
                ){
            query+=!query.contains("WHERE")?" WHERE ":" AND ";
            query+= "( r.c_mycoid = '"+myCoID+"' OR  r.c_hq_mycoid = '"+myCoID+"' )";
        }


        if(request.getParameter("psmbno") != null //&& !myCoID.isEmpty()
                ){
            query+=!query.contains("WHERE")?" WHERE ":" AND ";
            query+= " r.c_hrdc_no = '"+psmb_no+"'";
        }
        
        if(request.getParameter("status") != null //&& !myCoID.isEmpty()
                ){
            
            if(byStatus.equals("DEREGISTERED")){
                query+=!query.contains("WHERE")?" WHERE ":" AND ";
                query+= " r.c_data_status = 'DEREGISTER_APPROVED' ";
            }else{
                query+=!query.contains("WHERE")?" WHERE ":" AND ";
                query+= " r.c_emp_status = '"+byStatus+"' ";
            }
            
        }
        
        if(request.getParameter("isSME") != null //&& !myCoID.isEmpty()
                ){
            query+=!query.contains("WHERE")?" WHERE ":" AND ";
            query+= " r.c_isSME = 'Yes'";
        }

        if(request.getParameter("reg_type") != null //&& !reg_type.isEmpty()
                ){
            query+=!query.contains("WHERE")?" WHERE ":" AND ";
            query+= " r.c_empl_reg_type = '"+reg_type+"'";
        }

        if(request.getParameter("loginEmail") != null //&& !loginEmail.isEmpty()
                ){
            query+=!query.contains("WHERE")?" WHERE ":" AND ";
            query+= " r.c_empl_email_pri = '"+loginEmail+"'";
        }

        if(request.getParameter("sComp") != null ){
            query+=!query.contains("WHERE")?" WHERE ":" AND ";
//            query+= " (r.c_comp_name LIKE '%"+searchComp+"%' OR r.c_mycoid LIKE '%"+searchComp+"%') " +
            query+= " (r.c_comp_name = '"+searchComp+"' OR r.c_mycoid = '"+searchComp+"') " +
                    "AND c_emp_status = 'ACTIVE' ";
        }

        if(request.getParameter("sCurr") != null ){
            query+=!query.contains("WHERE")?" WHERE ":" AND ";
            query+= " r.c_mycoid != '"+currSearcher+"' ";
        }

        if(request.getParameter("byCurrentId") != null ){

            HashMap hm = db.selectOneRecord(
                    "SELECT * FROM app_fd_empm_usermap WHERE c_userId = ?",
                    new String[]{byCurrentId});

            if(hm!=null){
                byCurrentId = hm.get("c_compId").toString();
            }

            query+=!query.contains("WHERE")?" WHERE ":" AND ";
            query+= " r.id = '"+byCurrentId+"' ";
        }

        query+=!query.contains("WHERE")?" WHERE ":" AND ";
        query+=" (c_emp_status IS NOT NULL AND c_emp_status != '') ORDER BY dateCreated desc ";        
        query = addPagination(query, size, page);

        result = db.select(query);
        JSONArray array = new JSONArray();

        if(result.size()>0){

            for(HashMap resultHm:result){
                JSONObject empl = CommonUtils.getArrangedJson();
                String id = resultHm.get("id").toString();
                String empStatus = resultHm.get("c_emp_status").toString();
                String statusRemarks = resultHm.get("c_data_status").toString();

                String cease_ops_dt = "";
                String dereg_refno = "";
                String dereg_effective_dt = "";
                String dereg_status = "";

                if(empStatus.equals("INACTIVE") || statusRemarks.equals("DEREGISTERING")){
                    HashMap deregData = getDeregDt(id);
                    cease_ops_dt = deregData==null?"":deregData.get("c_cease_ops_dt").toString();
                    dereg_refno = deregData==null?"":deregData.get("c_dereg_refno").toString();
                    dereg_effective_dt = deregData==null?"":deregData.get("c_effective_dt").toString();
                    dereg_status = deregData==null?"":deregData.get("c_flow_status").toString();
                }

                String ind_code = resultHm.get("c_industry_sector") == null?"":resultHm.get("c_industry_sector").toString();
                String div_code = resultHm.get("c_div") == null?"":resultHm.get("c_div").toString();
                String main_code = resultHm.get("c_main_sector_code") == null?"":resultHm.get("c_main_sector_code").toString();
                String sub_code = resultHm.get("c_sector_code") == null?"":resultHm.get("c_sector_code").toString();
                String state_code = resultHm.get("c_empl_state") == null?"":resultHm.get("c_empl_state").toString();

                String total_emp = resultHm.get("c_total_empl")!=null
                                    && !resultHm.get("c_total_empl").toString().isEmpty()?
                                        resultHm.get("c_total_empl").toString():
                                        "0";

                empl.put("psmb_no", resultHm.get("c_hrdc_no"));
                empl.put("mycoid", resultHm.get("c_mycoid"));
                empl.put("comp_id", id);
                empl.put("comp_name", resultHm.get("c_comp_name"));

                String reg_type_ = resultHm.get("c_empl_reg_type").toString();

                empl.put("reg_type", resultHm.get("c_empl_reg_type"));

                if(!StringUtils.isBlank(reg_type_) && reg_type_.equals("HQ")){
                    JSONArray brList = getBranchList(db, id);
                    empl.put("branch", brList);
                }else{
                    JSONObject hqJo = new JSONObject();
                    hqJo.put("hq_mycoid", resultHm.get("c_hq_mycoid"));
                    hqJo.put("hq_empId", resultHm.get("c_hq_id"));

                    empl.put("hq", hqJo);
                }

                empl.put("total_employees", total_emp);
                empl.put("is_under_legal", resultHm.get("c_is_under_legal"));
                empl.put("is_SME", resultHm.get("c_isSME"));
                empl.put("psmb_emp_status", resultHm.get("c_last_move"));
                empl.put("status", empStatus);
                empl.put("status_remarks", statusRemarks);
                empl.put("org_type", resultHm.get("c_empl_org_type"));
                empl.put("email", resultHm.get("c_req_email"));
                empl.put("primary_telno", resultHm.get("c_empl_tel_no_pri"));


                JSONObject address = CommonUtils.getArrangedJson();

                address.put("address1", resultHm.get("c_empl_address").toString());
                address.put("address2", resultHm.get("c_empl_address2").toString());
                address.put("address3", resultHm.get("c_empl_address3").toString());
                address.put("postcode", resultHm.get("c_empl_postcode").toString());
                address.put("city", resultHm.get("c_empl_city").toString());
                address.put("city_label", MasterSetupData.getCityLabel(db, resultHm.get("c_empl_city").toString()));
                address.put("state", resultHm.get("c_empl_state").toString());
                address.put("state_label", MasterSetupData.getStateLabel(db, resultHm.get("c_empl_state").toString()));
                address.put("country", resultHm.get("c_empl_country").toString());
                address.put("country_label", MasterSetupData.getCountryLabel(db, resultHm.get("c_empl_country").toString()));

                empl.put("address", address);

                empl.put("industry_sector", ind_code);
                empl.put("industry_sector_label", getIndustryLabel(ind_code));
                empl.put("div", div_code);
                empl.put("div_label", getDivLabel(div_code));
                empl.put("main_sector_code", main_code);
                empl.put("main_sector_label", getMainSectorLabel(main_code));
                empl.put("sub_sector_code", sub_code);
                empl.put("sub_sector_label", getSubSectorLabel(sub_code));

                empl.put("state", state_code);
                empl.put("state_label", getStateLabel(state_code));

                empl.put("code_classification", resultHm.get("c_code_classification"));
                empl.put("levy_percentage", resultHm.get("c_levy_perc"));
                empl.put("SME_category", resultHm.get("c_sme_category"));
                empl.put("SME_type", resultHm.get("c_sme_type"));

                empl.put("reg_dt", resultHm.get("dateCreated"));
                empl.put("verify_dt", resultHm.get("c_verify_dt"));
                empl.put("approve_dt", resultHm.get("c_approve_dt"));
                empl.put("levy_liability_dt", resultHm.get("c_levy_liab_pymnt_dt"));
                
                JSONObject dereg = new JSONObject();
                dereg.put("dereg_refno", dereg_refno);
                dereg.put("cease_ops_dt", cease_ops_dt);
                dereg.put("dereg_effective_dt", dereg_effective_dt);
                dereg.put("dereg_status", dereg_status);
                
                empl.put("deregistration_data", dereg);

                empl.put("other_contacts_details", getOtherContactsDetails(db, resultHm.get("id").toString()));
                empl.put("eDisbursement", getDisbursementDetails(db, resultHm.get("id").toString(), resultHm.get("c_emp_status").toString()));

                array.put(empl);
            }
        }

        return new JSONObject().put("dataArray", array);
    }
    
    public String addPagination(String query, String size, String page) throws EmployerAPIException{
        int sizeInt = 10;
        int pageInt = 1;
        String pageQ = "";
        boolean isPaging = false;
        
        if(!size.isEmpty()){
            isPaging = true;
            try{
                sizeInt = Integer.parseInt(size);
                size = Integer.toString(sizeInt);
            }catch(Exception e){
                throw new EmployerAPIException("Size must be a number "+e.getMessage());  
            }
        }
            
        if(!page.isEmpty()){
            isPaging = true;
            try{
                pageInt = Integer.parseInt(page);
                pageInt = (pageInt-1)*sizeInt;
                page = Integer.toString(pageInt);
                size = Integer.toString(sizeInt);
            }catch(Exception e){                
                throw new EmployerAPIException("Page must be a number "+e.getMessage());  
            }    
        }
        
        if(isPaging){
            if(!size.isEmpty()){
                pageQ = " LIMIT "+size+" ";
            }
            if(!page.isEmpty()){
                pageQ += " OFFSET "+page+" ";
            }
        }
        
        return query+pageQ;
    }

    public JSONArray getOtherContactsDetails(DBHandler db, String empId) throws JSONException{
        ArrayList<HashMap<String, String>> contactList = db.select(
                "SELECT * FROM "+Constants.TABLE.EMP_OTHER_CONTACT_DETAILS
                +" WHERE c_fk = ? ",
                new String[]{empId}
        );

        JSONArray contactJA = new JSONArray();

        for(HashMap hm: contactList){
            JSONObject obj = CommonUtils.getArrangedJson();

            obj.put("name", hm.get("c_name").toString());
            obj.put("designation", hm.get("c_designation").toString());
            obj.put("email", hm.get("c_email").toString());
            obj.put("telno", hm.get("c_tel_no").toString());
            obj.put("has_account", "no");
            obj.put("apps_permitted", "");

            contactJA.put(obj);
        }

        contactList = db.select(
            "select \n" +
            "concat( p.c_firstName, ' ', p.c_lastName) as c_name,\n" +
            "'' as c_designation,\n" +
            "p.c_tel_no,\n" +
            "p.c_email,\n" +
            "p.c_apps_permitted\n" +
            "from app_fd_empm_persons_stp p WHERE c_compId = ?",
            new String[]{empId}
        );

        for(HashMap hm: contactList){
            JSONObject obj = CommonUtils.getArrangedJson();

            obj.put("name", hm.get("c_name").toString());
            obj.put("designation", hm.get("c_designation").toString());
            obj.put("email", hm.get("c_email").toString());
            obj.put("telno", hm.get("c_tel_no").toString());
            obj.put("has_account", "yes");
            obj.put("apps_permitted", hm.get("c_apps_permitted").toString());

            contactJA.put(obj);
        }

        return contactJA;
    }

    public JSONObject getDisbursementDetails(DBHandler db, String empId, String status) throws JSONException{

        JSONObject disbData = CommonUtils.getArrangedJson();

        ArrayList<HashMap<String, String>> disbList = db.select(
                "SELECT "+
                "d.id, " +
                "d.c_bank_acc_no,\n" +
                "b.c_swift_code,\n" +
                "b.c_bank_name,  \n" +
                "d.c_bank_branch,\n" +
                "d.c_comp_name_bank_stmt,\n" +
                "d.c_status,\n" +
                "d.dateCreated,\n" +
                "d.dateModified,\n" +
                "d.c_name,\n" +
                "d.c_designation,\n" +
                "d.c_tel_no,\n" +
                "d.c_email " +
                "from "+Constants.TABLE.EMPREG_DISBURSE+" d\n" +
                "INNER JOIN app_fd_stp_bank b ON b.id = d.c_bankId " +
                "WHERE d.c_emp_fk=? ",
                new String[]{empId}
        );

        JSONObject disbSummary =  CommonUtils.getArrangedJson();
        disbSummary.put("size", Integer.toString(disbList.size()));

        String link = "";

        if(!disbList.isEmpty()){
            String linkId = disbList.get(0).get("id").toString();
            link = CommonUtils.getBaseURL()+"/jw/web/userview/empm/shareable_view/_/e_disb?embed=true&id="+linkId;
        }else{
            link = CommonUtils.getBaseURL()+"/jw/web/userview/empm/shareable_view/_/e_disb?embed=true&empId="+empId;
        }

        if(status.equals("ACTIVE")){
            disbSummary.put("link", link);
        }else{
            disbSummary.put("link", "");
        }

        disbData.put("summary", disbSummary);

        JSONArray disbDataArray = new JSONArray();
        for(HashMap hm: disbList){
            JSONObject obj = CommonUtils.getArrangedJson();

            JSONObject details = CommonUtils.getArrangedJson();
            details.put("acc_no", hm.get("c_bank_acc_no").toString());
            details.put("swift_code", hm.get("c_swift_code").toString());
            details.put("name", hm.get("c_bank_name").toString());
            details.put("branch", hm.get("c_bank_branch").toString());
            details.put("comp_name_in_bank_stmt", hm.get("c_comp_name_bank_stmt").toString());

            obj.put("bank_details", details);

            details = CommonUtils.getArrangedJson();
            details.put("disb_id", hm.get("id").toString());
            details.put("disb_dateCreated", hm.get("dateCreated").toString());
            details.put("disb_dateModified", hm.get("dateModified").toString());
            details.put("disb_status", hm.get("c_status").toString());

            obj.put("disb_details", details);

            disbDataArray.put(obj);
        }

        disbData.put("details", disbDataArray);
        return disbData;
    }

    public String getIndustryLabel(String id){
        String query = "select concat(c_industry_sector_code, ' - ', c_industry_sector) as label "
                + "from app_fd_stp_industry_sector s where id = ?";
        HashMap result = db.selectOneRecord(query, new String[]{id});

        if(result == null){
            return "";
        }else{
            return result.get("label").toString();
        }
    }

    public String getDivLabel(String id){
        String query = "select concat(c_div_code, ' - ', c_descr) as label "
                + "from app_fd_stp_industry_div s where id = ?";
        HashMap result = db.selectOneRecord(query, new String[]{id});

        if(result == null){
            return "";
        }else{
            return result.get("label").toString();
        }
    }

    public String getMainSectorLabel(String id){
        String query = "select concat(c_main_sector_code, ' - ', c_descr) as label "
                + "from app_fd_stp_main_sector s where id = ?";
        HashMap result = db.selectOneRecord(query, new String[]{id});

        if(result == null){
            return "";
        }else{
            return result.get("label").toString();
        }
    }

    public String getSubSectorLabel(String id){
        String query = "select concat(c_sub_sector_code , ' - ', c_descr) as label "
                + "from app_fd_stp_sub_sector i where id = ?";
        HashMap result = db.selectOneRecord(query, new String[]{id});

        if(result == null){
            return "";
        }else{
            return result.get("label").toString();
        }
    }

    public String getStateLabel(String id){
        String query = "select s.c_state from app_fd_stp_state s where id = ?";
        HashMap result = db.selectOneRecord(query, new String[]{id});

        if(result == null){
            return "";
        }else{
            return result.get("c_state").toString();
        }
    }

    public JSONObject updateEmpData() throws JSONException, IOException, ParseException{

        JSONObject reqBody = HttpUtil.getRequestBody(request);
        JSONObject empl = new JSONObject();

        String hrdc_no = reqBody.optString("psmb_no", "");
        String new_empyee_no = reqBody.optString("employee_no", "0");
        String modifiedByName = reqBody.optString("modifiedBy", "SYSTEM");

        String query = "SELECT id FROM app_fd_empm_reg WHERE c_hrdc_no = ?";
        HashMap result_hm = db.selectOneRecord(query, new String[]{hrdc_no});

        if(result_hm == null){
            empl.put("Status", "FAILED");
            empl.put("Msg", "Employer not found");
            return empl;
        }

        String mainKey = result_hm.get("id").toString();

        HashMap empHm = new HashMap();
        empHm.put("curr_total_employees", new_empyee_no);
        empHm.put("modifiedByName", modifiedByName);
        empHm.put("modifiedBy", modifiedByName);

        CommonUtils.saveUpdateForm(Constants.APP_ID.EMPM,
                Constants.FORM_ID.APPLICATION_EMPDETAILS, mainKey, empHm);

        ArchiveBinder ab = new ArchiveBinder();
//        ab.buildAudit(db, mainKey, "Data Update", "");

        empl.put("Status", "SUCCESS");
        return empl;
    }

    public JSONObject updateEmpData2() throws JSONException, IOException, ParseException{

        JSONObject reqBody = HttpUtil.getRequestBody(request);
        JSONObject empl = new JSONObject();

        ArrayList<HashMap<String, String>> value_change_list = new ArrayList();

        JSONArray update = reqBody.getJSONArray("update");
        JSONArray where = reqBody.getJSONArray("where");
        String modifiedBy = reqBody.optString("modifiedBy", "SYSTEM");

        if(update == null || where == null){
            empl.put("Status", "FAILED");
            empl.put("Msg", "Incomplete information");
            return empl;
        }

        HashMap reqUpd = new HashMap();
        reqUpd.put("empl_no", "c_curr_total_employees");
        reqUpd.put("legal_status", "c_is_under_legal");
        reqUpd.put("legal_start_dt", "c_legal_start_dt");

        reqUpd.put("code_classification", "c_code_classification");
        reqUpd.put("levy_liab_pymnt_dt", "c_levy_liab_pymnt_dt");
        reqUpd.put("levy_perc", "c_levy_perc");
        reqUpd.put("mycoid", "c_mycoid");
        reqUpd.put("comp_name", "c_comp_name");

        HashMap newValueHm = new HashMap();

        boolean permitted = true;

        String updStmt = "";
        String qStmt = "";
        String whrStmt = " WHERE ";

        EmpmObj eo = null;

        for(int x=0;x<where.length();x++){
            JSONObject whrObj = where.getJSONObject(x);

            String column = whrObj.getString("field");

            if(x==0){
                whrStmt += column.equals("id")?column:"c_"+column;
            }else{
                whrStmt += "AND "+(column.equals("id")?column:"c_"+column);
            }

            if(column.equals("id")){
                eo = new EmpmObj(db, EmpmObj.BY_ID, whrObj.getString("value"));
            }else if(column.contains("hrdc_no")){
                eo = new EmpmObj(db, EmpmObj.BY_HRDC_NO, whrObj.getString("value"));
            }else if(column.contains("mycoid")){
                eo = new EmpmObj(db, EmpmObj.BY_MYCOID, whrObj.getString("value"));
            }

            whrStmt += "='"+whrObj.getString("value")+"' ";
        }

        ArrayList<HashMap<String, String>> chgeList = new ArrayList();

        for(int x=0;x<update.length();x++){
            JSONObject updObj = update.getJSONObject(x);

            String field_name = updObj.optString("field","");

            if(!reqUpd.containsKey(field_name)){
                permitted = false;
                break;
            }

            field_name = reqUpd.get(field_name).toString();
            String value = updObj.optString("value","");

            if(field_name.equals("c_levy_liab_pymnt_dt") || field_name.equals("c_legal_start_dt")
            ){
                value = CommonUtils.set_DT_ChangeDateFormatString(value, "yyyy-MM-dd");
            }

            if(x==0){
                qStmt += field_name;
                updStmt += field_name+"='"+value+"' ";
            }else{
                qStmt += ","+field_name;
                updStmt += ","+field_name+"='"+value+"' ";
            }
            
            boolean approval_upd = false;
        
            if(field_name.equals("c_code_classification") || field_name.equals("c_levy_liab_pymnt_dt") || field_name.equals("c_levy_perc")){
                approval_upd = true;
            }
            
            if(eo!=null) {                
                if(!value.equals(eo.getField(field_name)) && !approval_upd){
                    chgeList = AuditTrailUtil.buildChangeAuditHm(db, 
                            (String) field_name, value, eo.getField(field_name), chgeList, Constants.CACHE_TYPE.MAIN_EMP_DATA);
                }
                
            }

            newValueHm.put(field_name, value);
        }

        if(!permitted){
            empl.put("Status", "FAILED");
            empl.put("Msg", "Update Column not permitted");
            return empl;
        }

        String query = "SELECT id,"+qStmt+" FROM app_fd_empm_reg"+whrStmt;
        HashMap empDataHm = db.selectOneRecord(query);

        if(empDataHm == null){
            empl.put("Status", "FAILED");
            empl.put("Msg", "Employer not found");
            return empl;
        }

        String id = empDataHm.getOrDefault("id", "").toString();
        String q = "UPDATE app_fd_empm_reg SET "+updStmt+" WHERE id = '"+id+"'";
        int i = db.update(q);

        if(i==0){
            empl.put("Status", "FAILED");
            empl.put("Msg", "None updated");
            empl.put("Query", q);

            return empl;
        }

        String remarks = "";
        if(!chgeList.isEmpty()){
            remarks += Constants.SEE_DETAIL_WORD;
            
            new AuditTrailUtil().insertAuditTrail2(db, id, modifiedBy, "Employer Data Update", remarks, true, chgeList);
        }


        

//        value_change_list = getValueChangeList(newValueHm, result_hm);
//        
//        //create audit trail
//        java.util.Date dt = new java.util.Date();
//        
//        HashMap aud_hm = new HashMap();
//        aud_hm.put(STATUS, "Data Update");
//        aud_hm.put(REMARK, "");
//        aud_hm.put(AUDIT_FK, id);
//        aud_hm.put(DATEMODIFIED, dt);
//        aud_hm.put(MODIFIEDBY, modifiedBy);
//        aud_hm.put(MODIFIEDBYNAME, modifiedBy);
//        aud_hm.put(VALUE_CHGE_LIST, value_change_list);       
//        
//        String aud_id = insertAuditTrail(aud_hm);

        empl.put("Status", "SUCCESS");
        empl.put("Query", q);

        return empl;
    }

    public JSONObject deregEmp() throws JSONException, IOException, ParseException{
        JSONObject resJO = new JSONObject();
        JSONObject ploadJO = HttpUtil.getRequestBody(request);

        if(ploadJO==null || ploadJO.has("empId") || !ploadJO.has("pic")){
            resJO.put("Status", "FAILED");
            resJO.put("Msg", "Incorrect payload format");

            return resJO;
        }

        JSONArray ploadArr = ploadJO.getJSONArray("pic");
        JSONObject picJO = ploadArr.getJSONObject(0);

        String picName = picJO.getString("name");
        String picDesg = picJO.getString("desg");
        String picTelNo = picJO.getString("telNo");
        String picEmail = picJO.getString("email");

        String empId = ploadJO.getString("empId");
        String emp_ceaseOpsDt = ploadJO.getString("cease_ops_dt");

//        resJO.put("dt", CommonUtils.getCurrentDateTime("YYYY-MM-dd hh:mm:ss"));

        EmpmObj emp = new EmpmObj(db, EmpmObj.BY_ID, empId);

        if(emp.getId().isEmpty()){
            resJO.put("Status", "FAILED");
            resJO.put("Msg", "Employer not found");

            return resJO;
        }

        HashMap drgHm = new HashMap();
        drgHm.put("dreg_mycoid", emp.getMycoid());
        drgHm.put("comp_name", emp.getCompName());
        drgHm.put("dreg_emp_id", empId);
        drgHm.put("submitting_name", picName);
        drgHm.put("submitting_desg", picDesg);
        drgHm.put("submitting_tel_no_pri", picTelNo);
        drgHm.put("submitting_email_pri", picEmail);
        drgHm.put("dreg_reason", "Write-Off");
        drgHm.put("cease_ops_dt", emp_ceaseOpsDt);
        drgHm.put("dreg_remarks", "Applied from levy");
        drgHm.put("is_merging", "No");
        drgHm.put("form_type", "4");

        String drgId = CommonUtils.saveUpdateForm2("", Constants.FORM_ID.EMPLOYER_DEREG_FORM, "", drgHm);

        UserDao ud = (UserDao) AppUtil.getApplicationContext().getBean("userDao");
        User user = ud.getUserById(emp.getMycoid());

        String userId = user == null? "":user.getId();

        //api to start process
        WorkflowManager wm = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");


        AppService appService = (AppService) WorkflowUtil.getApplicationContext().getBean("appService");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();

        if(appService == null){
            LogUtil.info(this.getClass().toString(), "NO APPSERVICE!!!");

            resJO.put("Status", "ERROR");
            resJO.put("Msg", "APPSERVICE not available");

            return resJO;
        }else if(appDef == null){
            LogUtil.info(this.getClass().toString(), "NO APPDEF!!!");

            resJO.put("Status", "ERROR");
            resJO.put("Msg", "APPDEF not available");

            return resJO;
        }

        try{
            //get process
            WorkflowProcess process = appService.getWorkflowProcessForApp(appDef.getAppId(), appDef.getVersion().toString(), "emp_deregistration");
            //start process
            wm.processStart(process.getId(), null, null, null, drgId, false);

            resJO.put("status", "SUCCESS");
            resJO.put("msg", "");
            resJO.put("empMyCoID", emp.getMycoid());
            resJO.put("empId", emp.getId());
        }catch(Exception e){
            e.printStackTrace();
            resJO.put("Status", "ERROR");
            resJO.put("Msg", e.getMessage());
        }

        return resJO;
    }

    public JSONObject getPnSResult() throws JSONException{

        JSONArray cpArray = new JSONArray();

        String parentId = request.getParameter("parentId");
        String searchMycoid = request.getParameter("searchMycoid");
        LogUtil.info(this.getClass().toString(), searchMycoid);
        String sql = "SELECT id, concat( c_mycoid, ' (',c_hrdc_no, ')') c_mycoid, c_comp_name "
                + "FROM app_fd_empm_reg s WHERE c_mycoid = ? ";

        ArrayList<HashMap<String, String>> resultList = db.select(sql, new String[]{searchMycoid});
        LogUtil.info(this.getClass().toString(), resultList.toString());
        for(HashMap hm:resultList){
            String empId = hm.get("id").toString();
            String mycoid = hm.get("c_mycoid").toString();
            String comp_name = hm.get("c_comp_name").toString();

            boolean isSubCp = isCpSub(empId, parentId);
            String radioBtnHtml = buildHtml(isSubCp,empId, parentId);

            JSONObject obj = new JSONObject();

            obj.put("id", empId);
            obj.put("mycoid", mycoid);
            obj.put("comp_name", comp_name);
            obj.put("html", radioBtnHtml);

            cpArray.put(obj);
        }
        LogUtil.info(this.getClass().toString(), cpArray.toString());
        return new JSONObject().put("dataArray", cpArray);
    }

    public JSONObject getValidCompanies() throws JSONException{
        //if match valid company mycoid, return reg_id
        JSONArray cpArray = new JSONArray();

        String mycoid = request.getParameter("mycoid");
        String currId = request.getParameter("currentEmpId");

        String sql =
                "select * from app_fd_empm_reg r\n" +
                "WHERE not exists(\n" +
                "  select * from app_fd_empm_parent_sub s \n" +
                "  where s.c_parent_id = ? and s.c_subsidiary_id = r.id \n" +
//                "#   and c_type = 'SUBSIDIARY' -- where already subbed\n" +
                "  union\n" +
                "  select * from app_fd_empm_parent_sub s \n" +
                "  where s.c_subsidiary_id = ? and c_parent_id = r.id\n" +
//                "#   and c_type = 'PARENT' -- wont find his head\n" +
                ")\n" +
//                "and id != ? \n" +
                "and r.c_mycoid = ? \n" +
                "and r.c_emp_status = 'ACTIVE'";

        ArrayList<HashMap<String, String>> resultList = db.select(sql, new String[]{
                currId, currId, mycoid
        });

        EmpmObj currEmp = new EmpmObj(db, EmpmObj.BY_ID,currId );

        for(HashMap hm:resultList){
            String empId = hm.get("id").toString();
            String comp_name = hm.get("c_comp_name").toString();
            mycoid = hm.get("c_mycoid").toString();

            JSONObject obj = new JSONObject();
            obj.put("id", empId);
            obj.put("comp_name", comp_name);
            obj.put("mycoid", mycoid);

            obj.put("curr_comp_name", currEmp.getCompName());
            obj.put("curr_mycoid", currEmp.getMycoid());

            if(empId.equals(currId)){
                obj.put("is_self", true);
            }else{
                obj.put("is_self", false);
            }

            cpArray.put(obj);
        }

        return new JSONObject().put("dataArray", cpArray);
    }

    private boolean isCpSub(String empId, String parentId){
        String sql = "SELECT * FROM app_fd_empm_parent_sub WHERE c_parent_id = ?"
                + " and c_subsidiary_id = ? ";
        HashMap hm = db.selectOneRecord(sql, new String[]{parentId, empId});

        if(hm!=null){
            return true;
        }else{
            return false;
        }
    }

    private String buildHtml(boolean isSubCp, String empId, String parentId){

        String html = "<div class='switch-field'> ";

        if(isSubCp){
            html +=
                "   <input type='radio' id='yes-"+empId+"' name='switch-"+empId+"' value='insert' checked/>" +
                "   <label for='yes-"+empId+"'>Yes</label>" +
                "   <input type='radio' id='no-"+empId+"' name='switch-"+empId+"' value='delete' />" +
                "   <label for='no-"+empId+"'>No</label>" ;
        }else{
            html +=
                "   <input type='radio' id='yes-"+empId+"' name='switch-"+empId+"' value='insert'/>" +
                "   <label for='yes-"+empId+"'>Yes</label>" +
                "   <input type='radio' id='no-"+empId+"' name='switch-"+empId+"' value='delete' checked/>" +
                "   <label for='no-"+empId+"'>No</label>" ;
        }

        html +=  "</div>" +
                "<script> " +
                " $('input[type=radio][name=switch-"+empId+"]').change(function() { " +
                "   handleAPI(this.value, '"+parentId+"', '"+empId+"') " +
                " });" +
                "</script>";
        return html;
    }

    public JSONObject insertCpToSub() throws JSONException{

        JSONObject obj = new JSONObject();

        String pCpId = request.getParameter("parentId");
        String insertId = request.getParameter("insertId");
        String action = request.getParameter("action");

        if(StringUtils.isBlank(pCpId) ||
                StringUtils.isBlank(insertId) ||
                StringUtils.isBlank(action)){
            obj.put("Status", "FAILED");
            obj.put("Msg", "Empty Parameter(s)");

            return obj;
        }

        String result = "";
        int result_int = 0;
        boolean cpExist = false;

        String sql = "SELECT * FROM app_fd_empm_parent_sub WHERE "
                + "c_parent_id = ? and c_subsidiary_id = ? ";

        HashMap hm = db.selectOneRecord(sql, new String[]{pCpId, insertId});
        if(hm!=null){
            cpExist = true;
        }

        if(action.equals("insert") && !cpExist){
            hm = new HashMap();
            hm.put("parent_id", pCpId);
            hm.put("subsidiary_id", insertId);

            result = CommonUtils.saveUpdateForm2("",
                    Constants.FORM_ID.FORMID_PARENT_SUB, "", hm);
        }

        if(action.equals("delete") && cpExist){
            sql  = "DELETE FROM app_fd_empm_parent_sub WHERE "
                    + "c_parent_id = ? and c_subsidiary_id = ?";

            result_int = db.delete(sql, new String[]{pCpId, insertId});
        }

        if(!result.isEmpty() || result_int > 0){
            obj.put("Status", "SUCCESS");
        }else{
            obj.put("Status", "FAILED");
            obj.put("parent_id", pCpId);
            obj.put("subsidiary_id", insertId);
            obj.put("action", action);
        }

        return obj;
    }


    private String insertAuditTrail(HashMap aud_hm) {

        String date = aud_hm.get(DATEMODIFIED).toString();
        String modifiedBy = aud_hm.get(MODIFIEDBY).toString();
        String modifiedByName = aud_hm.get(MODIFIEDBYNAME).toString();
        String status = aud_hm.get(STATUS).toString();
        String remarks = aud_hm.get(REMARK).toString();
        String fk = aud_hm.get(AUDIT_FK).toString();

        HashMap hm = new HashMap();
        hm.put("createdBy", modifiedBy);
        hm.put("createdByName", modifiedByName);
        hm.put("modifiedBy", modifiedBy);
        hm.put("modifiedByName", modifiedByName);
        hm.put("status", status);
        hm.put("remarks", remarks);
        hm.put("fk", fk);

        String aud_id = CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM,
                                                    Constants.FORM_ID.AUDIT_TRAIL,
                                                    "", hm);

        if(aud_hm.get(VALUE_CHGE_LIST)==null){
            return aud_id;
        }

        for(HashMap each_hm:(ArrayList<HashMap<String, String>>)aud_hm.get(VALUE_CHGE_LIST)){

            hm = new HashMap();
            hm.put("field_name", each_hm.get(FIELD_NAME)==null?"":each_hm.get(FIELD_NAME));
            hm.put("prev_value", each_hm.get(PREV_VALUE)==null?"":each_hm.get(PREV_VALUE));
            hm.put("curr_value", each_hm.get(CURR_VALUE)==null?"":each_hm.get(CURR_VALUE));
            hm.put("fk", aud_id);

            CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM,
                                        Constants.FORM_ID.AUDIT_TRAIL_SUB,
                                        "", hm);
        }

        return aud_id;
    }

    private HashMap getDeregDt(String reg_id) {
        String query = "Select c_cease_ops_dt, c_dereg_refno, c_effective_dt, c_flow_status "
                + "from app_fd_empm_dereg "
                + "WHERE c_dreg_emp_id = ? ORDER BY dateCreated limit 1";
        HashMap<String, String> dreg_data = db.selectOneRecord(query, new String[]{reg_id});

        return dreg_data;
    }

    private ArrayList<HashMap<String, String>> getValueChangeList(HashMap newValueHm, HashMap result_hm) {

        ArrayList<HashMap<String, String>> audit_list = new ArrayList();
//        KeywordDictionary kd = new KeywordDictionary(db, Constants.TYPE_EMP_REG);

        Set set = newValueHm.entrySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            Map.Entry mentry = (Map.Entry) iterator.next();

            String prev_value = result_hm.get(mentry.getKey()) == null?"":result_hm.get(mentry.getKey()).toString();
            String new_value = mentry.getValue() == null?"":mentry.getValue().toString();

            if(new_value.equals(prev_value)){
                continue;
            }

            HashMap aud_hm = new HashMap();
            aud_hm.put(PREV_VALUE, prev_value);
            aud_hm.put(CURR_VALUE, new_value);
            aud_hm.put(FIELD_NAME, mentry.getKey());

            audit_list.add(aud_hm);
        }
//        LogUtil.info("newValueHm test", audit_list.toString());      
        return audit_list;
    }

    public JSONObject insertAuditTrail() throws IOException, ParseException, JSONException{
        JSONObject reqBody = HttpUtil.getRequestBody(request);

        String empId = reqBody.get("empId")==null?"":reqBody.get("empId").toString();
        String actionBy = reqBody.get("actionBy")==null?"":reqBody.get("actionBy").toString();
        String status = reqBody.get("status")==null?"":reqBody.get("status").toString();
        String remarks = reqBody.get("remarks")==null?"":reqBody.get("remarks").toString();

        EmpmObj eo = new EmpmObj(db,EmpmObj.BY_ID, empId);

        JSONObject resJO = new JSONObject();
        resJO.put("result", "FAILED");

        if(eo.getId().isEmpty()){
            resJO.put("remarks", "Employer not found");
            return resJO;
        }

        if(actionBy.isEmpty() || status.isEmpty() || remarks.isEmpty()){
            resJO.put("remarks", "Not enough data");
            return resJO;
        }

        ArrayList list = new ArrayList();

        String id = new AuditTrailUtil().insertAuditTrail2(db, empId, actionBy, status, remarks, false, list);

        if(!id.isEmpty()){
            resJO.put("result", "SUCCESS");
            return resJO;
        }

        return resJO;
    }

    private JSONArray getBranchList(DBHandler db, String id) throws JSONException {
        ArrayList<HashMap<String, String>> brList = db.select(
                "SELECT id, c_comp_name, c_mycoid FROM "
                + Constants.TABLE.EMPREG+" r WHERE "
                + "r.c_hq_id = ?",
                new String[]{id}
        );

        JSONArray ja = new JSONArray();

        for(HashMap br:brList){
            JSONObject obj = new JSONObject();

            obj.put("id", br.get("id").toString());
            obj.put("comp_name", br.get("c_comp_name").toString());
            obj.put("mycoid", br.get("c_mycoid").toString());

            ja.put(obj);
        }

        return ja;
    }
}
