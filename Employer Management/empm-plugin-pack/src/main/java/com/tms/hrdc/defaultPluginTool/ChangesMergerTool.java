/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;

/**
 *
 * @author faizr
 */
public class ChangesMergerTool extends DefaultApplicationPlugin{

    @Override
    public String getName() {
        return "Changes Merger Tool";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "To merge changed data in Employer";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Emp. Data Merger Tool";
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
        WorkflowAssignment wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");     
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        String id = appService.getOriginProcessId(wfAssignment.getProcessId());
        
        DBHandler db = new DBHandler();
        
        try{
            db.openConnection();
            
            String sql = "SELECT * FROM app_fd_empm_reg_changes WHERE id = ?";            
            HashMap reqData = db.selectOneRecord(sql, new String[]{id});
            
            String empId = reqData.get("c_emp_fk")==null?"":reqData.get("c_emp_fk").toString();
            
            if(empId.isEmpty()){
                db.closeConnection();
                return null;
            }
            
            String changeType = reqData.get("c_req_type").toString();
            String chgeFld = "";
            String chgeVal = "";

            String changeQuery = "";
            
            switch(changeType){
                case "Change of MyCoID":
//                    chgeFld = "c_mycoid";
                    chgeVal = reqData.get("c_new_mycoid").toString();

                    changeQuery = "c_mycoid='"+chgeVal+"'";
                break;
                case "Change Company Name":
//                    chgeFld = "c_comp_name";
                    chgeVal = reqData.get("c_new_comp_name").toString();

                    String mycoid = db.selectOneValueFromTable(
                            "SELECT c_mycoid FROM app_fd_empm_reg WHERE id = ?",
                            new String[]{empId}
                    );

                    db.update(
                            "UPDATE dir_user SET firstName = ? WHERE id = ? ",
                            new String[]{chgeVal},
                            new String[]{mycoid}
                    );

                    changeQuery = "c_comp_name='"+chgeVal+"'";
                break;
                case "Change Company Activity (Sector Code)":
//                    chgeFld = "c_sector_search_id";
                    chgeVal = reqData.get("c_sector_search_id_new").toString();
                    HashMap<String,String> sectorDetailsNew = getMSICDetails(db, chgeVal);

                    changeQuery = sectorDetailsNew.entrySet().stream()
                            .map(entry -> entry.getKey() + "='" + entry.getValue() + "'")
                            .collect(Collectors.joining(", "));
                break;
            }

            int i = 0;
//            if(!chgeFld.isEmpty() && !chgeVal.isEmpty()){
            if(!changeQuery.isEmpty()){
//                sql = "UPDATE "+Constants.TABLE.EMPREG+" "
//                        + "SET "+chgeFld+" = ?"
//                        + "WHERE id = ?";

                sql = "UPDATE "+Constants.TABLE.EMPREG+" "
                        + "SET "+changeQuery+" "
                        + "WHERE id = '"+empId+"'";
//                int i = db.update(sql, new String[]{chgeVal}, new String[]{empId});
                i = db.update(sql);
            }
            
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }        
        return null;
    }


    private HashMap getMSICDetails(DBHandler db, String sectorId){
        HashMap hm = db.selectOneRecord(
                "select \n" +
                        "q.c_sector_section as c_industry_sector,\n" +
                        "(\n" +
                        "select \n" +
                        "  concat(s1.c_industry_sector_code,' - ', s1.c_industry_sector)\n" +
                        "  from app_fd_stp_industry_sector s1 where id = q.c_sector_section\n" +
                        "  limit 1\n" +
                        ") as c_industry_sector_label,\n" +
                        "\n" +
                        "q.c_sector_div as c_div,\n" +
                        "(\n" +
                        "select \n" +
                        "  concat(s2.c_div_code,' - ', s2.c_descr)\n" +
                        "  from app_fd_stp_industry_div s2 where id = q.c_sector_div\n" +
                        "  limit 1\n" +
                        ") as c_div_label,\n" +
                        "\n" +
                        "q.c_main_sector_code as c_main_sector_code,\n" +
                        "(\n" +
                        "select \n" +
                        "  concat(s4.c_main_sector_code,' - ', s4.c_descr)\n" +
                        "  from app_fd_stp_main_sector s4 where id = q.c_main_sector_code\n" +
                        "  limit 1\n" +
                        ") as c_main_sector_label,\n" +
                        "\n" +
                        "q.c_sector_class as c_class_code,\n" +
                        "(\n" +
                        "select \n" +
                        "  concat(s5.c_sector_class_code,' - ', s5.c_descr)\n" +
                        "  from app_fd_stp_class_sector s5 where id = q.c_sector_class\n" +
                        "  limit 1\n" +
                        ") as c_class_label,\n" +
                        "\n" +
                        "concat(q.c_sub_sector_code, ' - ', q.c_descr) as c_sector_descr\n" +
                        "\n" +
                        "from app_fd_stp_sub_sector q\n" +
                        "where id = ? ",
                new String[]{sectorId}
        );

        hm.put("c_sector_search_id", sectorId);
        hm.put("c_sector_code", sectorId);

        return hm;
    }
}
