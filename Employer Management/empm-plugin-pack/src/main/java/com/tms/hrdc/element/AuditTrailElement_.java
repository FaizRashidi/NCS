/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.element;

import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.KeywordDictionary;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.AbstractSubForm;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormContainer;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author faizr
 */
public class AuditTrailElement_ extends Element implements FormBuilderPaletteElement, FormContainer{
    
    protected Map<FormData, FormRowSet> cachedRowSet = new HashMap<FormData, FormRowSet>();
    
    @Override
    public String getName() {
        return this.getClass().toString();
    }

    @Override
    public String getVersion() {
        return "2.0.0";
    }

    @Override
    public String getDescription() {
        return "To record workflow changes";
    }
    
    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getFormBuilderTemplate() {
        return "<label class='label'>"
                + "Grid"
                + "</label>"
                + "<table cellspacing='0'>"
                + "<tr><th>Header</th><th>Header</th></tr><tr><td>Cell</td><td>Cell</td></tr>"
                + "</table>";
    }

    @Override
    public String getLabel() {
        return "EMPM Audit Trail";
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/audit_trail_grid.json", null, true, "message/StoreAuditTrailElement");
    }

    @Override
    public String getFormBuilderCategory() {
        return "HRDC Emp. Mgmt Element";
    }

    @Override
    public int getFormBuilderPosition() {
        return 1200;
    }

    @Override
    public String getFormBuilderIcon() {
        return "<i class=\"fa fa-wheelchair-alt\" aria-hidden=\"true\"></i>";
    }    

    private ArrayList<HashMap<String, String>> processData(DBHandler db, ArrayList<HashMap<String, String>> sub_list) {
        
        for(HashMap hm:sub_list){
            String isSecFld = hm.get("c_isSectorField")==null?"":
                    hm.get("c_isSectorField").toString();
            String isLocFld = hm.get("c_isLocationField")==null?"":
                    hm.get("c_isLocationField").toString();
            
            String prev_val = hm.get("c_prev_value")==null?"":
                    hm.get("c_prev_value").toString();
            String curr_val = hm.get("c_curr_value")==null?"":
                    hm.get("c_curr_value").toString();
            
            if(isSecFld.equals("true")){
                hm.put("c_curr_value", getSectLabel(db, curr_val));
                hm.put("c_prev_value", getSectLabel(db, prev_val));
            }
            
            if(isLocFld.equals("true")){        
                hm.put("c_curr_value", getLocLabel(db, curr_val));
                hm.put("c_prev_value", getLocLabel(db, prev_val));
            }
        }
        
        return sub_list;
    }

    private String getSectLabel(DBHandler db, String id) {
        String query = "select data.id, data.i_value from (\n" +
                        "select id, concat(s.c_descr, '(',s.c_sub_sector_code,')') as i_value from app_fd_stp_sub_sector s\n" +
                        "UNION\n" +
                        "select id, concat(c_descr, '(',c_main_sector_code,')') from app_fd_stp_main_sector \n" +
                        "UNION\n" +
                        "select id, concat(a.c_descr, '(',a.c_div_code,')') from app_fd_stp_industry_div a\n" +
                        "UNION\n" +
                        "select id, concat(s.c_industry_sector, ' (', s.c_industry_sector_code, ')') from app_fd_stp_industry_sector s\n" +
                        ") data WHERE data.id = ?";
        
        HashMap hm = db.selectOneRecord(query, new String[]{id});
        
        if(hm==null){
            return id;
        }
        
        return hm.get("i_value")==null?"":hm.get("i_value").toString();
    }

    private String getLocLabel(DBHandler db, String id) {
        String query = "select data.id, data.i_value from (\n" +
                    "select id, c_location as i_value from app_fd_stp_location\n" +
                    "union\n" +
                    "select id, c_city from app_fd_stp_city\n" +
                    "union\n" +
                    "select id, c_state from app_fd_stp_state\n" +
                    "union\n" +
                    "select id, c_country from app_fd_stp_country\n" +
                    ") data WHERE data.id = ?";
        
        HashMap hm = db.selectOneRecord(query, new String[]{id});
        
        if(hm==null){
            return id;
        }
        
        return hm.get("i_value")==null?"":hm.get("i_value").toString();
    }

    public class REF{
        static final String NUMBER = "no";        
        static final String VALUE_FROM = "value_from";
        static final String VALUE_TO = "value_to";
        
//        static final String DATETIME = "dateCreated";
//        static final String USERNAME = "createdByName";
        
        static final String DATETIME = "dateModified";
        static final String USERNAME = "modifiedByName";
        
        static final String STATUS = "c_status";
        static final String REMARKS = "c_remarks";
        static final String ID = "id";
        static final String LINK = "link";
        
        static final String SUB_AUDIT = "sub_audit";
        static final String SUB_FIELD_NAME = "c_field_name";
        static final String SUB_PREV_VAL = "c_prev_value";
        static final String SUB_CURR_VAL = "c_curr_value";
    }
    
     @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        String template = "auditTable.ftl";
        String pKey = getPrimaryKey(formData);
        DBHandler db = new DBHandler();
        try{
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
        
            KeywordDictionary kd = new KeywordDictionary(db);
        
            ArrayList<HashMap> auditHm = getAuditData(pKey, db);
            String customHTML = buildHTMLScript(db, kd, auditHm);

            dataModel.put("value", customHTML);
            
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }

        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }   
    
     private String buildHTMLScript(DBHandler db, KeywordDictionary kd, ArrayList<HashMap> aud_list) {
         
        int count = 2;
        String cls = "";
        
        String html = "<table cellspacing=\"0\" style=\"width:100%;\" class=\"tablesaw tablesaw-stack\" data-tablesaw-mode=\"stack\" >\n" +
                        "    <thead>\n" +
                        "    <tr>\n" +
                        "        <th id=\"approver_usr\" style=\"\">Date</th>\n" +
                        "        <th id=\"approver_desg\" style=\"\">Action By</th>\n" +
                        "        <th id=\"approver_status\" style=\"\">Status</th>\n" +
                        "        <th id=\"approver_dept\" style=\"\">Reference Field</th>\n" +
                        "        <th id=\"approver_state\" style=\"\">Value From</th>\n" +
                        "        <th id=\"approver_address\" style=\"\">Value To</th>\n" +
                        "        <th id=\"approver_link\" style=\"\"></th>\n" +
                        "    </tr>\n" +
                        "    </thead>\n" +
                        "    <tbody>";
        
//        LogUtil.info("Audit", aud_list.toString());
        
        if(aud_list == null ){
            html += "</tbody>"
                + "</table>";
            
            return html;
        }
        
        for(HashMap audHm:aud_list){
            
            if(count%2==0){
                cls = "\"grid-row even pg-tr-show\"";
            }else{
                cls = "\"grid-row odd pg-tr-show\""; 
            }
            
            ArrayList<HashMap> sub_audit = (ArrayList<HashMap>) (audHm.containsKey(REF.SUB_AUDIT)?audHm.get(REF.SUB_AUDIT):new ArrayList());
            int size = sub_audit==null?1:(sub_audit.size()==0?1:sub_audit.size());
            
            html += "<tr class="+cls+" >\n"
                    + "<td rowspan="+size+">"+audHm.get(REF.DATETIME)+"</td>\n"
                    + "<td rowspan="+size+">"+audHm.get(REF.USERNAME)+"</td>\n"
                    + "<td rowspan="+size+">"+audHm.get(REF.STATUS)+"</td>\n";
            
            if(sub_audit!=null && sub_audit.size()>0){
                HashMap firstSub = sub_audit.get(0);                
                html += "<td>"+kd.getEmplrMapList().get(firstSub.get(REF.SUB_FIELD_NAME))+"</td>\n"
                        + "<td>"+firstSub.get(REF.SUB_PREV_VAL)+"</td>\n"
                        + "<td>"+firstSub.get(REF.SUB_CURR_VAL)+"</td>\n";
                sub_audit.remove(0);
            }else{
                html += "<td></td>\n"
                        + "<td></td>\n"
                        + "<td></td>\n";
            }
            
            html += "<td rowspan="+size+">"+audHm.get(REF.LINK)+"</td>"
                    + "</tr>";
            
            for(HashMap subHm:sub_audit){
                html += "<tr class="+cls+" >"
                        + "<td>"+kd.getEmplrMapList().get(subHm.get(REF.SUB_FIELD_NAME))+"</td>\n"
                        + "<td>"+subHm.get(REF.SUB_PREV_VAL)+"</td>\n"
                        + "<td>"+subHm.get(REF.SUB_CURR_VAL)+"</td>\n"
                        + "</tr>";
            }
            
            count++;
        }
        
        html += "</tbody>"
                + "</table>";
//
//        LogUtil.info("HTML", html);
        return html;
    }
    
    private String getPrimaryKey(FormData formData) {
        Form form = FormUtil.findRootForm(this);

        form = findFormForLoadBinder(form);
        if (form == null) {
            form = FormUtil.findRootForm(this);
        }
        String primaryKeyValue = "";            
        try{
            primaryKeyValue = StringUtils.isBlank(form.getPrimaryKeyValue(formData))?"":form.getPrimaryKeyValue(formData);
        }catch(NullPointerException e){

        }
        
        return primaryKeyValue;
    }
    
    protected Form findFormForLoadBinder(Element element) {
        Form form = null;
        if (element != null) {
            if (element.getLoadBinder() == this) {
                if (element instanceof AbstractSubForm) {
                    Collection<Element> children = element.getChildren();
                    if (!children.isEmpty()) {
                        form = (Form) children.iterator().next();
                    }
                } else if (element instanceof Form) {
                    form = (Form) element;
                }
            } else {
                for (Element child : element.getChildren()) {
                    form = findFormForLoadBinder(child);
                    if (form != null) {
                        break;
                    }
                }
            }
        }
        return form;
    }
    
    private ArrayList<HashMap> getAuditData(String pKey, DBHandler db) {
        
        ArrayList data = new ArrayList();
        
        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
            
            String query = "SELECT h.id, h.dateCreated, h.createdBy, h.createdByName, "
                    + "h.dateModified, h.modifiedBy, h.modifiedByName, h.c_status, h.c_remarks, "
                    + "concat('<a onclick=\"window.open(''/jw/web/userview/empm/emp/_/emp_reg_arc?id=',c.id,'&embed=true'',''popup'',''width=950,height=400''); return false;\">',\n" 
                    + "        'View Archive', '</a>') as link "
                    + "FROM app_fd_empm_audit h "
                    + "LEFT JOIN app_fd_empm_emp_arc c ON h.id = c.c_audit_id and c_is_main_ref is null "
                    + "WHERE h.c_fk = ? "
                    + " ORDER BY h.dateCreated desc ";
            ArrayList<HashMap<String, String>> resultList = db.select(query, new String[]{pKey});
            
            if(resultList == null){
                throw new Exception();
            }
//            LogUtil.info("AUDIT", "result "+resultList.toString());
            for(HashMap pHm:resultList){
                HashMap aud = new HashMap();
                aud = pHm;
                
                pKey = pHm.get("id").toString();
                
//                query = "SELECT c_field_name, c_prev_value, c_curr_value FROM app_fd_empm_audit_sub h WHERE h.c_fk = ?";
                query = "SELECT c_field_name, c_prev_value, c_curr_value, k.c_isLocationField, k.c_isSectorField \n" +
                        "FROM app_fd_empm_audit_sub h " +
                        "INNER JOIN app_fd_empm_keywords k on k.c_columnID = h.c_field_name WHERE h.c_fk = ?";
                ArrayList<HashMap<String, String>> sub_list = db.select(query, new String[]{pKey});
                
                if(sub_list != null){
                    sub_list = processData(db, sub_list);
                    aud.put(REF.SUB_AUDIT, sub_list);
                }
                data.add(aud);
                
            }
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            LogUtil.info("AUDIT", "No Audit trail data dudeeeeee");
        } finally {
            db.closeConnection();
        }
        
        return data;
    }
}
