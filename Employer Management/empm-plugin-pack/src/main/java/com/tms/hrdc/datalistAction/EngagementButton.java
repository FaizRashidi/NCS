/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.datalistAction;

import com.tms.hrdc.dao.CurrentUser;
import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.KeywordDictionary;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.sql.DataSource;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.commons.util.LogUtil;
import org.joget.workflow.util.WorkflowUtil;

/**
 *
 * @author faizr
 */
public class EngagementButton extends DataListActionDefault {

    public String getName() {
        return this.getClass().toString(); 
    }

    public String getVersion() {
        return "1.0";
    }

    public String getDescription() {
        return "PE - Engagement Action Button"; 
    }

    public String getLinkLabel() {
        String label = getPropertyString("label");
        if (label == null || label.isEmpty()) {
            label = "Delete User & Record";
        }
        return label;
    }
    
    @Override
    public String getHref() {
        return getPropertyString("href"); 
    }

    @Override
    public String getTarget() {
        return "post";
    }

    @Override
    public String getHrefParam() {
        return getPropertyString("hrefParam"); 
    }

    @Override
    public String getHrefColumn() {
        return getPropertyString("hrefColumn");
    }
    
    public String getConfirmation() {
        String confirm = getPropertyString("confirmation");
        if (confirm == null || confirm.isEmpty()) {
            confirm = "Confirm?";
        }
        return confirm;
    }

    public String getLabel() {
        return "HRDC - EMPM - Engagement Multi-Function Button";
    }

    public String getClassName() {
        return this.getClass().getName();
    }

    public String getPropertyOptions() {
        String json = "[{\n"
                + "    title : 'Application & User Delete Button',\n"
                + "    properties : [{\n"
                + "        label : 'Label',\n"
                + "        name : 'label',\n"
                + "        type : 'textfield',\n"
                + "        description : 'Potential Employer Button',\n"
                + "        value : 'Potential Employer Button'\n"
                + "    },{\n" 
                + "            name: 'type', " 
                + "            label: 'PE Button Type', " 
                + "            type: 'radio', " 
                + "            options : [\n" 
                + "                {value: 'egmnt_writeOff', label : 'Write-Off'}," 
                + "                {value: 'batch', label : 'Batch'}," 
                + "                {value: 'push', label : 'Push'}," 
                + "                {value: 'pull', label : 'Pull'}," 
                
                + "                {value: 'egmnt_complete', label : 'Engagement Complete'}," 
                
                + "                {value: 'approve_egmnt_change', label : 'Accept Engagement Changes'}," 
                + "                {value: 'reject_egmnt_change', label : 'Reject Engagement Changes'}," 
                + "                {value: 'query_egmnt_change', label : 'Query Engagement Changes'}," 
                
                + "                {value: 'approve_value_change', label : 'Accept Value Changes'}," 
                + "                {value: 'reject_value_change', label : 'Reject Value Changes'}," 
                + "                {value: 'query_value_change', label : 'Query Value Changes'}" 
                + "            ]\n" 
                + "        }]\n"
                + "}]";
        return json;
    }
    
    int count = 0,updateCount = 0, existCount = 0;;
    
    @Override
    public DataListActionResult executeAction(DataList dataList, String[] rowKeys) {
        
        DBHandler db = new DBHandler();
        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
        } catch (SQLException ex) {
            ex.printStackTrace();
            db.closeConnection();
        } 
          
        String type = (String) getProperty("type");
        String message = "";
        String url = "";
        String batch = "";
        
        for(String id:rowKeys){
            
            switch(type){
                case "egmnt_writeOff":
                    String wOffId = PotEmpStateChangerButton.getWriteOffId(db);
                    handleWriteOff(db, id, wOffId);
                    message = Integer.toString(updateCount)+" records pending write-off ";
                break;
                case "push":
                    processEgmnt(db, id, type);
                    message = "Pushed "+Integer.toString(updateCount)+" Potential Employer(s) to AEU ";
                break;
                case "pull":
                    processEgmnt(db, id, type);
                    message = "Pulled "+Integer.toString(updateCount)+" Potential Employer(s)";
                break;
                case "egmnt_complete":
                    updateEgmntStatus(db, id, Constants.STATUS.EGMNT.COMPLETED);
                    message = "Completed "+Integer.toString(updateCount)+" Engagement(s)";
                break;     
                case "approve_egmnt_change":
                    updateEgmntStatus(db, id, Constants.STATUS.EGMNT.ACKNOWLEDGED);
                    message = "Approved "+Integer.toString(updateCount)+" Engagement(s)";
                break;  
                case "reject_egmnt_change":
                    updateEgmntStatus(db, id, Constants.STATUS.EGMNT.REJECTED);
                    message = "Rejected "+Integer.toString(updateCount)+" Engagement(s)";
                break;  
                case "query_egmnt_change":
                    updateEgmntStatus(db, id, Constants.STATUS.EGMNT.QUERY);
                    message = "Rejected "+Integer.toString(updateCount)+" Engagement(s)";
                break;  
                case "batch":
                    batch = batch.isEmpty()?createBatch():batch;
                    setBatch(db, id, batch);
                    message = Integer.toString(updateCount)+" records grouped in batch ("+batch+")";
                    url="pe_engagement_batch?d-4319158-fn_c_batch="+batch;
                break;
                case "approve_value_change":
                    updateChgeDataStatus(db, id, Constants.STATUS.EGMNT_CHANGE_STATUS.APPROVED);
                    message = "Approved "+Integer.toString(updateCount)+" Data ";
                break;  
                case "reject_value_change":
                    updateChgeDataStatus(db, id, Constants.STATUS.EGMNT_CHANGE_STATUS.REJECTED);
                    message = "Rejected "+Integer.toString(updateCount)+" Data ";
                break;
                case "query_value_change":
                    updateChgeDataStatus(db, id, Constants.STATUS.EGMNT_CHANGE_STATUS.QUERY);
                    message = "Queried "+Integer.toString(updateCount)+" Data ";
                break;  
            }            
        }
        
        // message building 
        if(type.equals("sub_batch")){
//            message = handleSubBatch(db, rowKeys);
            
            message = Integer.toString(count)+" records grouped in sub-batch "+message;
        }
        
        db.closeConnection();
        
        DataListActionResult result = new DataListActionResult();
        result.setType(DataListActionResult.TYPE_REDIRECT);
        
        if(url.isEmpty()){
            result.setUrl("REFERER");
        }else{
            result.setUrl(url);
        }
        
        if(!message.isEmpty()){
            result.setMessage(message);
        }
        
        return result;
    }
    
    private void handleWriteOff(DBHandler db, String egmId, String writeOffId){
        
        String sql = "SELECT c_pe_fk FROM "+Constants.TABLE.POT_EMP_ENGAGEMENT+" WHERE id = ? ";
        HashMap hm = db.selectOneRecord(sql, new String[]{egmId});
        
        if(hm==null){
            return;
        }
        
        String peId = hm.get("c_pe_fk")==null?"":hm.get("c_pe_fk").toString();        
        
        sql = "UPDATE "+Constants.TABLE.POT_EMP 
                + " SET c_status = ?, c_writeoff_fk = ? "
                + " WHERE id = ? ";
        
        count += db.update(sql, 
                new String[]{Constants.STATUS.POT_EMP.PENDING_WRITE_OFF, 
                                writeOffId}, 
                new String[]{peId});
        
        HashMap audHm = new HashMap();
        audHm.put("status", "Submitted for Write-Off");
        audHm.put("fk", writeOffId);
        audHm.put("createdByName", WorkflowUtil.getCurrentUserFullName());
        
        CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                                    Constants.FORM_ID.AUDIT_TRAIL, 
                                                    "", audHm);
        updateCount++;
    }
    
    //ENGAGEMENT ===============================================================
    private String processEgmnt(DBHandler db, String peId, String passType) {
        
        ArrayList<HashMap<String, String>> pList = isIDBatch(db, peId);
        String empId = "";
        
        if(pList.size() > 0){ //is batch handle multiple            
            for(HashMap pe:pList){
                peId = pe.get("id")==null?"":pe.get("id").toString();
                empId = pe.get("c_emp_fk")==null?"":pe.get("c_emp_fk").toString();
                handleEgmntFlow(db, peId,empId, passType);
            }
            return "";
        }
        String sql = "SELECT * FROM "+Constants.TABLE.POT_EMP+" WHERE id = ?";
        HashMap hm = db.selectOneRecord(sql, new String[]{peId});
        
        if(hm!=null){
            peId = hm.get("id")==null?"":hm.get("id").toString();
            empId = hm.get("c_emp_fk")==null?"":hm.get("c_emp_fk").toString();
            handleEgmntFlow(db, peId, empId, passType);
        }
        
        return "";
    }
    
    private boolean handleEgmntFlow(DBHandler db, String potEmpId, String empId, String passType){
        String sql = "SELECT * FROM "+Constants.TABLE.POT_EMP_ENGAGEMENT
                + " WHERE c_pe_fk = ? AND c_status != ? ";
        HashMap hm = db.selectOneRecord(sql, new String[]{potEmpId, 
            Constants.STATUS.EGMNT.ACKNOWLEDGED});
        
        if(hm!=null){
            // pe already pulled/pushed
            return false;
        }
        
        String currentUser = new CurrentUser().getId();
        String duplId = EmpmObj.duplicateEmpData(db, empId);
        String status = "", status_audit = "";
        
        if(passType.equals("push")){
            status = "PUSHED FROM POTENTIAL EMPLOYER";
            status_audit = "Data pushed from potential employer";
        }else{
            status = "PULLED FROM POTENTIAL EMPLOYER";          
            status = "Data pulled from potential employer";          
        }
                
        String engRefCode = db.selectOneValueFromId(Constants.TABLE.STP_EMPREG, 
                "c_pe_eng_refCode", Constants.DATA_ID.MAIN_SETUP_ID);
        String refno = engRefCode+"/"
                +CommonUtils.get_DT_CurrentDateTime("yyyy")+"/"
                +CommonUtils.getRefNo("6", "eng_batch_counter");
        
        HashMap eHm = new HashMap();
        eHm.put("pe_fk", potEmpId);
        eHm.put("ref_no", refno);
        eHm.put("temp_emp_fk", duplId);
        eHm.put("status", status);
        eHm.put("createdBy", new CurrentUser().getId());
        eHm.put("createdByName", new CurrentUser().getFullName());
        eHm.put("assigned_officer", ""); //TODO - find officer
        
        String engId = CommonUtils.saveUpdateForm2("", Constants.FORM_ID.POTEMP_ENGAGEMENT, 
                                    "", eHm);
        
        //update PE Status
        sql = "UPDATE "+Constants.TABLE.POT_EMP+" SET c_status = ? WHERE id = ?";
        db.update(sql, new String[]{Constants.STATUS.POT_EMP.ENGAGEMENT}, 
                new String[]{potEmpId});
        
        HashMap varHm = new HashMap();
        varHm.put("reqId", new CurrentUser().getId());
        
        new AuditTrailUtil().insertAuditTrail2(db, empId, new CurrentUser().getFullName(), status, "", false, new ArrayList());
        
        CommonUtils.startProcess(Constants.PROCESS_DEFKEYS.PE_ENGAGEMENT,
                engId, varHm);
        
        updateCount++;
        return true;
    }
    
    private ArrayList<HashMap<String, String>> isIDBatch(DBHandler db, String id){
        String sql = "SELECT * FROM "+Constants.TABLE.POT_EMP+" WHERE c_batch = ?";
        ArrayList<HashMap<String, String>> list = db.select(sql, new String[]{id});
        
        return list;
    }

    private void updateEgmntStatus(DBHandler db, String id, String status) {
        
        String sql = "select e.id, e.dateCreated, e.c_pe_fk, c_temp_emp_fk , p.c_emp_fk\n" +
                "from app_fd_empm_pe_egmnt e " +
                "inner join app_fd_empm_pe_potEmp p on p.id = e.c_pe_fk " +
                "WHERE e.id = ? ";

        HashMap egHm = db.selectOneRecord(sql, new String[]{id});

        String empId = egHm.get("c_emp_fk")==null?"":egHm.get("c_emp_fk").toString();
        String dupEmpId = egHm.get("c_temp_emp_fk")==null?"":egHm.get("c_temp_emp_fk").toString();
        String potEmpId = egHm.get("c_pe_fk")==null?"":egHm.get("c_pe_fk").toString();
        
        sql = "UPDATE "+Constants.TABLE.POT_EMP_ENGAGEMENT+" SET c_status = ?"
                + " WHERE id = ? ";
        
        db.update(sql, new String[]{status}, new String[]{id});
        
        switch(status){
            case Constants.STATUS.EGMNT.COMPLETED:
                
                sql = "DELETE FROM "+Constants.TABLE.AUDIT_SUB+" WHERE c_fk = ? ";
                int i = db.delete(sql, new String[]{id});
                
                sql = "SELECT * FROM "+Constants.TABLE.EMPREG+" WHERE id = ? ";
                HashMap empHm = db.selectOneRecord(sql, new String[]{empId});

                sql = "SELECT * FROM "+Constants.TABLE.POT_EMP_EMPREG_TEMP+" WHERE id = ? ";
                HashMap duplHm = db.selectOneRecord(sql, new String[]{dupEmpId});

                //(PUSHED)
                KeywordDictionary kd = new KeywordDictionary(db);                
                empHm = kd.removeBasicKeys((HashMap) empHm, db); 
                duplHm = kd.removeBasicKeys((HashMap) duplHm, db);   

                //merge data           
                HashMap difference = kd.getDifferenceValues(empHm, duplHm);

                if(difference!=null){
                    LogUtil.info(this.getClassName(), "changes "+ difference.toString());
                    //put the diff into another table for DMP to scrutiny...
                    kd.recordChanges(id, empHm, duplHm);
                }   
            break;
            case Constants.STATUS.EGMNT.ACKNOWLEDGED:
                //merge all approved/new
                sql = "SELECT * FROM "+Constants.TABLE.AUDIT_SUB+" WHERE c_fk = ? "
                        + " AND c_status = ?";
                ArrayList<HashMap<String, String>> chgeHm = db.select(sql, 
                                    new String[]{
                                        id,
                                        Constants.STATUS.EGMNT_CHANGE_STATUS.NEW
                                    });

                for(HashMap hm:chgeHm){
                    String newValue = hm.get("c_curr_value").toString();
                    String fieldName = hm.get("c_field_name").toString();
                    String chgeId = hm.get("id").toString();

                    if(!fieldName.isEmpty()){
                        sql = "UPDATE "+Constants.TABLE.EMPREG+" SET "+fieldName+"=? WHERE id = ?";
                        i = db.update(sql, new String[]{newValue}, new String[]{empId});
                        LogUtil.info(this.getClassName(), "Updating empreg.. "+
                                id+" - field "+fieldName+" -> "+newValue+" result: "+Integer.toString(i));
                    }                

                    sql = "UPDATE "+Constants.TABLE.AUDIT_SUB+" SET c_status = ? WHERE id = ?";
                    db.update(sql, new String[]{Constants.STATUS.EGMNT_CHANGE_STATUS.APPROVED},
                            new String[]{chgeId});
                }

                //update pot emp
                sql = "UPDATE "+Constants.TABLE.POT_EMP+" SET c_status = ? WHERE id = ?";
                db.update(sql, new String[]{Constants.STATUS.POT_EMP.TRUE}, 
                        new String[]{potEmpId});
                sql = "UPDATE "+Constants.TABLE.EMPREG+" SET c_last_move = ? WHERE id = ?";
                db.update(sql, new String[]{Constants.LAST_MOVEMENT.TRUE_POTENTIAL}, 
                        new String[]{empId});
            break;
            case Constants.STATUS.EGMNT.REJECTED:
                sql = "UPDATE "+Constants.TABLE.AUDIT_SUB+" SET c_status = ? WHERE c_fk = ? AND c_status = ?";
                db.update(sql, new String[]{Constants.STATUS.EGMNT_CHANGE_STATUS.REJECTED},
                        new String[]{id,
                        Constants.STATUS.EGMNT_CHANGE_STATUS.NEW}); 
            break;
            case Constants.STATUS.EGMNT.QUERY:
                sql = "UPDATE "+Constants.TABLE.AUDIT_SUB+" SET c_status = ? WHERE c_fk = ? AND c_status = ?";
                db.update(sql, new String[]{Constants.STATUS.EGMNT_CHANGE_STATUS.QUERY},
                        new String[]{id,
                        Constants.STATUS.EGMNT_CHANGE_STATUS.NEW}); 
            break;            
        }
        updateCount++;
    }
    
    private void updateChgeDataStatus(DBHandler db, String id, String status) {
        
    }

//    private HashMap getChangedKeyValues(HashMap empHm, HashMap duplHm) {
//        Iterator bKeyIterator = duplHm.keySet().iterator();
//        Object key;
//        Object value;
//        HashMap difference = new HashMap();
//
//        while (bKeyIterator.hasNext()) {
//            key = bKeyIterator.next();
//            
//            if(key.equals("id")){
//                continue;
//            }
//            
//            if (empHm.containsKey(key)) {
//                value = duplHm.get(key);
//                difference.put(key, value);
//            }
//        }
//        
//        return difference;
//    }

//    private void mergeChanges(String empId, HashMap difference) {
//        if(difference!=null){
//
//            Set set = difference.entrySet();
//            Iterator iterator = set.iterator();
//            HashMap chgeHm = new HashMap();
//            
//            while (iterator.hasNext()) {
//                Map.Entry mentry = (Map.Entry) iterator.next();
//
//                String colName = mentry.getKey().toString();
//                String colVal = mentry.getValue().toString();
//                
//                if(colName.isEmpty() || colName.equals("id")){
//                    continue;
//                }
//
//                if(colName.startsWith("c_")){
//                    colName = colName.replaceFirst("c_", "");
//                }
//
//                chgeHm.put(colName, colVal);
//            }
//
//            CommonUtils.saveUpdateForm2("", 
//                    Constants.FORM_ID.EMP_TEMP_MAIN_FORM, empId, chgeHm);
//
////            String query = "INSERT INTO "+Constants.TABLE.POT_EMP_EMPREG_TEMP+" ("+colNames+") "
////                    + "VALUES ("+values+")";
////            int i = db.update(query);
//            LogUtil.info(this.getClassName(), "Change "+chgeHm.toString());
////            temp_empId = CommonUtils.saveUpdateForm2("", Constants.FORM_ID.POTEMP_ENG_TEMPREG_FORM, 
////                                    "", dupHm);
//        }
//    }

    private String createBatch() {
        String batch = CommonUtils.getRefNo("3", Constants.ENV_VAR.EGMNT.BATCH_COUNTER);
        return "E"+batch;
    }

    private void setBatch(DBHandler db, String id, String batch) {
        String sql = "UPDATE "+Constants.TABLE.POT_EMP_ENGAGEMENT+" "
                + "SET c_batch = ?, c_batch_create_dt =? WHERE id = ?";
        int i = db.update(sql, new String[]{batch, 
        CommonUtils.get_DT_CurrentDateTime("YYYY-MM-dd hh:mm:ss")}, new String[]{id});
        
        updateCount+=i;
    }
    
}
