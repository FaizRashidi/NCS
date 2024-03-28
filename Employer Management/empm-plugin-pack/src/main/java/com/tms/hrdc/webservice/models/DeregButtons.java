/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.webservice.models;

import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
//import com.tms.hrdc.util.Constants.URL;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.EmpModuleSetup;
import com.tms.hrdc.util.HttpUtil;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.joget.commons.util.LogUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class DeregButtons {
    
    final String F4 = "f4";
    final String F4_OB = "f4_ob";
    final String F4A = "f4a";
    final String F4A_OB = "f4a_ob";
    final String F5 = "f5";
        
    HttpServletRequest request;
    DBHandler db;
    HttpUtil http;
    
    private void msg(String msg){
        LogUtil.info(this.getClass().getName(), msg);
    }
    
    public DeregButtons(DBHandler d, HttpServletRequest req) throws SQLException {
        db = d;
        request = req;      
    }
    
    /*
    API
    */
    public JSONObject checkDeregPending() throws JSONException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException{  
        List<String> strings = Arrays.asList(new String[]{F4, F4_OB, F4A, F4A_OB, F5});
        
        String form = request.getParameter("form");     
        String empId = request.getParameter("empId");    
        
        if(StringUtils.isBlank(form) || StringUtils.isBlank(empId)){
            return new JSONObject().put("message", "Parameter Incomplete, "
                    + "please include param 'form' and/or 'empId'");
        }
        
        if(!strings.contains(form.toLowerCase())){
            return new JSONObject().put("message", "Unrecognized form value, "
                    + "please choose either "+strings.toString());
        }
        EmpmObj emp = new EmpmObj(db, EmpmObj.BY_ID, empId);
        msg(form+" start "+empId + " hrdcno "+ emp.getHrdcNo());
        
        JSONObject payload = new JSONObject();            

        String psmbNo = emp.getHrdcNo();
        String mycoid = emp.getMycoid();
        String returnMsg = "PENDING";
        String formBypass = "";
        msg( mycoid+" "+psmbNo);
        boolean hasPending = false;

        boolean pendingLevy = checkLevyPending(mycoid);
        boolean pendingGrant = checkGrantPending(psmbNo);
        boolean pendingClaim = checkClaimPending(mycoid);

        if(pendingLevy || pendingGrant || pendingClaim){
            hasPending = true;
        }

        boolean bypassStatus = getFormBypassStatus(db, form, empId);        
        String empStatus = emp.getDataStatus();                
        
        if(!hasPending){
            returnMsg = "PASS";
        }

//        if(bypassStatus && hasPending){
//            returnMsg = "PASS";
//        }           

        payload.put("bypass_check", bypassStatus);
        payload.put("pendingCheck", returnMsg);
        
        if(empStatus.equals("DEREGISTERING")){
            payload.put("isDeregistering", true);
        }else if(empStatus.equals("DEREGISTER_APPROVED")){
            payload.put("isDeregistering", "DEREGISTERED");
        }else{
            payload.put("isDeregistering", false);
        }
            
        return payload;
    }
    
    /*
    API
    */
    public JSONObject setWDButton() throws JSONException{
        String recId = request.getParameter("recId");        
        
        String aTag = "";
        
        HashMap hm = db.selectOneRecord(
            "SELECT id FROM "+Constants.TABLE.DEREG+" WHERE c_f5_fk = ?",
            new String[]{recId}
        );

//        LogUtil.info("DEREG WD FORM 5", "id "+recId+", got data? "+
//                (hm==null?"NO DATA":hm.toString() ) );
        
        if(hm!=null){
            return handleForm5WD(recId);
        }
        
        hm = db.selectOneRecord(
                "SELECT id, c_flow_status, c_approve_dt, c_dereg_refno FROM "+Constants.TABLE.DEREG+" WHERE id = ?",
                new String[]{recId}
        );

//        LogUtil.info("DEREG WD", "id "+recId+", got data? "+
//                (hm==null?"NO DATA":hm.toString() ) );
        
        if(hm==null){
            return new JSONObject();
        }
        
        String deregStatus = hm.get("c_flow_status").toString().toUpperCase();
        String approved_dt = hm.get("c_approve_dt").toString();
        String dereg_refno = hm.get("c_dereg_refno").toString();

        // PENDING.., QUERY SENT, APPROVED, REJECTED, CANCELLED, WITHDRAWN
        
        HashMap wdStatusHm = getWDStatus(db, recId);
        HashMap cancelStatusHm = getCancelStatus(db, recId);
        
        String wdId = "";
        String wdStatus = "";
        String cancelStatus = "";

//        LogUtil.info("DEREG WD", dereg_refno+" status-> "+deregStatus+", aprv dt-> "+approved_dt);
        
        if(wdStatusHm!=null){
            wdStatus = wdStatusHm.get("c_status").toString();
            wdId = wdStatusHm.get("id").toString();
        }
        if(cancelStatusHm!=null){
            cancelStatus = cancelStatusHm.get("c_status").toString();
            wdId = wdStatusHm.get("id").toString();
        }
        
        if(!deregStatus.equals("APPROVED") && 
            !deregStatus.equals("CANCELLED") &&
            !deregStatus.equals("WITHDRAWN") &&
            !deregStatus.equals("REJECTED") &&
            !deregStatus.isEmpty()
        ){
            if(wdStatus.isEmpty() || wdStatus.equals("REJECTED")){
                 aTag =  "<br /><a class='action_btn' href='dereg_apply_wd?deregId="+recId+"&wd_type=WITHDRAW'>Withdraw</a>";
            }else{
                 aTag =  "<br /><a class='action_btn' href='dereg_apply_wd_view?id="+wdId+"&wd_type=WITHDRAW'>View Withdrawal Application</a>";
            }
        }
        
        if(deregStatus.equals("APPROVED") ){
            if(isNowBeforeCancelPeriod(db, approved_dt) && (cancelStatus.isEmpty() || cancelStatus.equals("REJECTED"))){
//                 aTag =  "<a class='action_btn' href='dereg_apply_wd?deregId="+recId+"&wd_type=CANCEL'>Cancel</a>";
            }else if(isNowBeforeCancelPeriod(db, approved_dt) && !cancelStatus.isEmpty()){
                 aTag =  "<br /><a class='action_btn' href='dereg_apply_wd_view?id="+wdId+"&wd_type=WITHDRAW'>View Cancellation Application</a>";
            }
            
            if(!isNowBeforeCancelPeriod(db, approved_dt) && !cancelStatus.isEmpty()){
                 aTag =  "<br /><a class='action_btn' href='dereg_apply_wd_view?id="+wdId+"&wd_type=WITHDRAW'>View Cancellation Application</a>";
            }
        }
        
        if(deregStatus.equals("CANCELLED") ){            
            aTag =  "<br /><a class='action_btn' href='dereg_apply_wd_view?id="+wdId+"&wd_type=CANCEL'>View Cancellation Application</a>";
        }
        
        if(deregStatus.equals("WITHDRAWN") ){            
             aTag =  "<br /><a class='action_btn' href='dereg_apply_wd_view?id="+wdId+"&wd_type=WITHDRAW'>View Withdrawal Application</a>";
        }
        
        return new JSONObject().put("value", aTag);
    }
    
    public JSONObject setCancelButton() throws JSONException{
        String recId = request.getParameter("recId");        
        String userId = StringUtils.isBlank(request.getParameter("userId"))?"":request.getParameter("userId").toString();

        boolean showButton = true;
        String aTag = "";
        
        EmpModuleSetup stp = new EmpModuleSetup(db);
        String cancelPeriod = stp.getCancellationPeriod();
        int cancelPeriod_int = 0;    
        int dayDiff_int = 0;
        
        //get all dereg == 'APPROVED' || F4 APPROVED || F4A APPROVED
        HashMap hm = db.selectOneRecord(
                "SELECT r.c_dreg_emp_id, r.c_dreg_mycoid, r.c_flow_status,r.c_approve_dt," +
                "DATEDIFF(CURDATE(), STR_TO_DATE(c_approve_dt, '%Y-%m-%d %H:%i:%s.%f')) AS day_difference," +
                "modifiedByName " +
                "FROM "+Constants.TABLE.DEREG+" r " +
                "WHERE id=? AND (c_flow_status = ? OR c_flow_status = ? OR  c_flow_status = ?) ",
                new String[]{recId, "APPROVED", "FORM 4 APPROVED", "FORM 4A APPROVED"}                
        );
        
        if(hm==null){
            return new JSONObject();
        }
        
        String deregStatus = hm.get("c_flow_status").toString();
        String approved_dt = hm.get("c_approve_dt").toString();
        String dayDiff = hm.get("day_difference").toString();

//        LogUtil.info(this.getClass().toString(),"DAYDIFF "+dayDiff+", STP "+cancelPeriod);
        
        try{
            cancelPeriod_int = Integer.parseInt(cancelPeriod);
            dayDiff_int = Integer.parseInt(dayDiff);
        }catch(Exception e){            
        }
        
        HashMap cancelRec = db.selectOneRecord(
                "SELECT * FROM "+Constants.TABLE.DEREG_WD+" d "
                + " WHERE c_wd_type = 'CANCELLATION' AND c_dereg_id = ? AND (c_approval = 'Approved' OR c_approval IS NULL)"
                + "ORDER BY dateCreated DESC LIMIT 1",
                new String[]{recId});
        
        boolean cr8New = true;
        String cancelId = "";

//        LogUtil.info(this.getClass().toString(),cancelRec==null?"":cancelRec.toString());
        
        if(cancelRec!=null){          
            String cancelStatus = cancelRec.get("c_status").toString();
            cancelId = cancelRec.get("id").toString();
            cr8New = false;
//            if(!cancelStatus.equals("REJECTED")){
//                cr8New = false;
//            }
        }

//        LogUtil.info(this.getClass().toString(),"DAYDIFF "+Boolean.toString((dayDiff_int < cancelPeriod_int))+", CR8NEW "+Boolean.toString(cr8New));

        if(showButton) {
            if (cr8New && (dayDiff_int < cancelPeriod_int)) {
                aTag = "<br /><a class='action_btn' href='dereg_apply_cancel?deregId=" + recId + "&wd_type=CANCELLATION'>Cancel</a>";
            }

            if (!cr8New) {
                aTag = "<br /><a class='action_btn' href='dereg_apply_cancel_view?id=" + cancelId + "&wd_type=CANCELLATION'>View Cancellation Application</a>";
            }
        }
        
        return new JSONObject().put("value", aTag);
    }

    private HashMap getWDStatus(DBHandler db, String parentId){
        
        HashMap hm = db.selectOneRecord(
                "SELECT id, c_status FROM "+Constants.TABLE.DEREG_WD+" WHERE c_dereg_id = ? AND c_wd_type = 'WITHDRAW' ORDER BY dateCreated desc",
                new String[]{parentId}
        );
        
        return hm;
    }
    
    private HashMap getCancelStatus(DBHandler db, String parentId){
        
        String status = "";
        
        HashMap hm = db.selectOneRecord(
                "SELECT id, c_status FROM "+Constants.TABLE.DEREG_WD+" WHERE c_dereg_id = ? AND c_wd_type = 'CANCELLATION' ORDER BY dateCreated desc",
                new String[]{parentId}
        );
        
        return hm;
    }

    private boolean isNowBeforeCancelPeriod(DBHandler db, String approved_dt) {
        int cancelDaysPeriod = 0;
        HashMap hm = db.selectOneRecord(
                "SELECT * FROM "+Constants.TABLE.STP_EMPREG+" WHERE id = ?",
                new String[]{Constants.DATA_ID.MAIN_SETUP_ID}
        );
        
        if(hm!=null){
            String cnclPeriodStr = hm.getOrDefault("c_dereg_cancel_period", "0").toString();
            
            try{
                cancelDaysPeriod = Integer.parseInt(cnclPeriodStr);
            }catch(Exception e){                
            }
        }
        
        String format = "yyyy-MM-dd hh:mm:ss";
        
        Date date = CommonUtils.set_DT_String2Date(approved_dt, format);
        Date todayDt = CommonUtils.set_DT_String2Date(CommonUtils.get_DT_CurrentDateTime(format), format);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        // Add X days to the given date
        calendar.add(Calendar.DAY_OF_MONTH, cancelDaysPeriod);
        
        Date dateAfterPeriod = calendar.getTime();

        // Check if today has passed the calculated date
        if (todayDt.after(dateAfterPeriod)) {
            return false;
        } else {
            return true;
        }        
    }
    
    int statusCode = 500;

    private boolean checkLevyPending(String psmbno) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, JSONException  {
        
        boolean hasPending = false;
        JSONObject resp_data = new JSONObject();
        
        try{
            http = new HttpUtil();            
            String url = Constants.LEVYAPI.PENDING+"?mycoid="+psmbno;
            http.sendGetRequest(url);
            resp_data = http.getJSONResponse();
            statusCode = http.getStatusCode();
            
        }catch(Exception e){
            e.printStackTrace();
        }

        if(statusCode!=200){
            return hasPending;
        }

        //check interest pending
        if(resp_data.has("interest_pending") && !hasPending){
            hasPending = hasPending(resp_data,"interest_pending");
        }
        if(resp_data.has("installment_pending") && !hasPending){
            hasPending = hasPending(resp_data,"installment_pending");
        }
        if(resp_data.has("arrears_pending") && !hasPending){
            hasPending = hasPending(resp_data,"arrears_pending");
        }
        if(resp_data.has("forfeit_pending") && !hasPending){
            hasPending = hasPending(resp_data,"forfeit_pending");
        }
        if(resp_data.has("waive_pending") && !hasPending){
            hasPending = hasPending(resp_data,"waive_pending");
        }
        if(resp_data.has("adjustment_pending") && !hasPending){
            hasPending = hasPending(resp_data,"adjustment_pending");
        }
        if(resp_data.has("form3_pending") && !hasPending){
            hasPending = hasPending(resp_data,"form3_pending");
        }
        if(resp_data.has("refund_pending") && !hasPending){
            hasPending = hasPending(resp_data,"refund_pending");
        }
        if(resp_data.has("exemption_pending") && !hasPending){
            hasPending = hasPending(resp_data,"exemption_pending");
        }
        return hasPending;
    }
    
    private boolean hasPending(JSONObject resp_data,String header) throws JSONException{
        JSONArray array = resp_data.getJSONArray(header);
        JSONObject obj = new JSONObject();
        
        if(array.length()>0){
            obj = array.getJSONObject(0);
        }else{
            return false;
        }
        
        Integer count = 0;
        
        if(obj.has("pending_application")){
            try{
                count = (Integer) obj.get("pending_application");
            }catch(Exception e){
                e.printStackTrace();
            }            
        }
        if(obj.has("count_pending")){
            try{
                count = (Integer) obj.get("count_pending");
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        
        if(count>0){
            return true;
        }
        return false;
    }

    private boolean checkGrantPending(String psmbno) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, JSONException {
        
        boolean hasPending = false;
        JSONObject resp_data = new JSONObject();
        try{
            http = new HttpUtil();
            http.sendGetRequest(Constants.GRANTAPI.SUMMARY+"?status=Pending&psmb_no="+psmbno);

            resp_data = http.getJSONResponse();
            statusCode = http.getStatusCode();

        }catch(Exception e){
            e.printStackTrace();
        }

        if(statusCode!=200){
            return hasPending;
        }

        if(resp_data.has("data_string")){
            return hasPending;
        }

        if(resp_data.has("data")){
            JSONArray array = resp_data.getJSONArray("data");

            if(array.length() > 0){
                hasPending = true;
            }
        }
        
        return hasPending;
    }

    private boolean checkClaimPending(String mycoid) throws IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, JSONException {
        boolean hasPending = false;
        JSONObject resp_data = new JSONObject();
        String claimApiStatus = "";
        List pendingStatus = Arrays.asList(new String[]{"NEW", "PROCESSING", "SENT TO", "RECOMMENDED", "QUERY"});
        
        try{
            http = new HttpUtil();

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("mycoid", mycoid);

            http.setBody(jsonBody);
            http.sendPostRequest(Constants.CLAIMAPI.SUMMARY+"?action=claimSummaryEmployer");
            resp_data = http.getJSONResponse();
            statusCode = http.getStatusCode();
            
        }catch(Exception e){
            e.printStackTrace();
        }

        if(statusCode!=200){
            return hasPending;
        }
        
        if(resp_data!=null){
            claimApiStatus = resp_data.getString("status");
        }
        if(StringUtils.isBlank(claimApiStatus)){
            return hasPending;
        }
        if(claimApiStatus.equals("ERROR")){
            return hasPending;
        }

        JSONArray empClaim = resp_data.getJSONObject("data").getJSONArray("claimApiStatus");
        for(int i=0;i<empClaim.length();i++){
            JSONObject claim = empClaim.getJSONObject(i);
            String claim_status = claim.getString("claimApiStatus");

            if(!StringUtils.isBlank(claim_status) && pendingStatus.contains(claim_status.toUpperCase())){
                hasPending = true;
                break;
            }
        }       
        
        return hasPending;
    }

    private boolean getFormBypassStatus(DBHandler db, String form, String empId) {
//        String query = "SELECT  c_dereg_no_check as f4, "
//                + "             c_dereg_bhlf_no_check as f4_ob, "
//                + "             c_f4a_dereg_no_check as f4a, "
//                + "             c_f4a_dereg_bhlf_no_check as f4a_ob, "
//                + "             c_f5_dereg_no_check as f5 "
//                + "     FROM    app_fd_empm_reg_stp WHERE id = ?";
        String query = "";
        
        msg("Form "+form.toLowerCase());
        
        switch(form.toLowerCase()){
            case "f4a":
            case "f4a_ob":                
                query = "SELECT c_f4a_dereg_no_check FROM app_fd_empm_dreg_bypass_stp WHERE c_f4a_dereg_no_check LIKE '%"+empId+"%'";
                break;
            case "f4":
            case "f4_ob":                
                query = "SELECT c_dereg_no_check FROM app_fd_empm_dreg_bypass_stp WHERE c_dereg_no_check LIKE '%"+empId+"%'";
                break;                
            case "f5":
                query = "SELECT c_f5_dereg_no_check FROM app_fd_empm_dreg_bypass_stp WHERE c_f5_dereg_no_check LIKE '%"+empId+"%'";
                break;           
        }
        
        HashMap result = db.selectOneRecord(query);
        
        boolean checkVal = false;      
        
        
        if(result!=null){
            msg("bypass "+result.toString());
            checkVal = true;
        }else{
//            msg("bypass error "+query);
        }
        
        return checkVal;
    }

    private JSONObject handleForm5WD(String recId) throws JSONException {       
        
        // check if wd for form 5 exist
        HashMap hm = db.selectOneRecord(
                "SELECT id, c_status FROM "+
                Constants.TABLE.DEREG_WD+" WHERE c_dereg_id = ? "+
                "AND c_dereg_form_type = '5' AND c_wd_type = 'WITHDRAW' "+
                "ORDER BY dateCreated desc LIMIT 1",
                new String[]{recId}
        );
        
        String aTag = "";
        String form5WdId = "";
        
        boolean showWDButton = true;
        
        if(hm!=null){
            String status = hm.get("c_status").toString();
            form5WdId = hm.get("id").toString();
            if(status.equals("APPROVED")){
                showWDButton = false;
            }

            LogUtil.info("Form 5", hm.toString());
        }
        
        if(showWDButton){
            aTag =  "<br /><a class='action_btn' href='dereg_apply_wd?deregId="+recId+"&wd_type=WITHDRAW'>Withdraw</a>";
        }
        
        if(!showWDButton && !form5WdId.isEmpty()){
            aTag =  "<br /><a class='action_btn' href='dereg_apply_wd_view?id="+form5WdId+"&wd_type=WITHDRAW'>View Withdrawal Application</a>";
        }
        
        return new JSONObject().put("value", aTag);
    }
    
}
