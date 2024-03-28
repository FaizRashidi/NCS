package com.tms.hrdc.datalistAction;

import com.tms.hrdc.binder.PotEmpExcelBinder;
import com.tms.hrdc.dao.CurrentUser;
import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.util.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.commons.util.LogUtil;
import org.joget.workflow.model.WorkflowProcess;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.joget.workflow.util.WorkflowUtil;

/**
 *
 * @author faizr
 */
public class PotEmpStateChangerButton extends DataListActionDefault {

    public String getName() {
        return this.getClass().toString(); 
    }

    public String getVersion() {
        return "1.0";
    }

    public String getDescription() {
        return "Potential Employer Action Button"; 
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
        return "HRDC - EMPM - PE Multi-Function Button";
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
                + "                {value: 'add_to_PE', label : 'Add to Potential Employer'}," 
                + "                {value: 'add_to_truePE', label : 'Add to True Potential Employer'}," 
                + "                {value: 'add_to_dirtyList', label : 'Add to Dirty List'}," 
                + "                {value: 'flush_out', label : 'Flush Out'}," 
                + "                {value: 'flush_out_uploaded_peBatch', label : 'Flush Out NewlyUploaded PE'}," 
                + "                {value: 'write_off', label : 'Write-Off'}," 
                + "                {value: 'sub_batch', label : 'Sub Batch'}," 
                + "                {value: 'complaint', label : 'Complaint'}," 
                + "                {value: 'cksp', label : 'CKSP'}" 
                + "            ]\n" 
                + "        }]\n"
                + "}]";
        return json;
    }
    
    int count = 0,updateCount = 0, failedCount = 0, existCount = 0;;
    String additional_msg = "";
    String CURRENTUSERNAME = "";

    // new to pe counts
    int pe_inserted = 0, pe_overwritten = 0, pe_failed = 0;
    String overwritten = "", failed = "";
    
    int itemCount = 0;
    int batchCount = 0;
    
    private void msg(String msg){
        LogUtil.info(this.getClass().toString(), msg);
    }
    
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
        String writeOffId = "";
        String engId = "";
        
        msg("Type "+type);
        
        CURRENTUSERNAME = new CurrentUser().getFullName();
        
        for(String id:rowKeys){
//            LogUtil.info("rowkye",id);
            ArrayList<HashMap<String, String>> pList = isIDBatch(db, id);
            String peId = "";
            String empId = "";
//            LogUtil.info("rowkye 1",pList.toString());
            // for adding to PE
            
//            LogUtil.info("rowkye 2",pList.toString());
            
            switch(type){
                case "add_to_PE":                    
                    
                    String batch = "";
                    String uplFk = "";
                    
                    pList = getAllUploadedSuccessItem(db, id);
                    
                    for(HashMap pe:pList){
                        batch = pe.get("c_batch")==null?"":pe.get("c_batch").toString();
                        empId = pe.get("c_emp_fk")==null?"":pe.get("c_emp_fk").toString();
                        uplFk = pe.get("id")==null?"":pe.get("id").toString();
                        
                        handleImportToPE(db, uplFk, batch, empId);
                    }                    
//                    message = Integer.toString(updateCount)+" accepted as potential employers ";
                    message = "Succesully submitted to PE: "+ Integer.toString(pe_inserted)+". "+
                            "Existing PE Overwritten: "+Integer.toString(pe_overwritten)+". "+
                            "Failedn: "+Integer.toString(pe_overwritten);
                break;
                case "add_to_truePE":
                    for(HashMap pe:pList){
                        peId = pe.get("id")==null?"":pe.get("id").toString();
                        empId = pe.get("c_emp_fk")==null?"":pe.get("c_emp_fk").toString();
                        handleImportToTrue(db, peId, empId);   
                    }
                    message = "Passed criteria: "+Integer.toString(updateCount)+", "+
                                "Not passed criteria "+Integer.toString(failedCount);
                break;
                case "add_to_dirtyList":
                    for(HashMap pe:pList){
                        peId = pe.get("id")==null?"":pe.get("id").toString();
                        empId = pe.get("c_emp_fk")==null?"":pe.get("c_emp_fk").toString();
                        handleImportToDirty(db, peId, empId);   
                    }
                    message = Integer.toString(updateCount)+" sent to dirty list ";                    
                break;                
                case "flush_out":
                    for(HashMap pe:pList){
                        peId = pe.get("id")==null?"":pe.get("id").toString();
                        empId = pe.get("c_emp_fk")==null?"":pe.get("c_emp_fk").toString();
                        handleFlushOut(db, peId, empId);   
                    }
                    message = Integer.toString(updateCount)+" records flushed out ";
                break;         
                case "flush_out_uploaded_peBatch":
                    
                    db.update(
                            "UPDATE "+Constants.TABLE.POT_EMP_UPLOAD+" SET c_upl_status = ? WHERE id = ?",
                            new String[]{PotEmpExcelBinder.MSG_DELETED},
                            new String[]{id}
                    );
                                        
                    pList = getAllUploadedItem(db, id);
                    
                    for(HashMap pe:pList){
                        String uplItemId = pe.get("id")==null?"":pe.get("id").toString();
                        empId = pe.get("c_emp_fk")==null?"":pe.get("c_emp_fk").toString();
                        handleFlushOutUploadedItem(db, uplItemId, empId);        
                        
                        db.update(
                                "UPDATE "+Constants.TABLE.POT_EMP_UPLOAD+" SET c_total_row = c_total_row-1 WHERE id = '"+id+"'"
                        );
                    }
                    
                    db.delete(
                            "DELETE FROM "+Constants.TABLE.POT_EMP_UPLOAD+" WHERE id = ?",
                            new String[]{id}
                    );
                    
                    batchCount++;
                    
                    message = Integer.toString(itemCount)+" records flushed out \"\n\" "
                            + " from "+ Integer.toString(batchCount)+
                            " batches ";
                break;      
                case "write_off":                    
                    for(HashMap pe:pList){
                        peId = pe.get("id")==null?"":pe.get("id").toString();
                        empId = pe.get("c_emp_fk")==null?"":pe.get("c_emp_fk").toString();                        
                        writeOffId = getWriteOffId(db);
                        handleWriteOff(db, peId, writeOffId);    
                        startWriteOffProcess(writeOffId);
                    }                    
                    message = Integer.toString(updateCount)+" records pending for write-off ";
                break;
                case "complaint":
                    for(HashMap pe:pList){
                        peId = pe.get("id")==null?"":pe.get("id").toString();
                        empId = pe.get("c_emp_fk")==null?"":pe.get("c_emp_fk").toString();
                        sendToEnforcement(db, peId, empId);
                    }
                    message = Integer.toString(updateCount)+" sent to Enforcement ";
                break;
            }            
        }
        
        // message building 
        if(type.equals("sub_batch")){
            message = handleSubBatch(db, rowKeys);
            
            message = Integer.toString(count)+" records grouped in sub-batch "+message;
        }
        
        db.closeConnection();
        
        DataListActionResult result = new DataListActionResult();
        result.setType(DataListActionResult.TYPE_REDIRECT);
        result.setUrl("REFERER");
        
        if(!message.isEmpty()){
            result.setMessage(message);
        }
        
        return result;
    }

    private void handleImportToPE(DBHandler db, String uplId, String batch, String tempId) {
        
        int countIn = 0;
        String query = "";
        ArrayList list = new ArrayList();
        boolean isMycoidExistAsPE = false;
        boolean override = false;
        boolean insert = false;

        // check if mycoid exist as potential
        // exists -> getreg ID, override with NEW & use new batch. Status is Potential
        // !exists -> create New. Status Potential
        // delete data from pe_upl_data & update

        HashMap tempHm = db.selectOneRecord(
                "SELECT * FROM "+Constants.TABLE.EMPREG_TEMP+" WHERE id = ?",
                new String[]{tempId}
        );

        if(tempHm==null){
            return;
        }

        String tempMycoid = tempHm.getOrDefault("c_mycoid","").toString();
        String tempTotalEmplCount = tempHm.getOrDefault("c_total_empl","").toString();

        HashMap existingPotentialMycoidHm = db.selectOneRecord(
                "select \n" +
                "p.c_batch, r.id as existingEmpId,  p.id as existingPotEmpId, r.c_total_empl \n" +
                "FROM app_fd_empm_pe_potEmp p\n" +
                "INNER JOIN app_fd_empm_reg r ON r.id = p.c_emp_fk\n" +
                "WHERE p.c_status = 'POTENTIAL'\n" +
                "and r.c_mycoid = ?  ",
                new String[]{tempMycoid}
        );


        String existingEmpId = "";
        String existingPotEmpId = "";
        String existingPotTotalEmplCount = "";

        if(existingPotentialMycoidHm!=null){
            isMycoidExistAsPE = true;
            existingEmpId = existingPotentialMycoidHm.getOrDefault("existingEmpId","").toString();
            existingPotEmpId = existingPotentialMycoidHm.getOrDefault("existingPotEmpId","").toString();
            existingPotTotalEmplCount = existingPotentialMycoidHm.getOrDefault("c_total_empl","0").toString();
        }else{
            insert = true;
        }

        int existingPotTotalEmplCount_int = CommonUtils.strToInt(existingPotTotalEmplCount);
        int tempTotalEmplCount_int = CommonUtils.strToInt(tempTotalEmplCount);

        if(isMycoidExistAsPE && existingPotTotalEmplCount_int < tempTotalEmplCount_int){
             override = true;
        }

        String empId = "";
        String peId = "";

        if( (isMycoidExistAsPE && override) || insert){
            empId = EmpUtil.createOrOverrideEmpFromTemplate(db, tempId, existingEmpId);
            int i = db.update(
                    "UPDATE "+Constants.TABLE.AUDIT+" SET c_fk = ? WHERE c_fk = ?",
                    new String[]{empId},
                    new String[]{tempId}
            );
//        LogUtil.info(this.getClassName(),"UPDATE AUDIT "+Integer.toString(i));
            i = db.update(
                    "UPDATE "+Constants.TABLE.EMP_OTHER_CONTACT_DETAILS+" SET c_fk = ? WHERE c_fk = ?",
                    new String[]{empId},
                    new String[]{tempId}
            );
//        LogUtil.info(this.getClassName(),"UPDATE OTHER CONTACT DETAILS "+Integer.toString(i));

            // create potential employer data
            HashMap newPe = new HashMap();
            newPe.put("emp_fk", empId);
            newPe.put("batch", batch);
            newPe.put("status", Constants.STATUS.POT_EMP.POTENTIAL);
            newPe.put("is_registered", "No");

            peId = CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, "pot_emp_data", existingPotEmpId, newPe);

            //delete regtemp, pe_upl_data
            db.delete(
                    "DELETE FROM "+Constants.TABLE.POT_EMP_UPLOAD_DATA+" WHERE id = ?",
                    new String[]{uplId}
            );
            db.delete(
                    "DELETE FROM "+Constants.TABLE.EMPREG_TEMP+" WHERE id = ?",
                    new String[]{tempId}
            );

            query = "UPDATE app_fd_empm_pe_file_upl SET " +
                    "c_pe_count = c_pe_count+1, " +
                    "c_total_success = c_total_success-1 " +
                    "WHERE id = ? ";
            db.update(query, new String[]{},new String[]{batch});
        }else{
            pe_failed++;
        }

        if(insert){
            pe_inserted++;
        }

        if(override){
            pe_overwritten++;
        }
    }

    private boolean alreadyInPE(DBHandler db, String batch, String empFk) {
        String query = "SELECT * FROM app_fd_empm_pe_potEmp WHERE "
                + "c_emp_fk = ? and c_batch = ?";
        HashMap qHm = db.selectOneRecord(query, new String[]{empFk, batch});
        
        if(qHm != null){
            return true;
        }
        
        return false;
    }

    //To check if exits in PE
    private boolean checkMycoidExistAsPE(DBHandler db, String batch, String empFk) {
        String query = "SELECT * FROM app_fd_empm_pe_potEmp WHERE "
                + "c_emp_fk = ? and c_batch = ?";
        HashMap qHm = db.selectOneRecord(query, new String[]{empFk, batch});

        if(qHm != null){
            return true;
        }

        return false;
    }
    
    public void handleFlushOut(DBHandler db, String peId, String empId){
        
        String batchId = db.selectOneValueFromId(Constants.TABLE.POT_EMP,
                "c_batch", peId);
        
        String sql = "DELETE FROM app_fd_empm_pe_potEmp WHERE id = ?";
        updateCount += db.delete(sql, new String[]{peId});

        sql = "DELETE FROM app_fd_empm_reg WHERE id = ?";
        db.delete(sql, new String[]{empId});
        
        sql = "SELECT * FROM "+Constants.TABLE.POT_EMP+" "
                + "WHERE c_batch = ?";        
        //check if batch is empty
        ArrayList bList = db.select(sql, new String[]{batchId});
        if(bList.size()==0){
            sql = "DELETE FROM "+Constants.TABLE.POT_EMP_UPLOAD+" WHERE id = ?";
            db.delete(sql, new String[]{batchId});
        }
    }
    
    
    private String handleSubBatch(DBHandler db, String[] ids) {
        
        String refno = getRefPrefix(db)
                        +"/Sub/"
                        +CommonUtils.get_DT_CurrentDateTime("YYYY")
                        +"/"
                        +CommonUtils.getRefNo("6", 
                                Constants.ENV_VAR.POT_EMP.WRITE_OFF_COUNTER);
        
        HashMap subBatchHm = new HashMap();
        subBatchHm.put("upl_status", "SubBatch");
        subBatchHm.put("batch", refno);
        
        String batchId = CommonUtils.saveUpdateForm2("", Constants.FORM_ID.POTEMP_FILE_UPL, "", subBatchHm);
        
        for(String id:ids){
            String query = "UPDATE app_fd_empm_pe_potEmp SET c_batch = ?  where id = ?";        
            count += db.update(query, new String[]{batchId }, new String[]{id});
        }
        
        return refno;
    }
    
    private String getRefPrefix(DBHandler db) {
        String prefix = "";
        HashMap hm = db.selectOneRecord("SELECT c_pe_batch_refCode FROM app_fd_empm_reg_stp WHERE id = ?", 
                new String[]{Constants.DATA_ID.MAIN_SETUP_ID});

        prefix = hm!=null?hm.get("c_pe_batch_refCode").toString():"";
        
        return prefix;
    }
    
    private static String getWORefPrefix(DBHandler db) {
        String prefix = "";
        HashMap hm = db.selectOneRecord("SELECT c_pe_wo_refCode FROM app_fd_empm_reg_stp WHERE id = ?", 
                new String[]{Constants.DATA_ID.MAIN_SETUP_ID});

        prefix = hm!=null?hm.get("c_pe_wo_refCode").toString():"";
        
        return prefix;
    }

    public static String getWriteOffId(DBHandler db) {
        
        String refno = getWORefPrefix(db)
                        +"/"
                        +CommonUtils.get_DT_CurrentDateTime("YYYY")
                        +"/"
                        +CommonUtils.getRefNo("6", 
                                Constants.ENV_VAR.POT_EMP.WRITE_OFF_COUNTER);
        
        HashMap hm = new HashMap();        
        hm.put("status", "Pending Write-Off Approval");                
        hm.put("ref_no", refno);                
        return CommonUtils.saveUpdateForm2("", Constants.FORM_ID.POTEMP_WRITEOFF, "", hm);
    }
    
    private void handleWriteOff(DBHandler db, String potemp_id, String writeOffId){
        String sql = "UPDATE "+Constants.TABLE.POT_EMP 
                + " SET c_status = ?, c_writeoff_fk = ? "
                + " WHERE id = ? ";
        
        updateCount += db.update(sql, 
                new String[]{Constants.STATUS.POT_EMP.PENDING_WRITE_OFF, 
                                writeOffId}, 
                new String[]{potemp_id});
        
        HashMap audHm = new HashMap();
        audHm.put("status", "Submitted for Write-Off");
        audHm.put("fk", writeOffId);
        audHm.put("createdByName", WorkflowUtil.getCurrentUserFullName());
        
        CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                                    Constants.FORM_ID.AUDIT_TRAIL, 
                                                    "", audHm);
    }
    
//    private void processPE(DBHandler db, String peId, String btnType) {
//        ArrayList<HashMap<String, String>> pList = isIDBatch(db, peId);
//        LogUtil.info(this.getClassName(), pList.toString());
//        String empId = "";
//        
//        for(HashMap pe:pList){
//            peId = pe.get("id")==null?"":pe.get("id").toString();
//            empId = pe.get("c_emp_fk")==null?"":pe.get("c_emp_fk").toString();
//            setData(db, peId,empId, btnType);
//        }
//    }
    
    private ArrayList<HashMap<String, String>> isIDBatch(DBHandler db, String id){
        String sql = "SELECT * FROM "+Constants.TABLE.POT_EMP+" WHERE c_batch = ?";
        ArrayList<HashMap<String, String>> list = db.select(sql, new String[]{id});
        
        if(list.isEmpty()){
            sql = "SELECT * FROM "+Constants.TABLE.POT_EMP+" WHERE id = ?";
            list = db.select(sql, new String[]{id});
        }
        
        return list;
    }
    
//    private void setData(DBHandler db, String peId, String empId, String btnType) {
//        switch(btnType){
//            case "complaint":
//                sendToEnforcement(db, peId, empId);
//            break;
//            case "add_to_truePE":
//                handleImportToTrue(db, peId, empId);                
//            break;
//            case "add_to_dirtyList":
//                handleImportToDirty(db, peId, empId);
//            break;
//        }
//        
//    }
    
    private void sendToEnforcement(DBHandler db, String peId, String empId) {
        String sql = "UPDATE "+Constants.TABLE.POT_EMP+" SET c_status = ? WHERE id = ?";
        int i = db.update(sql, new String[]{Constants.STATUS.POT_EMP.ENFORCEMENT},
                new String[]{peId});
        
        sql = "UPDATE "+Constants.TABLE.EMPREG+" SET c_last_move = ? WHERE id = ?";
        i = db.update(sql, 
                new String[]{
                    Constants.LAST_MOVEMENT.COMPLAINT_TO_ENFORCEMENT
                },
                new String[]{empId});
        
        updateCount++;
    }
    
    private void handleImportToDirty(DBHandler db, String peId, String empId) {
        
        String sql = "UPDATE "+Constants.TABLE.POT_EMP+" SET c_status = ? WHERE id = ?";
        int i = db.update(sql, new String[]{Constants.STATUS.POT_EMP.DIRTY},
                new String[]{peId});
        
        sql = "UPDATE "+Constants.TABLE.EMPREG+" SET c_data_status = ?,c_last_move = ? WHERE id = ?";
        i = db.update(sql, 
                new String[]{
                    Constants.STATUS.EMP.DIRTY_LISTED,
                    Constants.LAST_MOVEMENT.DIRTY_LIST
                },
                new String[]{empId});
        
        if(i>0){ 
            updateCount++; 
        }else{
            failedCount++;
        }
    }
    
    private void handleImportToTrue(DBHandler db, String peId, String empId) {
        
        EmpmObj emp = new EmpmObj(db, EmpmObj.BY_ID, empId);
        CriteriaUtil cut = new CriteriaUtil(db);
        
        String query = "";
        int countIn = 0;
        
        if(cut.isPassCriteria(emp)){
            
            query = "UPDATE app_fd_empm_pe_potEmp SET c_status = ? WHERE id = ?";
            countIn = db.update(query, 
                        new String[]{Constants.STATUS.POT_EMP.TRUE},
                        new String[]{peId});

            query = "UPDATE app_fd_empm_reg SET c_data_status= ?, c_last_move = ? WHERE id = ? ";
            countIn = db.update(query, 
                        new String[]{Constants.STATUS.EMP.TRUE_POTENTIAL_EMPLOYER,
                        Constants.LAST_MOVEMENT.TRUE_POTENTIAL},
                        new String[]{empId});
            updateCount+=countIn;
        }else{   
            
            query = "UPDATE app_fd_empm_pe_potEmp SET c_status = ?, c_dismissal_reason = '"+cut.getMessage()+"' WHERE id = ?";
            countIn = db.update(query, 
                        new String[]{Constants.STATUS.POT_EMP.DISMISS},
                        new String[]{peId});

            query = "UPDATE app_fd_empm_reg SET c_data_status= ?, c_last_move=? WHERE id = ? ";
            countIn = db.update(query, 
                                    new String[]{
                                        Constants.STATUS.EMP.POTENTIAL_EMPLOYER,
                                        Constants.LAST_MOVEMENT.POTENTIAL
                                    },
                                    new String[]{empId}
                                );
            failedCount+=countIn;
        }       
    }

    private void startWriteOffProcess(String writeOffId) {
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");

        Long appVersion = appService.getPublishedVersion(Constants.APP_ID.EMPM);
        //get process
        WorkflowProcess process = appService.getWorkflowProcessForApp(Constants.APP_ID.EMPM, 
                appVersion.toString(), "pe_write_off");

        WorkflowUserManager wum = (WorkflowUserManager) AppUtil.getApplicationContext().getBean("workflowUserManager");
        String username = wum.getCurrentUsername();
        //start process
        workflowManager.processStart(process.getId(), null, null, username, writeOffId, false);
        
    }
    
    // VV UPLOADED TO PE --------------------------------------------------

//    private ArrayList<HashMap<String, String>> isIdUplBatch(DBHandler db, String id) {
//
//        String query = "SELECT * from app_fd_empm_pe_upl_data u where u.c_batch = ? "
//                        + "AND c_status = 'SUCCESS' "
//                        + "AND (c_isPotEmp is null OR c_isPotEmp='false')";
//        ArrayList<HashMap<String, String>> qList = db.select(query, new String[]{id});
//        
//        if(qList.isEmpty()){
//            LogUtil.info("not batch id",id);
//            query = "SELECT * from app_fd_empm_pe_upl_data u where id = ? "
//                        + "AND c_status = 'SUCCESS' "
//                        + "AND (c_isPotEmp is null OR c_isPotEmp='false')";
//            
//            qList = db.select(query, new String[]{id});
//        }else{
//            LogUtil.info("batch id",id);
//        }
//        
//        return qList;
//    }
    
    private ArrayList<HashMap<String, String>> getAllUploadedSuccessItem(DBHandler db, String id) {

        String query = "SELECT * from app_fd_empm_pe_upl_data u where u.c_batch = ? "
                        + "AND c_status = 'SUCCESS' "
                        + "AND (c_isPotEmp is null OR c_isPotEmp='false')";
        ArrayList<HashMap<String, String>> qList = db.select(query, new String[]{id});
        
        if(qList.isEmpty()){
            LogUtil.info("not batch id",id);
            query = "SELECT * from app_fd_empm_pe_upl_data u where id = ? "
                        + "AND c_status = 'SUCCESS' "
                        + "AND (c_isPotEmp is null OR c_isPotEmp='false')";
            
            qList = db.select(query, new String[]{id});
        }else{
            LogUtil.info("batch id",id);
        }
        
        return qList;
    }
    
    // VV FLUSH OUT UPLOADED BATCH --------------------------------------------------

    private ArrayList<HashMap<String, String>> getAllUploadedItem(DBHandler db, String id) {
        ArrayList<HashMap<String, String>> pEmpl  = db.select(
                "SELECT * FROM app_fd_empm_pe_upl_data p WHERE c_batch = ?",
                new String[]{id}
        );
        
        db.update(
                "UPDATE "+Constants.TABLE.EMPREG_TEMP+" r "
                        + "JOIN "+Constants.TABLE.POT_EMP_UPLOAD_DATA+" p ON p.c_emp_fk = r.id "
                        + "SET r.c_mycoid = CONCAT(r.c_mycoid,'_DELETED'),r.c_comp_name = CONCAT(r.c_comp_name,'_DELETED') "
                        + "WHERE p.c_batch = '"+id+"'"
        );
        
        return pEmpl;
    }

    private void handleFlushOutUploadedItem(DBHandler db, String uplItemId, String empId) {
        
        int del = db.delete(
            "DELETE FROM "+Constants.TABLE.POT_EMP_UPLOAD_DATA+" WHERE id = ? ",
            new String[]{uplItemId}
        );
        if(del>0){
            del = db.delete(
                "DELETE FROM "+Constants.TABLE.POT_EMP_EMPREG_TEMP+" WHERE id = ? ",
                new String[]{empId}
            );
        }        
        itemCount++;
    }
    
    // --------------------------------------------------------------------------
    
}
