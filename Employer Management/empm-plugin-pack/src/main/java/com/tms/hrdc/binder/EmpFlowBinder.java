/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.util.HashMap;
import java.util.Map;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.workflow.model.WorkflowAssignment;

/**
 *
 * @author faizr
 */
public class EmpFlowBinder extends WorkflowFormBinder{
    
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
        return "Used in main flow's loadbinder";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Main Flow Binder";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
       return "";
    }
    
    private void pm(String msg){
        LogUtil.info(this.getClassName(), msg);
    }
    
    public FormRowSet load(Element element, String id, FormData formData) {
        
        FormRowSet rows = new FormRowSet();

        String pKey = formData.getPrimaryKeyValue();
        
        String formId = super.getFormId();
        String table = super.getTableName().startsWith("app_fd_")?super.getTableName():"app_fd_"+super.getTableName();
        DBHandler db = new DBHandler();
        
        switch(formId){
            case "": // -- > form ID of reg
                //DO UPDATE BRANCH DATA
                // use msg() function to log (lie 55)
                updateBranchMycoid(db, pKey);
            break;
            
            default:
                updateViewStatus(db, formId, table, pKey);
        }
                
        return rows;
    }
    
    @Override
    public FormRowSet store(Element element, FormRowSet rowSet, FormData formData) {

        rowSet = super.store(element, rowSet, formData);
        
        return rowSet;
    }
    
    private void updateViewStatus(DBHandler db, String formId, String table, String pKey){
        
        try{
            db.openConnection();
            
            int upd = db.update("UPDATE "+table+" SET c_is_viewed = ? WHERE id = ?",
                    new String[]{Constants.STATUS.VIEW_STATUS.PENDING},
                    new String[]{pKey});
            
        }catch(Exception e){
            e.printStackTrace();
            HashMap hm = new HashMap();
            hm.put("is_viewed", "PENDING");
            CommonUtils.saveUpdateForm("", formId, pKey, hm);            
        }finally{
            db.closeConnection();
        }
        
    }
    
    private void updateBranchMycoid(DBHandler db, String pKey){
        try{
            db.openConnection();
            
            //Refer function in RefNumGenerator Class Line 136
            
        }catch(Exception e){
            e.printStackTrace();   
        }finally{
            db.closeConnection();
        }
    }
}
