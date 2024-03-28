package com.tms.hrdc.defaultPluginTool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.DBHandler;
import antlr.StringUtils;
import com.tms.hrdc.util.Constants;
import javax.sql.DataSource;
import java.sql.PreparedStatement;

public class SchedulerPotentialEmployer extends DefaultApplicationPlugin{


    private Connection con;

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
        return "Schedular for Potential Employer";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Scheduler For Potential Employer";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/acc_creator_tool.json", null, true);
    }
    
    @Override
    public Object execute(Map props) {

       DBHandler db = new DBHandler();

        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));

            String query = "SELECT * FROM app_fd_empm_reg r WHERE c_general_status ='FORM 1 APPROVED'\n" +
                            "and c_approve_dt < (NOW()-INTERVAL (SELECT c_empRejected AS interval_no FROM app_fd_empm_reg_stp) MONTH)\n" +
                            "AND not EXISTS(SELECT * FROM app_fd_empm_pe_pot_empl s \n" +
                            "WHERE s.c_pe_myCoID = r.c_mycoid)";
                            
            ArrayList<HashMap<String, String>> data = db.select(query);

            String column1 = "", column2 = "", column3 = "", column4 = "", column5= "", column6= "", 
                    column7 = "", column8 = "", column9= "", column10= "", column11= "";
            
            LogUtil.info("Potential Employer Scheduler", data.toString());
            
            for(HashMap hm:data){
                column1 = hm.get("c_mycoid")==null?"":hm.get("c_mycoidn").toString();
                column2 = hm.get("c_empl_email_pri")==null?"":hm.get("c_empl_email_pri").toString();
                column3 = hm.get("c_comp_name")==null?"":hm.get("c_comp_name").toString();
                column4 = hm.get("c_empl_fax_no_pri")==null?"":hm.get("c_empl_fax_no_pri").toString();
                column5 = hm.get("c_empl_postcode")==null?"":hm.get("c_empl_postcode").toString();
                column6 = hm.get("c_empl_country")==null?"":hm.get("c_empl_country").toString();
                column7 = hm.get("c_empl_empl_city")==null?"":hm.get("c_empl_city").toString();
                column8 = hm.get("c_empl_empl_city")==null?"":hm.get("c_empl_country").toString();
                column9 = hm.get("c_empl_state")==null?"":hm.get("c_empl_state").toString();
                column10 = hm.get("c_empl_country_code_pri")==null?"":hm.get("c_empl_country_code_pri").toString();
                column11 = hm.get("c_empl_address")==null?"":hm.get("c_empl_address").toString();
            
                HashMap saveData = new HashMap();
                saveData.put("pe_myCoID", column1);
                saveData.put("pe_email", column2);
                saveData.put("pe_compName", column3);
                saveData.put("pe_contact", column4);  
                saveData.put("pe_faxNo", column5);   
                saveData.put("pe_postcode", column6); 
                saveData.put("pe_country", column7); 
                saveData.put("pe_city", column8); 
                saveData.put("pe_state", column9); 
                saveData.put("pe_country_code", column10); 
                saveData.put("pe_address", column11); 

                CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, "potential_acc_form", "", saveData);
            }
        
        } catch (SQLException ex) {
            ex.printStackTrace();
            
        } finally {
            db.closeConnection();
        }
        
        return null;
    }

private void insert (String[] args) {

    PreparedStatement stmt = null;
    DBHandler db = new DBHandler();

    try {
        db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
         String query="insert into app_fd_empm_pe_pot_empl(c_pe_myCoID, c_pe_email, c_pe_compName, c_pe_contact, c_pe_faxNo, c_pe_postcode, c_pe_country,"
         +"c_pe_city, c_pe_state, c_pe_country_code, c_pe_address)"
         +"values(?,?,?,?,?,?,?,?,?,?,?) ";
         stmt= con.prepareStatement(query);
         stmt.executeUpdate();

         
      }
      catch(Exception e){
         e.printStackTrace();
        } finally {
            db.closeConnection();
        }
      
    
}

}

        