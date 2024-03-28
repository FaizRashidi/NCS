/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.webservice.models;

import com.google.zxing.WriterException;
import com.tms.hrdc.binder.AssignmentManager;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class AssignmentsImpl {
    
    HttpServletRequest request;
    HttpServletResponse response;
    DBHandler db;
    
    static final String OFFICER = "officer";
    static final String APPROVER = "approver";
    static final String ERDADMIN = "erdAdmin";
    
    public AssignmentsImpl(DBHandler d, HttpServletRequest req) throws SQLException {
        db = d;
        request = req;       
    }
    
    private void msg(String msg){
        LogUtil.info(this.getClass().toString(), msg);
    }
    
    // To get Assigned Users to be displayed for customer, but strip the sv names.
    public JSONObject getAssignedOfficerDetailed() throws JSONException{
        String recId = request.getParameter("recId");
        
        String[] asgnees = getProcessingOfficer(recId);
//            asgnees = getActuallyAssignedOfficers(db, recId, OFFICER);

        //if no data, find dereg instead
        HashMap empHm = db.selectOneRecord(
            "SELECT * FROM\n" +
            "(\n" +
            "  select           r.c_empl_email_pri as email, re.c_ref_no as refno, re.id " +
            "  from		"+Constants.TABLE.EMPREG+" r " +
            "  INNER JOIN 	"+Constants.TABLE.EMPREG_APPL+" re ON re.c_empl_fk = r.id " +
            "  UNION " +
            "  select		r.c_empl_email_pri, d.c_dereg_refno, d.id " +
            "  from		"+Constants.TABLE.DEREG+" d " +
            "  INNER JOIN	"+Constants.TABLE.EMPREG+" r ON d.c_dreg_emp_id = r.id " +
            ") a WHERE id = ? ",
            new String[]{recId}
        );
        
        String refno = empHm==null?"":empHm.get("refno").toString();
        String reqEmail = empHm==null?"":empHm.get("email").toString();
        
        JSONArray officerArr = new JSONArray();
        JSONObject officerData = new JSONObject();
        
        if(asgnees.length > 0){
            for(String asgn:asgnees){
                
                HashMap userDetails = db.selectOneRecord(
                        "SELECT \n" +
                        "u.c_username, u.c_email, u.c_office_telephone_mobile_no\n" +
                        "FROM app_fd_stp_hrdc_usr u " +
                        "WHERE u.c_username = ?",
                        new String[]{asgn});
                
                String email = "";
                String telno = "";
                if(userDetails!=null){
                    email = userDetails.get("c_email").toString();
                    telno = userDetails.get("c_office_telephone_mobile_no").toString();
                }
                
                JSONObject officerjson = new JSONObject();
        
                officerjson.put("officer_name", getName(db, asgn));
                officerjson.put("officer_email", email);
                officerjson.put("officer_telno", telno);
                
                officerArr.put(officerjson);
            }
        }
        
        officerData.put("officer", officerArr);
        officerData.put("refno", refno);
        officerData.put("sent_mail", reqEmail);
        
//        LogUtil.info(this.getClass().toString(), "SUBMIT REPORT "+officerData.toString());
        
        return officerData;
    }
    
    // To get Assigned Users to be displayed for customer, but strip the sv names.
    public JSONObject getAssignedOfficerSimplified() throws JSONException{
        String recId = StringUtils.isBlank(request.getParameter("recId"))?"":request.getParameter("recId");

        String[] asgnees = getProcessingOfficer(recId);
        
        JSONArray officerArr = new JSONArray();
        JSONObject officerData = new JSONObject();
        
        if(asgnees.length > 0){
            for(String asgn:asgnees){
                officerArr.put(new JSONObject().put("officer_name", getName(db, asgn)));
            }
        }
        
        officerData.put("officer", officerArr);
        
//        LogUtil.info(this.getClass().toString(), "SUBMIT REPORT "+officerData.toString());
        
        return officerData;
    }

    private String[] getProcessingOfficer(String recId){
        String isForm5 = "";
        HashMap f1_hm = db.selectOneRecord(
                "SELECT * FROM (\n" +
                        "  SELECT id, dateCreated, '' isform5 FROM app_fd_empm_regAppl r WHERE r.c_empl_fk =\n" +
                        "  (\n" +
                        "    select c_fk FROM (\n" +
                        "     select id, c_fk FROM app_fd_empm_form1\n" +
                        "      WHERE id = ? \n" +
                        "     UNION\n" +
                        "     select id, c_fk FROM app_fd_empm_form1A\n" +
                        "      WHERE id = ? \n" +
                        "    ) x \n" +
                        "  ) \n" +
                        "  UNION\n" +
                        "  SELECT id, dateCreated, 'true' isform5 FROM app_fd_empm_dereg d WHERE d.c_f5_fk = ? " +
                        ") r order by r.dateCreated desc limit 1",
                new String[]{recId, recId, recId}
        );

        if(f1_hm!=null){
            recId = f1_hm.get("id").toString();
            isForm5 = f1_hm.getOrDefault("isform5","").toString();
        }

        //get activityID
        //if dereg - get current level with getworkflow

        // if activity dead, f1/f1a get original regappl id and refer table
        String[] asgnees = {};

        HashMap asgHm = getTaskData(db, recId);
        String assignmentId = "";
        String curr_assignees = "";

        if(asgHm==null){
//            return new JSONObject().put("value", "");
        }else{
//            LogUtil.info("ASSIGNEE API", "task data "+asgHm.toString());
            assignmentId = asgHm.get("assignmentId").toString();
            curr_assignees = asgHm.get("curr_assignees").toString();
        }

        if(getWorkFlowVar(assignmentId).equals("officerId") ||
                getWorkFlowVar(assignmentId).equals("dereg_officer_id")){
            asgnees = getActuallyAssignedOfficers(db, recId, OFFICER);
//            LogUtil.info("ASSIGNEE API", "task data officerID "+Arrays.asList(asgnees).toString());
        }

        if(getWorkFlowVar(assignmentId).equals("approverId")||
                getWorkFlowVar(assignmentId).equals("dereg_approver_id")){
            asgnees = getActuallyAssignedOfficers(db, recId, APPROVER);
//            LogUtil.info("ASSIGNEE API", "task data approverID "+Arrays.asList(asgnees).toString());
        }

        if(asgnees.length==0){
            asgnees = getActuallyAssignedOfficers(db, recId, APPROVER);
//            LogUtil.info("ASSIGNEE API", "task data setup approver "+Arrays.asList(asgnees).toString());
        }

        if(asgnees.length==0){
            asgnees = getActuallyAssignedOfficers(db, recId, OFFICER);
//            LogUtil.info("ASSIGNEE API", "task data setup officer "+Arrays.asList(asgnees).toString());
        }

        if(asgnees.length==0){
            asgnees = getActuallyAssignedOfficers(db, recId, ERDADMIN);
//            LogUtil.info("ASSIGNEE API", "task data setup erdadmin "+Arrays.asList(asgnees).toString());
        }

        if(asgnees.length==0 && !curr_assignees.isEmpty()){
            asgnees=curr_assignees.split(";");

//            LogUtil.info("ASSIGNEE API", "task data proc participant "+Arrays.asList(asgnees).toString());
        }

        return asgnees;
    }
    
//    private static String assignmentId = "";

    private String[] getReceiveFrom(String recId){
        
        String isForm5 = "";
        HashMap f1_hm = db.selectOneRecord(
                "SELECT * FROM (\n" +
                        "  SELECT id, dateCreated, '' isform5 FROM app_fd_empm_regAppl r WHERE r.c_empl_fk =\n" +
                        "  (\n" +
                        "    select c_fk FROM (\n" +
                        "     select id, c_fk FROM app_fd_empm_form1\n" +
                        "      WHERE id = ? \n" +
                        "     UNION\n" +
                        "     select id, c_fk FROM app_fd_empm_form1A\n" +
                        "      WHERE id = ? \n" +
                        "    ) x \n" +
                        "  ) \n" +
                        "  UNION\n" +
                        "  SELECT id, dateCreated, 'true' isform5 FROM app_fd_empm_dereg d WHERE d.c_f5_fk = ? " +
                        ") r order by r.dateCreated desc limit 1",
                new String[]{recId, recId, recId}
        );

        if(f1_hm!=null){
            recId = f1_hm.get("id").toString();
            isForm5 = f1_hm.getOrDefault("isform5","").toString();
        }

        //get activityID
        //if dereg - get current level with getworkflow

        // if activity dead, f1/f1a get original regappl id and refer table
        String[] asgnees = {};

        HashMap asgHm = getTaskData(db, recId);
        String assignmentId = "";
        String curr_assignees = "";
        
        

        if(asgHm==null){
//            return new JSONObject().put("value", "");
        }else{
//            LogUtil.info("ASSIGNEE API", "task data "+asgHm.toString());
            assignmentId = asgHm.get("assignmentId").toString();
            curr_assignees = asgHm.get("curr_assignees").toString();
        }
        
        
        if(assignmentId.contains("request_changes")){
            msg("getTAskData "+asgHm.toString()+" , recId "+recId);
        }

        if(getWorkFlowVar(assignmentId).equals("officerId") ||
                getWorkFlowVar(assignmentId).equals("dereg_officer_id")){
            asgnees = getRecvdFrom(db, recId, OFFICER);
//            LogUtil.info("ASSIGNEE API", "task data officerID "+Arrays.asList(asgnees).toString());
        }
        
        
        if(assignmentId.contains("request_changes")){
            msg("asgnees getWFVAR "+Arrays.asList(asgnees).toString());
        }

        if(getWorkFlowVar(assignmentId).equals("approverId")||
                getWorkFlowVar(assignmentId).equals("dereg_approver_id")){
            asgnees = getRecvdFrom(db, recId, APPROVER);
//            LogUtil.info("ASSIGNEE API", "task data approverID "+Arrays.asList(asgnees).toString());
        }

        if(asgnees.length==0){
            asgnees = getRecvdFrom(db, recId, APPROVER);
//            LogUtil.info("ASSIGNEE API", "task data setup approver "+Arrays.asList(asgnees).toString());
        }
        
        if(assignmentId.contains("request_changes")){
            msg("asgnees APPROVER "+Arrays.asList(asgnees).toString());
        }

        if(asgnees.length==0){
            asgnees = getRecvdFrom(db, recId, OFFICER);
//            LogUtil.info("ASSIGNEE API", "task data setup officer "+Arrays.asList(asgnees).toString());
        }

        if(assignmentId.contains("request_changes")){
            msg("asgnees OFFICER "+Arrays.asList(asgnees).toString());
        }
        
        if(asgnees.length==0){
            asgnees = getRecvdFrom(db, recId, ERDADMIN);
//            LogUtil.info("ASSIGNEE API", "task data setup erdadmin "+Arrays.asList(asgnees).toString());
        }
//        LogUtil.info("ASSIGNEE API", "task data size  "+Integer.toString(asgnees.length)+", "+Arrays.asList(asgnees).toString());
        if(asgnees.length==0 && !curr_assignees.isEmpty()){
            asgnees=curr_assignees.split(";");

            
        }
//        LogUtil.info("ASSIGNEE API", "task data proc participant "+Arrays.asList(asgnees).toString());
        return asgnees;
    }
    
    // To get current approver names, but strip the sv names.
    public JSONObject getAssignees() throws JSONException{
        String recId = request.getParameter("recId");
        HashMap asgHm = getTaskData(db, recId);
        
        if(asgHm==null){
            return new JSONObject().put("value", "");
        }
        
        String curr_assignees_name = "";
        String curr_assignees_usr = asgHm.get("curr_assignees").toString();
        String assignmentId = asgHm.get("assignmentId").toString();
        String[] asgnees = {};
        String[] currArr = curr_assignees_usr.split(Constants.SEPARATOR);
        List currList = Arrays.asList(currArr);
         
        if(getWorkFlowVar(assignmentId).equals("officerId") ||
            getWorkFlowVar(assignmentId).equals("dereg_officer_id")){
            asgnees = getActuallyAssignedOfficers(db, recId, OFFICER);
        }
        
        if(getWorkFlowVar(assignmentId).equals("approverId")||
            getWorkFlowVar(assignmentId).equals("dereg_approver_id")){
            asgnees = getActuallyAssignedOfficers(db, recId, APPROVER);
        }
        
        ArrayList nameDisplay = new ArrayList();
        
        if(asgnees.length > 0){
            for(String asgn:asgnees){
                if(currList.contains(asgn)){
                    nameDisplay.add(getName(db, asgn));
                }
            }
        }
        
        curr_assignees_name = StringUtils.join(nameDisplay, "</li><li>");
        curr_assignees_name = "<ul><li>"+curr_assignees_name+"</li></ul>";
        
        return new JSONObject().put("value", curr_assignees_name);
    }
    
    public JSONObject getCurrentTaskReturnAsgnee() throws JSONException{
        String actId = request.getParameter("actId");
        
        JSONObject jo = new JSONObject();
        jo.put("return_asgn", "");
        jo.put("msg", "Error");
        
        //activityId = get id to get orgini id 
        // return top stack
        
        HashMap hm = db.selectOneRecord(
                "SELECT w.* from wf_process_link w " +
                "INNER JOIN SHKAssignmentsTable s ON s.ActivityProcessId = w.processId " +
                "where s.ActivityId = ? " +
                "LIMIT 1 ", new String[]{actId});
        
        if(hm==null){
            return jo;
        }
        
        String recId = hm.get("originProcessId").toString();
        String type = getApprovalType(actId);
        
        hm = db.selectOneRecord(
                "SELECT * FROM "+Constants.TABLE.ASSIGNEES_DATA+" a "
                + "WHERE a.c_record_id = ? AND c_type = ? AND c_status = ? "
                + "ORDER BY dateCreated desc LIMIT 1"
                , new String[]{recId, type, "CURRENT"});
        
        if(hm==null){
            jo.put("return_asgn", "");
            jo.put("msg", "Task never forwarded so nothing to return to");            
            jo.put("block", "true");            
            return jo;
        }
        jo.put("return_asgn", AssignmentsImpl.getNameStr(db, hm.get("c_received_from").toString()));
        jo.put("msg", "");        
        
        return jo;
    }
        
    // To get Last Assignees approver names, but strip the sv names.
    public JSONObject getPrevAssignees() throws JSONException{
        String recId = request.getParameter("recId");
//        HashMap asgHm = getTaskData(db, recId);
//
//        if(asgHm==null){
//            return new JSONObject().put("value", "");
//        }
//
        String curr_assignees_name = "";
//        String curr_assignees_usr = asgHm.get("curr_assignees").toString();
//        String assignmentId = asgHm.get("assignmentId").toString();
//        String[] asgnees = {};
//        String[] currArr = curr_assignees_usr.split(Constants.SEPARATOR);
//        List currList = Arrays.asList(currArr);
//
//        if(getWorkFlowVar(assignmentId).equals("officerId") ||
//            getWorkFlowVar(assignmentId).equals("dereg_officer_id")){
//            asgnees = getRecvdFrom(db, recId, OFFICER);
//        }
//
//        if(getWorkFlowVar(assignmentId).equals("approverId")||
//            getWorkFlowVar(assignmentId).equals("dereg_approver_id")){
//            asgnees = getRecvdFrom(db, recId, APPROVER);
//        }
//
        List<String> nameDisplay = new ArrayList();
        String[] asgnees = {};
   
        try{
            asgnees = getReceiveFrom(recId);
            nameDisplay = (List) Arrays.asList(asgnees);
            
            for(int i=0;i<nameDisplay.size();i++){
                String name = nameDisplay.get(i);
                String full_name = getName(db, name);
                name = full_name.isEmpty()?name:full_name;
                nameDisplay.set(i, name);
            }
//            LogUtil.info("PREV NAME recId",recId+" "+nameDisplay.toString());
            curr_assignees_name = StringUtils.join(nameDisplay, "</li><li>");
        curr_assignees_name = "<ul><li>"+curr_assignees_name+"</li></ul>";
        }catch(Exception e){
            e.printStackTrace();
        }
        
        
//        if(asgnees.length > 0){
//            for(String asgn:asgnees){
//                LogUtil.info("PREV START "+recId, asgn);
//                if(currList.contains(asgn)){
//                    nameDisplay.add(getName(db, asgn));
//                }
//                String name = getCompanyName(db, recId, asgn);
//                LogUtil.info("PREV NAME 1 "+recId,name);
//                name = name.isEmpty()?getName(db, asgn):name;
//                LogUtil.info("PREV NAME 2 "+recId,name);
//                if(!nameDisplay.contains(asgn)){
//                    nameDisplay.add(name.isEmpty()?getCompanyName(db, recId):name);
//                    nameDisplay.add(asgn);
//                }
//            }
//        }
        
        
        
        return new JSONObject().put("value", curr_assignees_name);
    }
    
    public static String getCompanyName(DBHandler db, String recId, String asgnUsr){

        // dereg
        // reg
        // wd
        // cancel
        // reqChange
//        HashMap hm = db.selectOneRecord(
//                "select x.compId, CONCAT(r.c_mycoid, ' (', u.c_userId,')' ) as name from\n" +
//                        "(\n" +
//                        "  SELECT a.c_empl_fk as compId from app_fd_empm_regAppl a WHERE id = ? \n" +
//                        "  UNION\n" +
//                        "  SELECT d.c_dreg_emp_id from app_fd_empm_dereg d WHERE id = ? \n" +
//                        "  UNION\n" +
//                        "  SELECT c.c_emp_fk from app_fd_empm_reg_changes c WHERE id = ? \n" +
//                        "  UNION\n" +
//                        "  SELECT w.c_wd_emp_id from app_fd_empm_dereg_wd w WHERE id = ? \n" +
//                        ") x\n" +
//                        "INNER JOIN app_fd_empm_reg r ON r.id = x.compId\n" +
//                        "INNER JOIN app_fd_empm_usermap u on u.c_compId = x.compId\n" +
//                        "INNER JOIN dir_user d ON u.c_userId = d.id " +
//                        "WHERE u.id = ? ",
//                new String[]{recId, recId, recId, recId, asgnUsr});
        HashMap hm = db.selectOneRecord(
                "SELECT \n" +
                        "concat(r.c_so_name, ' (', r.c_empl_email_pri, ')' ) as pic\n" +
                        "from app_fd_empm_regAppl a \n" +
                        "INNER JOIN app_fd_empm_reg r on r.id = a.c_empl_fk\n" +
                        "WHERE a.id = ?\n" +
                        "UNION\n" +
                        "select  \n" +
                        "concat(d.c_submitting_name, ' (', d.c_submitting_email_pri, ')' ) as pic\n" +
                        "from app_fd_empm_dereg d\n" +
                        "where d.id = ?\n" +
                        "UNION\n" +
                        "select \n" +
                        "concat(c.createdByName, ' (', usr.email, ')')\n" +
                        "from \n" +
                        "app_fd_empm_reg_changes c\n" +
                        "inner join dir_user usr ON usr.id = c.createdBy\n" +
                        "where c.id = ? \n" +
                        "UNION\n" +
                        "select \n" +
                        "concat(c.createdByName, ' (', usr.email, ')')\n" +
                        "from \n" +
                        "app_fd_empm_dereg_wd c\n" +
                        "inner join dir_user usr ON usr.id = c.createdBy\n" +
                        "where c.id = ?",
                new String[]{recId, recId, recId, recId}
        );
        
        return hm==null?"":hm.get("name").toString();
    }

    public static String getCompanyName(DBHandler db, String recId){
        HashMap hm = db.selectOneRecord(
                "SELECT CONCAT(r.c_so_name, ' (', r.c_comp_name, ')') as name FROM "
                        + "app_fd_empm_reg r "
                        + "INNER JOIN app_fd_empm_regAppl ra ON ra.c_empl_fk = r.id "
                        + "WHERE ra.id = ?",
                new String[]{recId});

        return hm==null?"":hm.get("name").toString();
    }
    
    public static String getNameStr(DBHandler db, String csvUserId){
        
        String[] userIdArr = csvUserId.split(Constants.SEPARATOR);
        String username = "";
        
        for(String userId:userIdArr){
            username+= (username.isEmpty())?getName(db, userId):", "+getName(db, userId);
        }
        
        return username;
    } 
    
    public static String getName(DBHandler db, String usrId){
        HashMap hm = db.selectOneRecord(
                "SELECT CONCAT(firstName, ' ', lastName) as name FROM dir_user "
                        + "WHERE id = ?",
                new String[]{usrId});
        
        return hm==null?"":hm.get("name").toString();
    }    
    
    public static String getOldAssigner(DBHandler db, String proc) {
        String sql = "SELECT c_asgn_officer FROM "+Constants.TABLE.EMPREG
                + " WHERE id = ?";
        HashMap hm = db.selectOneRecord(sql, new String[]{proc});
        
        if(hm!=null){
            return hm.get("c_asgn_officer")==null?"":hm.get("c_asgn_officer").toString();
        }
        
        return "";
    }
    
    public static String getOldAssigner(DBHandler db, String wfV, String recId, String assignmentId) {
        
        String type = getApprovalType(assignmentId);
        
        String[] asgnees = getOriginalAssignees(db, recId, type); //x,x,x
//        ArrayList<HashMap <String, String>> sv = getSVs(db);        
        
        List<String> currList = new ArrayList<String>();
        Collections.addAll(currList, asgnees);
        
//        for(HashMap hm:sv){
//            String usr = hm.get("c_usr")!=null?hm.get("c_usr").toString():"";
//            if(!usr.isEmpty() && !currList.contains(usr)){
//                currList.add(usr);
//            }
//        }
//        
        return StringUtils.join(currList, Constants.SEPARATOR);
    }
     
    public static void saveOldAssignees(DBHandler db, String proc, String curr_assignees) {
        proc = CommonUtils.getEmpId_empReg(db, proc);
        String sql = "UPDATE "+Constants.TABLE.EMPREG+" SET c_asgn_officer = ? "
                + "WHERE id = ?";
        db.update(sql, new String[]{curr_assignees}, new String[]{proc});
    }
    
    public static void reassignToNew(String assignmentId, String wfV, String new_assignees) {
        new_assignees = new_assignees.replaceAll(",", ";");        
        WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager"); 
        workflowManager.activityVariable(assignmentId, wfV, new_assignees);        
        workflowManager.reevaluateAssignmentsForActivity(assignmentId);
    }
    
    public static String getApprovalType(String assignmentId){
        
        String type = "";
        if(getWorkFlowVar(assignmentId).equals("officerId") ||
            getWorkFlowVar(assignmentId).equals("dereg_officer_id")){
            type = OFFICER;
        }
        
        if(getWorkFlowVar(assignmentId).equals("approverId")||
            getWorkFlowVar(assignmentId).equals("dereg_approver_id")){
            type = APPROVER;
        }
        
        return type;
    }

    public static String getWorkFlowVar(String assignmentId) {
        if(assignmentId.contains("emp_registration_approval1")
                || assignmentId.contains("emp_registration_approval1_reprocessing")){
            return "officerId";
        }
        
        if(assignmentId.contains("emp_registration_approval2")
                || assignmentId.contains("emp_registration_approval2_reprocessing")){
            return "approverId";
        }
        
        //TODO cater for dereg
        if(assignmentId.contains("emp_deregistration_dereg_verify_form4")
            || assignmentId.contains("emp_deregistration_dereg_verify_form4_m")
            || assignmentId.contains("emp_deregistration_dereg_verify_form4_2")
            || assignmentId.contains("emp_deregistration_dereg_verify_form4_m_2")){
            return "dereg_officer_id";
        }
        
        if(assignmentId.contains("emp_deregistration_dereg_approval_form4_2")
            || assignmentId.contains("emp_deregistration_dereg_approval_form4_m_2")
            || assignmentId.contains("emp_deregistration_dereg_approval_form4_m")
            || assignmentId.contains("emp_deregistration_dereg_approval_form4")){
            return "dereg_approver_id";
        }
        
        if(assignmentId.contains("wd_approval_activity")
            || assignmentId.contains("deregCancellation_approval_activity")
            || assignmentId.contains("wo_approval")
            || assignmentId.contains("changes_approval")){
            return "approverId";
        }
        
        return "";
    }

    public static String includeSV(DBHandler db, String curr_assignees) {        
        ArrayList<HashMap<String, String>>  svList = getSVs(db);
        String[] currArr = curr_assignees.split(Constants.SEPARATOR);
                
        List<String> currList = new ArrayList<String>();
        Collections.addAll(currList, currArr);
        
        for(HashMap hm:svList){
            String usr = hm.get("c_usr").toString();
            if(!currList.contains(usr)){
                currList.add(usr);
            }
        }
        
        return StringUtils.join(currList, Constants.SEPARATOR);
    }
    
    public static String includeSV2(DBHandler db, ArrayList<HashMap<String, String>> svList,String curr_assignees) {        
//        ArrayList<HashMap<String, String>>  svList = getSVs(db);
        String[] currArr = curr_assignees.split(Constants.SEPARATOR);
                
        List<String> currList = new ArrayList<String>();
        Collections.addAll(currList, currArr);
        
        for(HashMap hm:svList){
            String usr = hm.get("c_usr").toString();
            if(!currList.contains(usr)){
                currList.add(usr);
            }
        }
        
        return StringUtils.join(currList, Constants.SEPARATOR);
    }
    
    public static ArrayList<HashMap<String, String>> getSVs(DBHandler db){
        String sql = "SELECT c_usr," +
                    " CONCAT(firstName, ' ', lastName) as name " +
                    "FROM app_fd_empm_appvr_usr_stp u " +
                    "INNER JOIN dir_user d ON d.id = u.c_usr  " +
                    "INNER JOIN app_fd_empm_appvr_stp s on  u.c_sv_fk = s.id " +
                    "where s.c_type = 'Employer' ";
        
        ArrayList<HashMap<String, String>> svList = db.select(sql);
        
        return svList;
    }
    
    public static ArrayList<HashMap<String, String>> getEgmntSVs(DBHandler db){
        String sql = "SELECT c_usr," +
                    " CONCAT(firstName, ' ', lastName) as name " +
                    "FROM app_fd_empm_appvr_usr_stp u " +
                    "INNER JOIN dir_user d ON d.id = u.c_usr  " +
                    "INNER JOIN app_fd_empm_appvr_stp s on u.c_eng_admin_fk = s.id " +
                    "where s.c_type = 'Engagement' ";
        
        ArrayList<HashMap<String, String>> svList = db.select(sql);
        
        return svList;
    }
    
    public static ArrayList<HashMap<String, String>> getPESVs(DBHandler db){
        String sql = "SELECT c_usr," +
                    " CONCAT(firstName, ' ', lastName) as name " +
                    "FROM app_fd_empm_appvr_usr_stp u " +
                    "INNER JOIN dir_user d ON d.id = u.c_usr  " +
                    "INNER JOIN app_fd_empm_appvr_stp s on u.c_officer_fk = s.id " +
                    "where s.c_type = 'Potential Employer' ";
        
        ArrayList<HashMap<String, String>> svList = db.select(sql);
        
        return svList;
    }
    
    
    
    public static ArrayList<HashMap<String, String>> getApprovers(DBHandler db){
        String sql = "SELECT c_usr," +
                    " CONCAT(firstName, ' ', lastName) as name " +
                    "FROM app_fd_empm_appvr_usr_stp u " +
                    "INNER JOIN dir_user d ON d.id = u.c_usr  " +
                    "INNER JOIN app_fd_empm_appvr_stp s on  u.c_approver_fk = s.id " +
                    "where s.c_type = 'Employer' ";
        
        ArrayList<HashMap<String, String>> svList = db.select(sql);
        
        return svList;
    }
    
    public static ArrayList<HashMap<String, String>> getOfficers(DBHandler db){
        String sql = "SELECT c_usr," +
                    " CONCAT(firstName, ' ', lastName) as name " +
                    "FROM app_fd_empm_appvr_usr_stp u " +
                    "INNER JOIN dir_user d ON d.id = u.c_usr  " +
                    "INNER JOIN app_fd_empm_appvr_stp s on  u.c_officer_fk = s.id " +
                    "where s.c_type = 'Employer' ";
        
        ArrayList<HashMap<String, String>> svList = db.select(sql);
        
        return svList;
    }
    
    public static HashMap getTaskData(DBHandler db, String proc) {
        
        //check if form5
        
        
        String sql = "select " +
                        " distinct ActivityId as assignmentId,\n" +
                        "(select \n" +
                        " 	group_concat(\n" +
                        "    ResourceId " +
                        "     SEPARATOR '"+Constants.SEPARATOR+"')\n" +
                        "  FROM SHKAssignmentsTable e WHERE\n" +
                        "      e.ActivityId = s.ActivityId\n" +
                        " ) as curr_assignees,\n" +
                        "(select \n" +
                        " 	group_concat( " +
                        "    concat(firstName, ' ', lastName ) "+
                        "     SEPARATOR '"+Constants.SEPARATOR+"') " +
                        "  FROM SHKAssignmentsTable e " +
                        "  INNER JOIN dir_user d ON d.id = e.ResourceId " +
                        "  WHERE\n" +
                        "      e.ActivityId = s.ActivityId\n" +
                        " ) as curr_assignees_name\n" +
                        "from SHKAssignmentsTable s\n" +
                        "inner join wf_process_link w on w.processId = s.ActivityProcessId\n" +
                        "where w.originProcessId = ? " +
//                        "AND activityId like '%empm_emp%' " +
                        "AND activityId not like '%query%'";
        
        HashMap hm = db.selectOneRecord(sql, new String[]{proc});
        
//        String usr = hm.get("curr_assignees").toString();
//        String name = hm.get("curr_assignees_name").toString();
        
        if(hm!=null){
            return hm;
        }
        
        return hm;
    }
    
    /*
    @param commaSeparatedName csv-name
    @param recvdFrom name
    @param recId foreignKey
    @param type approver/officer
    @param isFirstInsert true/false
    */
    public static void recordAssignedOfficers(String commaSeparatedName, 
            String recvdFrom, String recId, String type, boolean isFirstInsert){
        HashMap hm = new HashMap();
        hm.put("record_id", recId);
        hm.put("officer_in_charge", commaSeparatedName);
        hm.put("received_from", recvdFrom);
        hm.put("type", type);
        if(isFirstInsert){
            hm.put("status", Constants.STATUS.ASGN_STATUS.CURRENT_BASE);
        }else{
            hm.put("status", Constants.STATUS.ASGN_STATUS.CURRENT);
        }        
        
        CommonUtils.saveUpdateForm2("", Constants.FORM_ID.EMP_ASGN_DATA, "", hm);        
    }
    
    
    public static void recordAssignedOfficers(DBHandler db, String recvdFrom,
            ArrayList<HashMap<String, String>> data, String recId, String type){
        ArrayList aprvrList = new ArrayList();
        for(HashMap hm:data){     
            
            if(!aprvrList.contains(hm.get("c_usr").toString()) ){
                aprvrList.add(hm.get("c_usr").toString());
            }
        }        
        
        String usrs = String.join(Constants.SEPARATOR, aprvrList);
        
        int i = db.update(
                "UPDATE "+Constants.TABLE.ASSIGNEES_DATA+" SET c_officer_in_charge = ?, c_status = ? WHERE c_record_id = ? AND c_type = ?",
                new String[]{usrs, Constants.STATUS.ASGN_STATUS.CURRENT},
                new String[]{recId,type});
        
        if(i==0){
            HashMap hm = new HashMap();
            hm.put("record_id", recId);
            hm.put("received_from", recvdFrom);
            hm.put("officer_in_charge", usrs);
            hm.put("type", type);
            hm.put("status", Constants.STATUS.ASGN_STATUS.CURRENT_BASE);

            CommonUtils.saveUpdateForm2("", Constants.FORM_ID.EMP_ASGN_DATA, "", hm);       
        }
    }
    
    public static void updateAssignedOfficers(DBHandler db, String asgnees, String recId, String assignmentId, String usr_recvd_from){
        String type = getApprovalType(assignmentId);
        
        HashMap currAsgn = db.selectOneRecord(
                                "SELECT "
                                + "id, c_officer_in_charge "
                                + "FROM "+Constants.TABLE.ASSIGNEES_DATA+" "
                                + "WHERE c_record_id = ? and c_type = ? and "
                                + "c_status in (?,?)", 
                                new String[]{
                                    recId, type, 
                                    Constants.STATUS.ASGN_STATUS.CURRENT,
                                    Constants.STATUS.ASGN_STATUS.CURRENT_BASE
                                });
        
        HashMap currAsgnPrev = db.selectOneRecord(
                                "SELECT "
                                + "id, c_officer_in_charge "
                                + "FROM "+Constants.TABLE.ASSIGNEES_DATA+" "
                                + "WHERE c_record_id = ? and c_type = ? and "
                                + "c_status in (?) ORDER BY dateCreated DESC ", 
                                new String[]{
                                    recId, type, 
                                    Constants.STATUS.ASGN_STATUS.FORWARDED
                                });
        
        String oIC = currAsgn.get("c_officer_in_charge").toString();
        String asgnId = currAsgn.get("id").toString();
        
        String oICPrev = "";
        String asgnIdPrev = "";
        usr_recvd_from = usr_recvd_from.isEmpty()?oIC:usr_recvd_from;
        
        if(currAsgnPrev!=null){
            oICPrev = currAsgnPrev.get("c_officer_in_charge").toString();
            asgnIdPrev = currAsgnPrev.get("id").toString();
        }
        
        HashMap hm = new HashMap();
        
        if(!StringUtils.isBlank(asgnees)){
            hm.put("status", Constants.STATUS.ASGN_STATUS.FORWARDED);
            CommonUtils.saveUpdateForm2("", Constants.FORM_ID.EMP_ASGN_DATA, asgnId, hm);   
            
            AssignmentsImpl.recordAssignedOfficers(asgnees,
                    usr_recvd_from, recId, type, false);
        }else if(asgnees.isEmpty() && currAsgnPrev!=null){
            hm.put("status", Constants.STATUS.ASGN_STATUS.CURRENT);
            hm.put("received_from", usr_recvd_from);
            
            CommonUtils.saveUpdateForm2("", 
                    Constants.FORM_ID.EMP_ASGN_DATA, 
                    currAsgnPrev.getOrDefault("id", "").toString(), hm);  
            
            //remove asgnId
            int i = db.delete("DELETE FROM "+Constants.TABLE.ASSIGNEES_DATA
                    +" WHERE id =? ", 
                    new String[]{
                        currAsgn.getOrDefault("id", "").toString()
                    });
//            LogUtil.info("RETURN", "TOP DELETION "+Integer.toString(i));
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    public static String[] getRecvdFrom(DBHandler db, String recId, String type){
        
//        String[] arrr =  new String[]{};
//            String q = "SELECT c_received_from AS 'pic' "
//                + "FROM "+Constants.TABLE.ASSIGNEES_DATA+" \n"
//                + "WHERE c_record_id = '"+recId+"' \n"
//                + "AND c_type = '"+type+"' \n"
//                + "AND c_status IN ('"+Constants.STATUS.ASGN_STATUS.CURRENT_BASE+"','"+Constants.STATUS.ASGN_STATUS.CURRENT+"') "
//                + "ORDER BY dateCreated desc "
//                + "LIMIT 1";
        
            HashMap hm = db.selectOneRecord(
                "SELECT c_received_from AS 'pic' "
                + "FROM "+Constants.TABLE.ASSIGNEES_DATA+" "
                + "WHERE c_record_id = ? "
                + "AND c_type = ? "
                + "AND c_status IN (?,?) "
                + "ORDER BY dateCreated desc "
                + "LIMIT 1",
                new String[]{recId, type, 
                    Constants.STATUS.ASGN_STATUS.CURRENT_BASE,
                    Constants.STATUS.ASGN_STATUS.CURRENT
                });           
            
        String assignees = hm != null? hm.getOrDefault("pic","").toString():"";      
        
        if(assignees.isEmpty()){
            return new String[]{};
        }else{
            return assignees.split(Constants.SEPARATOR);
        }
    }
    
    public static String[] getActuallyAssignedOfficers(DBHandler db, String recId, String type){
        
        HashMap hm = db.selectOneRecord(
                "SELECT c_officer_in_charge AS 'pic' "
                + "FROM "+Constants.TABLE.ASSIGNEES_DATA+" "
                + "WHERE c_record_id = ? "
                + "AND c_type = ? "
                + "AND c_status IN (?,?) "
                + "ORDER BY dateCreated desc "
                + "LIMIT 1",
                new String[]{recId, type, 
                    Constants.STATUS.ASGN_STATUS.CURRENT,
                    Constants.STATUS.ASGN_STATUS.CURRENT_BASE
                });     
        String assignees = hm != null? hm.get("pic").toString():"";
        
        if(assignees.isEmpty()){
            return new String[]{};
        }else{
            return assignees.split(Constants.SEPARATOR);
        }
    }
    
    public static String[] getOriginalAssignees(DBHandler db, String recId, String type){
        
        HashMap hm = db.selectOneRecord(
                "SELECT c_officer_in_charge "
                        + "FROM "+Constants.TABLE.ASSIGNEES_DATA+" "
                        + "WHERE c_record_id = ? "
                        + "AND c_type = ? "
                        + "AND c_status = ? "
                        + "ORDER BY dateCreated desc LIMIT 1",
                new String[]{recId, type, Constants.STATUS.ASGN_STATUS.FORWARDED});     
        String assignees = hm != null? hm.get("c_officer_in_charge").toString():"";
        
        if(assignees.isEmpty()){
            return new String[]{};
        }else{
            return assignees.split(Constants.SEPARATOR);
        }
    }
}
