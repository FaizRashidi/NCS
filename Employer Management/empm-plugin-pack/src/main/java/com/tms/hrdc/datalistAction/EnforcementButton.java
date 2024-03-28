/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.datalistAction;

import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;

/**
 *
 * @author faizr
 */
public class EnforcementButton extends DataListActionDefault {

    public String getName() {
        return this.getClass().toString(); 
    }

    public String getVersion() {
        return "1.0";
    }

    public String getDescription() {
        return "PE - Enforcement Action Button"; 
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
        return "HRDC - EMPM - Enforcement Multi-Function Button";
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
                + "        description : 'Potential Employer Button',\n"
                + "        value : 'Potential Employer Button'\n"
                + "    },{\n" 
                + "            name: 'type', " 
                + "            label: 'PE Button Type', " 
                + "            type: 'radio', " 
                + "            options : [\n" 
                + "                {value: 'complain', label : 'Complain'}" 
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
        String writeOffId = "";
        String batch = "";
        
        for(String id:rowKeys){
            
            switch(type){
                case "complaint":
//                    processEgmnt(db, id);
                    message = "Pushed "+Integer.toString(updateCount)+" Potential Employer(s) to AEU ";
                break;
            }            
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
    
}
