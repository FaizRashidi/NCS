/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.datalistAction;

import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author faizr
 */
public class DeleteSavedApplicationButton extends DataListActionDefault {

    public String getName() {
        return this.getClass().toString();
    }

    public String getVersion() {
        return "1.0";
    }

    public String getDescription() {
        return "To delete user (Used in development) V2"; 
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
            confirm = "Confirm Delete?";
        }
        return confirm;
    }

    public String getLabel() {
        return "HRDC - EMPM - Delete Saved Applications Button"; 
    }

    public String getClassName() {
        return this.getClass().getName();
    }

    public String getPropertyOptions() {
        String json = "[{\n"
                + "    title : '"+getLabel()+"',\n"
                + "    properties : [{\n"
                + "        label : 'Label',\n"
                + "        name : 'label',\n"
                + "        type : 'textfield',\n"
                + "        description : 'Delete User & Record',\n"
                + "        value : 'Delete User & Record'\n"
                + "    }]\n"
                + "}]";
        return json;
    }
    
    private  void msg(String msg){
        LogUtil.info(this.getClassName(), msg);
    }
    
    @Override
    public DataListActionResult executeAction(DataList dataList, String[] rowKeys) {
        DBHandler db = new DBHandler();
        String message = "";
        int deleted = 0;
        
        if(rowKeys.length != 0){
            
            for(String id:rowKeys){
                try{
                    db.openConnection();

                    int i = db.delete(
                            "DELETE FROM "+Constants.TABLE.EMPREG_APPL+" WHERE id = ?",
                            new String[]{id}
                    );
                    
                    if(i==0){
                        i = db.delete(
                            "DELETE FROM "+Constants.TABLE.DEREG+" WHERE id = ?",
                            new String[]{id}
                        );
                    }
                    
                    deleted+=i;
                }catch(Exception e){
                    e.printStackTrace();
                }finally{
                    db.closeConnection();
                }
            }
            message = Integer.toString(deleted)+" record(s) deleted";
        }else{
            message = "No data selected";
        }        
        
        DataListActionResult result = new DataListActionResult();
        result.setType(DataListActionResult.TYPE_REDIRECT);
        result.setUrl("REFERER");
        
        if(!message.isEmpty()){
            result.setMessage(message);
        }
        
        return result;
    }
    
}
