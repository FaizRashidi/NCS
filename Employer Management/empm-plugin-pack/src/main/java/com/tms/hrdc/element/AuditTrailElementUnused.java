/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.element;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.Grid;
import org.joget.apps.form.model.AbstractSubForm;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormBuilderPalette;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormContainer;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.model.FormStoreMultiRowElementBinder;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;

/**
 *
 * @author faizr
 */
public class AuditTrailElementUnused extends Element implements FormBuilderPaletteElement, FormContainer{
    
    protected Map<FormData, FormRowSet> cachedRowSet = new HashMap<FormData, FormRowSet>();
    
    @Override
    public String getName() {
        return "Audit_Trail_Grid";
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
        return "<label class='label'>Grid</label><table cellspacing='0'><tr><th>Header</th><th>Header</th></tr><tr><td>Cell</td><td>Cell</td></tr></table>";
    }

    @Override
    public String getLabel() {
        return "Audit Trail Element";
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/audit_trail_grid.json", null, true, "message/StoreAuditTrailElement");
    }

    @Override
    public String getFormBuilderCategory() {
        return FormBuilderPalette.CATEGORY_CUSTOM;
    }

    @Override
    public int getFormBuilderPosition() {
        return 1100;
    }

    @Override
    public String getFormBuilderIcon() {
        return "<i class=\"fa fa-paperclip\"></i>";
    }
    
    public class Constants{
        static final String NUMBER = "no";
        static final String DATETIME = "datetime";
        static final String USERNAME = "username";
        static final String STATUS = "status";
        static final String REMARKS = "remarks";
    }
    
    /**
     * Return a Map of value=label, each pair representing a grid column header.
     * @param formData
     * @return
     */
    protected Map<String, String> getHeaderMap(FormData formData, String includeNum) {
        Map<String, String> headerMap = new LinkedHashMap();

        if(includeNum.equals("true")){
            headerMap.put(Constants.NUMBER, "No.");
        }
        headerMap.put(Constants.DATETIME, StringUtil.stripHtmlRelaxed("Date Time"));
        headerMap.put(Constants.USERNAME, StringUtil.stripHtmlRelaxed("User"));
        headerMap.put(Constants.STATUS, StringUtil.stripHtmlRelaxed("Status"));
        headerMap.put(Constants.REMARKS, StringUtil.stripHtmlRelaxed("Remarks")); 
        
        return headerMap;
    }

    /**
     * Return the grid data
     * @param formData
     * @return A FormRowSet containing the grid cell data.
     */
    protected FormRowSet getRows(FormData formData, String includeNum) {
        
        String rowSort = getPropertyString("rowSort");
        String dateFormat = getPropertyString("dateFormat");  
        
        if (!cachedRowSet.containsKey(formData)) {
            
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
            
            FormRowSet rowSet = new FormRowSet();
            
            Map<String, String> headerMap = getHeaderMap(formData, includeNum);
            
            if (!FormUtil.isFormSubmitted(this, formData)) {      
                
                String query = "SELECT c_actionTime, c_actionBy, c_status, c_remarks "
                                + "FROM app_fd_audit_trail "
                                + " WHERE c_parentId = ?";
                
                if(rowSort.equals("asc")){
                    query+=" ORDER BY c_actionTime asc";
                }else{
                    query+=" ORDER BY c_actionTime desc";
                }
                
                DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
                Connection con = null;
                
                try{
                    con = ds.getConnection();
                    PreparedStatement ps = con.prepareStatement(query,
                            ResultSet.TYPE_SCROLL_INSENSITIVE, 
                            ResultSet.CONCUR_READ_ONLY);
                    ps.setString(1, primaryKeyValue);
                    ResultSet rs = ps.executeQuery();
                    
                    int rowCount = 1;
                    if(rowSort.equals("desc")){
                        if (rs.last()) {//make cursor to point to the last row in the ResultSet object
                            rowCount = rs.getRow();
                            rs.beforeFirst(); //make cursor to point to the front of the ResultSet object, just before the first row.
                        }
                    }
                    
                    if(rs.next()){
                        do{
                            FormRow data = new FormRow();
                            HashMap dataHm = new HashMap();
                            String dateTime = StringUtils.isBlank(rs.getString("c_actionTime"))?"":rs.getString("c_actionTime");
                            
                            dateTime = formatDate(dateTime, dateFormat);
                            
                            if(includeNum.equals("true")){
                                dataHm.put(Constants.NUMBER, Integer.toString(rowSort.equals("desc")?rowCount--:rowCount++));
                            }
                            
                            dataHm.put(Constants.DATETIME, dateTime);
                            dataHm.put(Constants.USERNAME, StringUtils.isBlank(rs.getString("c_actionBy"))?"":rs.getString("c_actionBy"));
                            dataHm.put(Constants.STATUS, StringUtils.isBlank(rs.getString("c_status"))?"":rs.getString("c_status"));
                            dataHm.put(Constants.REMARKS, StringUtils.isBlank(rs.getString("c_remarks"))?"":rs.getString("c_remarks"));
                            
                            for (String header : headerMap.keySet()) {
                                String val = (String) dataHm.get(header);
                                data.setProperty(header, val);
                            }
                            
                            if(!data.isEmpty()){
                                if(rowSet.isEmpty()){
                                    rowSet = new FormRowSet();    
                                }
                                rowSet.add(data);
                            }
                        }while(rs.next());
                    }
                    
                }catch(SQLSyntaxErrorException e){
                    LogUtil.info("SQL Error", e.getMessage());
                }catch(Exception e){
                    e.printStackTrace();
                }finally{
                    try {
                        con.close();
                    } catch (SQLException ex) {
                        Logger.getLogger(AuditTrailElementUnused.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            cachedRowSet.put(formData, rowSet);
        }
        return cachedRowSet.get(formData);
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
    
    public String getElementValue(String element, Form form, FormData formData) {
        try {
            Element e = FormUtil.findElement(element, form, formData);
            return FormUtil.getElementPropertyValue(e, formData);
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        }
        return "";
    }

    @Override
    public FormRowSet formatData(FormData formData) {
        
        FormRowSet rowSet = new FormRowSet();
        rowSet.setMultiRow(true);

        return rowSet;
    }

    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        String template = "audit_trail_grid.ftl";
        String includeNum = getPropertyString("includeNum");
        
        String[] valueArray = {Constants.DATETIME, Constants.USERNAME,Constants.REMARKS,  Constants.STATUS};
        List<String> values = new ArrayList<>(Arrays.asList(valueArray));
        if(includeNum.equals("true")){
           values.add(0, Constants.NUMBER);
        }
        dataModel.put("values", values);

        // set validator decoration
        String decoration = FormUtil.getElementValidatorDecoration(this, formData);
        dataModel.put("decoration", decoration);

        dataModel.put("customDecorator", getDecorator());
        
        // set headers
        Map<String, String> headers = getHeaderMap(formData, includeNum);
        dataModel.put("headers", headers);
        
        // set rows
        FormRowSet rows = getRows(formData, includeNum);
        dataModel.put("rows", rows);

        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }    
     
    @Override
    public Boolean selfValidate(FormData formData) {
        Boolean valid = true;
        
        FormRowSet rowSet = getRows(formData, "");
        String id = FormUtil.getElementParameterName(this);
        String errorMsg = getPropertyString("errorMessage");
        
        String min = getPropertyString("validateMinRow");
        if (min != null && !min.isEmpty()) {
            try {
                int minNumber = Integer.parseInt(min);
                if (rowSet.size() < minNumber) {
                    valid = false;
                }
            } catch (Exception e) {}
        }
        
        String max = getPropertyString("validateMaxRow");
        if (max != null && !max.isEmpty()) {
            try {
                int maxNumber = Integer.parseInt(max);
                if (rowSet.size() > maxNumber) {
                    valid = false;
                }
            } catch (Exception e) {}
        }
        
        if (!valid) {
            formData.addFormError(id, errorMsg);
        }
        
        return valid;
    }

    
    @Override
    public Collection<String> getDynamicFieldNames() {
        Collection<String> fieldNames = new ArrayList<String>();
        
//        if (getStoreBinder() == null) {
//            fieldNames.add(getPropertyString(FormUtil.PROPERTY_ID));
//        }
        
        return fieldNames;
    }

    protected String getDecorator() {
        String decorator = "";
        
        try {
            String min = getPropertyString("validateMinRow");
            
            if ((min != null && !min.isEmpty())) {
                int minNumber = Integer.parseInt(min);
                if (minNumber > 0) {
                    decorator = "*";
                }
            }
        } catch (Exception e) {}
        
        return decorator;
    }

    private String formatDate(String dateTime, String dateFormat) {
        String newFormattedTime = "";
        try {
            DateFormat inputFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Date da = (Date)inputFormatter.parse(dateTime);
            
            DateFormat outputFormatter = new SimpleDateFormat(dateFormat);
            newFormattedTime = outputFormatter.format(da);
            
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        
        return newFormattedTime;
    }
}
