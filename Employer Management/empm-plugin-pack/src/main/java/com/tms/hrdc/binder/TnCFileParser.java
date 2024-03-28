/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.util.DBHandler;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.PluginThread;
import org.joget.plugin.base.DefaultApplicationPlugin;

/**
 *
 * @author faizr
 */
public class TnCFileParser extends WorkflowFormBinder{

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
        return "Used in parsing Term & Condition ID with files";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - TermsConditions and Files Parser";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }
    
    @Override
    public FormRowSet store(Element element, FormRowSet rowSet, FormData formData) {
        
        String formId = super.getFormId();
        
        FormRow row = rowSet.get(0);

        String tnc = row.getProperty("template");
        
        DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
        DBHandler db = new DBHandler();
        
        try {
            db.openConnection(ds);
        } catch (SQLException ex) {
        }
        
        ArrayList<HashMap<String, String>> filesArray = getFiles(db);
        
        db.closeConnection();
        
        String url = AppUtil.getRequestContextPath();
        
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        
        for(HashMap fileHm:filesArray){
            String filename = "";
            try {
                filename = URLEncoder.encode(fileHm.get("c_file").toString(), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                
            }
            String filenameSeq = "{"+fileHm.get("c_file_num").toString()+"}"; 
            String id = fileHm.get("id").toString(); 
            String link = "<a href=\""+url+"/web/client/app/"+appDef.getAppId()
                            +"/"+appDef.getVersion()
                            +"/form/download/files_upload/"+id+"/"+filename+".?attachment=true\">here</a>";
            
            if(!tnc.contains(filenameSeq)){
                continue;
            }            
            tnc = tnc.replace(filenameSeq, link);
        }
        
        row.setProperty("template", tnc);
        
        return super.store(element, rowSet, formData);     
    }

    private ArrayList<HashMap<String, String>> getFiles(DBHandler db) {        
        String query = "SELECT * FROM app_fd_empm_files_setup s";        
        return db.select(query);     
    }
}
