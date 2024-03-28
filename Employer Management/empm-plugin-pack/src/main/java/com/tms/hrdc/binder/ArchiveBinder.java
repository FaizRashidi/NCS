/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.KeywordDictionary;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;

/**
 *
 * @author faizr
 */
public class ArchiveBinder extends WorkflowFormBinder{
    
    @Override
    public String getName() {
        return this.getClass().toString();
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Used in Employer Management to insert data to archive table after data submission";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Archive Binder";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }
    
    @Override
    public String getPropertyOptions() {
       return AppUtil.readPluginResource(getClass().getName(), "/properties/archive_binderX.json", null, true, "message/archive_binderX");
    }
    
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
    
    @Override
    public FormRowSet store(Element element, FormRowSet rows, FormData formData) {
        
        rows = super.store(element, rows, formData);
        LogUtil.info("== EMP-REG","Audit Trail Binder Start "+rows.get(0).getId());
        DBHandler db = new DBHandler();
        
        String logRemarks = "";
        String logStatus  = getPropertyStatus(element, formData);
        String modifiedByName  = getPropertyStatus(element, formData);       
        
        String formId = super.getFormId();
        logStatus = getAuditStatus(formId, rows);
        
        try{
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
            String mainKey = rows.get(0).getId();     
            
//            mainKey = getRegMainKey(db, mainKey);
            
            HashMap<String, String> hm = getMainData(db, mainKey);
            HashMap<String, String> hm_arch = getLatestArchiveData(db, mainKey);
            HashMap hm_clone = new HashMap();       
            HashMap hm_arch_clone = new HashMap();
            ArrayList<HashMap<String, String>> value_chge_list = new ArrayList();
            
            boolean is_data_same_as_prev = false;   
            
            if(hm_arch != null && !hm_arch.isEmpty()){
                
                KeywordDictionary kd = new KeywordDictionary(db);
                
                hm_clone = removeBasicKeys((HashMap) hm.clone(), db, kd);       
                hm_arch_clone = removeBasicKeys((HashMap) hm_arch.clone(), db, kd);  
                
                is_data_same_as_prev = hm_clone.equals(hm_arch_clone); 
            }else{
                //create
            }
            
            LogUtil.info("EMPM AUDIT DEBUG", "Is data same as prev? "+Boolean.toString(is_data_same_as_prev));
            
            if(!is_data_same_as_prev){                 
                value_chge_list = buildAuditHm(hm_clone, hm_arch_clone);
            }
            
            String modifiedBy = modifiedByName.isEmpty()?modifiedByName:hm.get("modifiedBy");
            modifiedByName = modifiedByName.isEmpty()?modifiedByName:hm.get("modifiedByName");
            
            String aud_id = new AuditTrailUtil().insertAuditTrail(db, mainKey, 
                    hm_arch_clone, hm_clone, modifiedBy, logStatus, logRemarks);
            
            if(hm_arch != null && !is_data_same_as_prev){ 
                HashMap refined_arch_hm = buildArchiveData(mainKey, hm, hm_arch, aud_id, false); 
                
                LogUtil.info("EMPM AUDIT DEBUG", "Inserting archive data ");
                
//                insertArchiveData(db, (String) refined_arch_hm.get(UUID), 
//                        (String[])refined_arch_hm.get(COL), 
//                        (String[])refined_arch_hm.get(VAL));
                
            }else if(hm_arch == null){
                HashMap refined_arch_hm = buildArchiveData(mainKey, hm, hm_arch, aud_id, true); 
                
                LogUtil.info("EMPM AUDIT DEBUG", "Inserting main archive data ");
                
//                insertArchiveData(db, (String) refined_arch_hm.get(UUID), 
//                        (String[])refined_arch_hm.get(COL), 
//                        (String[])refined_arch_hm.get(VAL));
            }
            
        }catch(SQLException e){
            e.printStackTrace();
        }finally{
            LogUtil.info("EMPM AUDIT DEBUG", "Closing connection");
            db.closeConnection();
            LogUtil.info("EMPM AUDIT DEBUG", "Connection closed");
        }
        
//        LogUtil.info("== EMP-REG","Audit Trail Binder Ends "+rows.get(0).getId());
        
        return rows;
    }
    
    public String getElementValue(String element, Form form, FormData formData) {
        try {
            Element e = FormUtil.findElement(element, form, formData);
            return FormUtil.getElementPropertyValue(e, formData);
        } catch (NullPointerException e) {
            //
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        }
        return "";
    }
    
    private HashMap buildArchiveData(String mainKey, HashMap<String, String> hm, 
                                        HashMap<String, String> hm_arch, String audit_id, 
                                        boolean is_main_ref) {
        
        String dateCreated = "";
        String uuid = UuidGenerator.getInstance().getUuid().substring(0, 5);
        uuid = "archive-"+uuid+"-"+mainKey;
        
        if(hm_arch == null){
            dateCreated = hm.get("dateCreated").toString();
        }        
        
        hm.put("c_audit_id", audit_id); //to connect with archive
        hm.remove("dateCreated");
        hm.remove("dateModified");
        
        String[] col = new String[hm.size()];
        String[] val = new String[hm.size()];               
        
        int count = 0;
        for (Map.Entry<String, String> set :
             hm.entrySet()) {
            
            String key = set.getKey();
            String value = set.getValue();
            
            if(key.equals("id")){
                continue;
            }            
            if(key.equals("c_arc_fk")){
                value = mainKey;
            }             
            if(StringUtils.isBlank(key)){
                continue;
            }            
            col[count] = key;
            val[count] = value;
            
            count++;
        }
        
        if(is_main_ref){
            col[count] = "c_is_main_ref";
            val[count] = "true";
        }        
        
        for(int x=0;x<col.length;x++){  
//           LogUtil.info("EMPM AUDIT DEBUG", "f - "+col[x]+", val - "+val[x]);
        }
        HashMap returnData = new HashMap();
        
        returnData.put(UUID, uuid);
        returnData.put(COL, col);
        returnData.put(VAL, val);
        
        return returnData;
    }

    private HashMap getMainData(DBHandler db, String mainKey) {
        String query = "SELECT * FROM app_fd_empm_reg WHERE id = ?";
//                + " and c_reg_status != ?";
        String[] cond = {mainKey};
        
        return db.selectOneRecord(query, cond);
    }
    
    private HashMap getLatestArchiveData(DBHandler db, String mainKey) {
        String query = "SELECT * FROM app_fd_empm_emp_arc WHERE c_arc_fk = ?"
                + " order by dateCreated DESC LIMIT 1";
        String[] cond = {mainKey};
        
        return db.selectOneRecord(query, cond);
    }

    private void insertArchiveData(DBHandler db, String id, String[] col, String[] val) {
        
        HashMap data = new HashMap();
        for(int x=0;x<col.length;x++){  
            String field = "";
            if(!StringUtils.isBlank(col[x]) && col[x].startsWith("c_")){
                field = col[x].substring(2, col[x].length());
            }else if(!StringUtils.isBlank(col[x])){
                field = col[x];
            }else{
                continue;
            }
            
            data.put(field, val[x]);
        }
        CommonUtils.saveUpdateForm("","archive_form", id, data);
    }    
    
    public HashMap removeBasicKeys(HashMap hm, DBHandler db, KeywordDictionary kd){
        
        HashMap newHm = (HashMap) hm.clone();     
        
        Set set = hm.entrySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {            
            Map.Entry mentry = (Map.Entry) iterator.next();
            
            if(!kd.getEmplrMapList().containsKey(mentry.getKey())){
                newHm.remove(mentry.getKey());
            }     
        }
        
        return newHm;
    }

    private ArrayList<HashMap<String, String>> buildAuditHm(HashMap hm_clone, HashMap hm_arch_clone) {
        
        ArrayList<HashMap<String, String>> audit_list = new ArrayList();
        
        Set set = hm_clone.entrySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            Map.Entry mentry = (Map.Entry) iterator.next();
            
            String prev_value = hm_arch_clone.get(mentry.getKey()) == null?"":hm_arch_clone.get(mentry.getKey()).toString();
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
        
        return audit_list;
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

    private String getAuditStatus(String formId, FormRowSet rows) {
        
        String status = "";
        
        switch(formId){
            case Constants.FORM_ID.APPLICATION:
                status = "Submitted FORM 1 Application";
            break;
            
            case Constants.FORM_ID.VERIFICATION:
                status = rows.get(0).getProperty("verify", "Verified");
            break;
            
            case Constants.FORM_ID.APPROVAL:
                status = rows.get(0).getProperty("approval", "Verified");
            break;
            
            case Constants.FORM_ID.QUERY:
                status = "Query Responded";
            break;
            
            case Constants.FORM_ID.DATA_UPDATE:
                status = "Data Update";
            break;
            
            default:
                status = "Error upon submit ["+formId+"]";
        }
        
        return status;
    }

    private String getPropertyStatus(Element element, FormData formData) {
        String logStatus = "";
        Form form = FormUtil.findRootForm(element);
        
        String statusFormField = getPropertyString("statusFormField");
        String statusFixed = getPropertyString("statusFixed");
        
        if (!"".equals(statusFormField)) {
            logStatus = getElementValue(statusFormField, form, formData);
        } else {
            logStatus = statusFixed;
        }
        
        String saveAsDraft = formData.getRequestParameter("saveAsDraft");
        if ((saveAsDraft != null) && (!"".equals(saveAsDraft))) {
            logStatus = "Saved As Draft";
        }
        
        return logStatus;
    }
    
}
