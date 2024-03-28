/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.validator;

import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.util.ArrayList;
import javax.sql.DataSource;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormValidator;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;

/**
 *
 * @author faizr
 */
public class EmpMyCoIDValidator extends FormValidator {

    @Override
    public String getName() {
        return "HRDC EMPM MyCoID Validator";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "To validate MyCoId Data";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - MyCoId - Validator";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        String formDefField = null;
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        if (appDef != null) {
            String formJsonUrl = "[CONTEXT_PATH]/web/json/console/app/" + appDef.getId() + "/" + appDef.getVersion() + "/forms/options";
            formDefField = "{name:'formDefId',label:'@@form.defaultformoptionbinder.formId@@',type:'selectbox',required:'True',options_ajax:'" + formJsonUrl + "'}";
        } else {
            formDefField = "{name:'formDefId',label:'@@form.defaultformoptionbinder.formId@@',type:'textfield',required:'True'}";
        }
        Object[] arguments = new Object[]{formDefField};
        String json = AppUtil.readPluginResource(getClass().getName(), "/properties/form/duplicateValueValidator.json", arguments, true, "message/form/DuplicateValueValidator");
        return json;
    }
    
    @Override
    public String getElementDecoration() {
        String decoration = "";
        String mandatory = (String) getProperty("mandatory");
        if ("true".equals(mandatory)) {
            decoration += " * ";
        }
        if (decoration.trim().length() > 0) {
            decoration = decoration.trim();
        }
        return decoration;
    }
    
    @Override
    public boolean validate(Element element, FormData data, String[] values) {
        
//        LogUtil.info(this.getClassName(), "VALIDATOR YO!!!!!!!!!!!!!!!!!!");
        boolean result = true;
        String id = FormUtil.getElementParameterName(element);
        String formDefId = (String) getProperty("formDefId");
        String fieldId = (String) getProperty("fieldId");
        String mandatory = (String) getProperty("mandatory");
        String regex = (String) getProperty("regex");
        String errorFormatMsg = (String) getProperty("errorFormatMsg");
        String errorDuplicateMsg = (String) getProperty("errorDuplicateMsg");
        PluginManager pm = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");

        //need to get regType value
        
        String regType = getRegType(element, data);
        
        //Check is empty or not
        if (isEmptyValues(values)) {
            if ("true".equals(mandatory)) {
                result = false;
                data.addFormError(id, pm.getMessage("form.duplicatevaluevalidator.e.missingValue", this.getClassName(), null));
            }
        } else {
            //check value format with regex
            if (!isFormatCorrect(regex, values)) {
                result = false;
                if (errorFormatMsg == null || errorFormatMsg.isEmpty() || errorFormatMsg.trim().length() == 0) {
                    errorFormatMsg = pm.getMessage("form.duplicatevaluevalidator.e.formatInvalid", this.getClassName(), null); 
                }
                data.addFormError(id, errorFormatMsg);
            } else {
                //check for duplicate value
                AppDefinition appDef = AppUtil.getCurrentAppDefinition();
                String tableName = null;
                AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");

                if (formDefId != null) {
                    tableName = appService.getFormTableName(appDef, formDefId);
                }
                if (tableName != null) {
                    if (regType.equals("HQ") &&
                            isDuplicate(formDefId, tableName, element, data, fieldId, values)) {
                        result = false;
                        if (errorDuplicateMsg == null || errorDuplicateMsg.isEmpty() || errorDuplicateMsg.trim().length() == 0) {
                            errorDuplicateMsg = pm.getMessage("form.duplicatevaluevalidator.e.valueAlreadyExist", this.getClassName(), null);
                        }
                        data.addFormError(id, errorDuplicateMsg);
                    }
                } else {
                    result = false;
                    data.addFormError(id, pm.getMessage("datalist.formrowdeletedatalistaction.noform", this.getClassName(), null));
                }
            }
        }
        return result;
    }
    
    protected boolean isEmptyValues(String[] values) {
        boolean result = false;
        if (values == null || values.length == 0) {
            result = true;
        } else {
            for (String val : values) {
                if (val == null || val.trim().length() == 0) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }
    
    protected boolean isFormatCorrect(String regex, String[] values) {
        if (regex == null || (regex != null && regex.trim().length() == 0)) {
            return true;
        }

        boolean result = true;
        if (values != null && values.length > 0) {
            for (String val : values) {
                if (val != null && !val.matches("^" + regex + "$")) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    protected boolean isDuplicate(String formDefId, String tableName, Element element, FormData formData, String fieldId, String[] values) {
        boolean result = false;
        FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");

        if (values != null && values.length > 0) {
            for (String val : values) {
                String key = null;
                
                String primaryKey = element.getPrimaryKeyValue(formData);
                
                if (!FormUtil.PROPERTY_ID.equals(fieldId)) {
                    try {
                        key = formDataDao.findPrimaryKey(formDefId, tableName, fieldId, val.trim());
                        
                        if (primaryKey != null && key != null && key.equals(primaryKey)) {
                            LogUtil.info(this.getClassName(), "MYCOID NOT CHANGED "+val.trim()+", "+key+" , id "+primaryKey);
                            return false;
                        }
                    } catch (Exception e) {
                        key = null;
                    }
                } else if (FormUtil.isElementPropertyValuesChanges(element, formData, values)) {
//                    if (formDataDao.load(formDefId, tableName, val.trim()) != null) {
//                        key = val.trim();
//                    }query for mycoid check here
                    LogUtil.info(this.getClassName(), "MYCOID CHANGED "+val.trim()+", id "+primaryKey);
                    checkExist(val.trim(), primaryKey);
                }
                
                if (key != null && key.trim().length() > 0) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }
    
    public boolean checkExist(String mycoid, String id){

        DBHandler db = new DBHandler();
        boolean exist = false;
        
        try{
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
        
            String query = "SELECT * FROM app_fd_empm_reg r"+
                    " inner join app_fd_empm_regAppl a on a.c_empl_fk = r.id "+
                    " WHERE r.c_mycoid = ? AND "+
                    " r.c_empl_reg_type = 'HQ' AND " +
                    "r.c_data_status IN "+
                    "(?,?,?,?) AND " +
                    " a.c_flow_status != ? AND " +
                    " a.id!=?";
            
            ArrayList list = db.select(query,
                            new String[]{
                                mycoid,
                                Constants.STATUS.EMP.REGISTERING,
                                Constants.STATUS.EMP.REGISTER_APPROVED,
                                Constants.STATUS.EMP.DEREGISTERING,
                                Constants.STATUS.EMP.DEREGISTER_REJECTED,
                                Constants.STATUS.EMP.DRAFT,
                                id
                            });
            
            if(list.size()>0){
                exist = true;
            }
            
        }catch(Exception e){
            e.printStackTrace();
        } finally {
            db.closeConnection();
        }
        
        return exist;
    }

    private String getRegType(Element element, FormData data) {
        
        Form form = FormUtil.findRootForm(element);
        Element regType_elem = FormUtil.findElement("empl_reg_type", form, data);
        String regType_val = "HQ";
        
        if(regType_elem != null){
            regType_val = FormUtil.getElementPropertyValues(regType_elem, data)[0];
//            LogUtil.info(this.getClassName(), "REGISTER TYPE!!! "+regType_val);
        }
        
        return regType_val;
    }
}
