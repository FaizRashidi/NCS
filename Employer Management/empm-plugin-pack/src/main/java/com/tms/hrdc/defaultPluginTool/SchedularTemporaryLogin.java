/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.datalistAction.DeleteUserAndRecord;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;

/**
 *
 * @author kahyi
 */
public class SchedularTemporaryLogin extends DefaultApplicationPlugin{

    @Override
    public String getName() {
        return this.getClassName();
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Schedular for Deleting Temporary Login Users";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Scheduler For Deleting Temporary Login Users";
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
    public Object execute(Map props) {

       DBHandler db = new DBHandler();

        try {
            db.openConnection();
            HashMap dayshm = db.selectOneRecord("SELECT c_tempLoginValid_days FROM "+Constants.TABLE.STP_EMPREG);
            
            String days = "0";
            if(dayshm!=null){
                days = dayshm.get("c_tempLoginValid_days").toString();
            }

            ArrayList<HashMap<String, String>> data = 
                    db.select("SELECT c_req_email FROM app_fd_empm_reg r WHERE "
                            + "c_emp_status = 'ACTIVE' "
                            + "AND datediff(now(),c_approve_dt) = "+days);
            
            LogUtil.info(this.getClassName(), "Deleting temp acc "+data.toString()+"..");
            
            for(HashMap hm:data){
                String email = hm.get("c_req_email")==null?"":hm.get("c_req_email").toString();
                if(!email.isEmpty()){
                    DeleteUserAndRecord.deleteUser(db, email);
                }
            }
        
        } catch (SQLException ex) {
            ex.printStackTrace();
            
        } finally {
            db.closeConnection();
        }
        
        return null;
    }

}
