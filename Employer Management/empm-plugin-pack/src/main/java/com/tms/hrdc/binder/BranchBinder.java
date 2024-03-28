/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.HttpUtil;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FileUtil;
import org.joget.apps.form.service.FormService;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.PluginThread;
import org.joget.commons.util.SecurityUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class BranchBinder extends WorkflowFormBinder {

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
        return "Used in Employer Management read uploaded excel files, and process "
                + "associated branches based on uploader's HQ data";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Branch Binder";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    String unsuccessfulReason = "";
    int unsuccessfulRowsUploaded = 0;
    int successfulRowsUploaded = 0;
    int totalRows = 0;

    public void msg(String msg) {
        LogUtil.info(getName(), msg);
    }

    @Override
    public FormRowSet store(Element element, FormRowSet rowSet, FormData formData) {

        FormRowSet rows = super.store(element, rowSet, formData);

        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        FormDefinitionDao formDefinitionDao = (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");
        FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
        FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");

//        String id = formData.getPrimaryKeyValue();
        final String id = rows.get(0).getId();
        final String hq_no = (String) rows.get(0).get("hqId");

        final HashMap appUtilhm = new HashMap();
        appUtilhm.put("appService", appService);
        appUtilhm.put("formDefinitionDao", formDefinitionDao);
        appUtilhm.put("formService", formService);
        appUtilhm.put("formDataDao", formDataDao);

        String formId = super.getFormId();

        switch (formId) {
            case Constants.FORM_ID.FORMID_EMPM_BRANCH_EXCEL:
                branchExcel(rows, hq_no);
                break;
            case Constants.FORM_ID.FORMID_EMPM_BRANCH_MANUAL:
                branchManual(rowSet, id, hq_no);
                break;
        }
 
        return rows;
    }

    private void branchExcel(final FormRowSet rows, final String hq_no) {

        FormRow row = rows.get(0);
        final String id = row.getId();

        Thread checkingThread = new PluginThread(new Runnable() {
            @Override
            public void run() {

                LogUtil.info("FORMROW DATA", rows == null?"NULL": rows.toString());

                if (rows != null && !rows.isEmpty()) {
                    try{
                        File file = null;
                        String fileName = (String) row.getCustomProperties().get("branch_file");

                        try {
                            file = FileUtil.getFile(fileName, "empm_br_file_upl", id);
                        } catch (IOException ex) {
                            Logger.getLogger(this.getClass().toString()).log(Level.SEVERE, null, ex);
                        }

                        String file_path = file.getAbsolutePath();
                        msg("Reading file");
                        Vector dataHolder = CommonUtils.ReadCSV(file_path, 1);
                        if (dataHolder.isEmpty() || dataHolder == null) {
                            LogUtil.info(getName(), "data is EMPTY");
                        }
                        ArrayList data = CommonUtils.VectorToArrayList(dataHolder);

                        totalRows = data.size();
                        updateFileUploadProcess(id, "Branch Uploading In Progress");

                        msg("Data is " + data.toString());
                        storeData(data, hq_no, id);

                    }catch(Exception e){
                        e.printStackTrace();
                        updateFileUploadProcess(id, e.getMessage());
                    }

                    updateFileUploadProcess(id, "Upload Complete ");
                }


            }
        });

        checkingThread.setDaemon(true);
        checkingThread.start();

        updateFileUploadProcess(id, "Branch Uploading In Progress");
    }

    private void branchManual(FormRowSet rowSet, String id, String hqId) {

        DBHandler db = new DBHandler();
        FormRow row = rowSet.get(0);
        String hq_mycoid = "";
        String branch_mycoid = "";
        int branch_count = 0;

        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));

            HashMap compHm = getMycoid(db, hqId);

            if (compHm != null) {
                hq_mycoid = compHm.get("c_mycoid").toString();
                branch_count = Integer.parseInt(compHm.get("branch_count").toString());
            }
            
            branch_mycoid = hq_mycoid + "_" + String.format("%04d", branch_count);
            HashMap branchHm = new HashMap();
            branchHm.put("mycoid", hq_mycoid);
            branchHm.put("branch_mycoid", branch_mycoid);
            branchHm.put("hqId", hqId);
            
            String branch = CommonUtils.saveUpdateForm2("", "branch_data", id, branchHm);
            
        } catch (SQLException e) {
            LogUtil.error(CommonUtils.class.getName(), e, e.getMessage());
        } finally {
            db.closeConnection();
        }

    }

    private void storeData(ArrayList rows, String hqId, String uplId) {

        DataFormatter formatter = new DataFormatter();
        DBHandler db = new DBHandler();
        msg("Storing data.."+rows.toString());
//        String hq_emp_id = "";
        String hq_mycoid = "", branch_mycoid = "", name = "";
        int branch_count = 0;

        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));

            totalRows = rows.size();
            HashMap compHm = getMycoid(db, hqId);

            for (int rowNum = 0; rowNum < rows.size(); rowNum++) {
                ArrayList columns_list = (ArrayList) rows.get(rowNum);

                if (compHm != null) {
//                    hq_emp_id = compHm.get("c_emp_hq_id").toString();
                    hq_mycoid = compHm.get("c_mycoid").toString();
                    branch_count = Integer.parseInt(compHm.get("branch_count").toString());
                    branch_count++;
                }

//                branch_mycoid = hq_mycoid + "_" + String.format("%04d", branch_count);

                String country = "MALAYSIA", state = "", city = "", postcode = "", location = "";
                String country_id = "", state_id = "", city_id = "";
                boolean hasCity = false, hasState = false,
                        hasPostcode = false, hasAddress = false,
                        hasTelNo = false, hasEmail = false, hasName = false;
                
                ArrayList<String> errorList = new ArrayList();

                HashMap columnValues = new HashMap();

                for (int j = 0; j < columns_list.size(); j++) {
                    String col_value = (String) columns_list.get(j);
                    col_value = col_value.toUpperCase();
                    
                    String error = "";

                    switch (j) {
                        case 0:
                            columnValues.put("name", col_value);
                            hasName = true;
                            name = col_value;                            
                            error = "No name";                            
                            break;
                        case 1:
                            columnValues.put("address", col_value);
                            hasAddress = true;                            
                            error = "No address";
                            break;
                        case 2:
                            columnValues.put("postcode", col_value);
                            hasPostcode = true;
                            error = "No postcode";
                            break;
                        case 3:
                            columnValues.put("city", col_value);
                            hasCity = true;
                            error = "No city";
                            break;
                        case 4:
                            columnValues.put("state", col_value);
                            hasState = true;
                            error = "No state";
                            break;
                        case 5:
                            columnValues.put("empl_amount", col_value);
                            error = "No employee count";
                            break;
                        case 6:
                            columnValues.put("tel_no", col_value);
                            hasTelNo = true;
                            error = "No telno";
                            break;
                        case 7:
                            columnValues.put("email_general", col_value);
                            hasEmail = true;
                            error = "No email";
                            break;
                    }
                    
                    if(StringUtils.isBlank(col_value)){
                        errorList.add(error);
                    }
                }

                msg("1 Storing data.."+columnValues.toString());

                if (columnValues.get("postcode") != null && columnValues.get("postcode") != "") {
                    HashMap pcHm = getPostcodeData(db,
                            columnValues.get("address").toString(),
                            columnValues.get("postcode").toString(),
                            (columnValues.get("country") == null) ? "MALAYSIA" : columnValues.get("country").toString(),
                            columnValues.get("state").toString(),
                            columnValues.get("city").toString());

                    country_id = pcHm.get("country_id").toString();
                    state_id = pcHm.get("state_id").toString();
                    city_id = pcHm.get("city_id").toString();

                    columnValues.put("country", country_id);
                    columnValues.put("state", state_id);
                    columnValues.put("city", city_id);
                }

                msg("2 Storing data.."+columnValues.toString());

                if (!errorList.isEmpty()) {
                    String errorMsg = "<ol><li>"+StringUtils.join(errorList, "</li><li>")+"</li></ol>";
                    unsuccessfulReason += "<li>Incomplete data at row " + Integer.toString(rowNum+2) + " <br />"
                                        +errorMsg+ "</li>";
                    unsuccessfulRowsUploaded++;
                }
                else if (checkName(db, hqId, name)) {
                    msg("Branch Name '"+name+"' Already Exist");
                }
                else {
                    columnValues.put("mycoid",hq_mycoid);
                    columnValues.put("branch_mycoid", branch_mycoid);
                    columnValues.put("branch_status", "ACTIVE");
                    columnValues.put("hqId", hqId);
                    String branch = CommonUtils.saveUpdateForm2("", "branch_data", "", columnValues);

                    LogUtil.info("Inserted branch", branch);

                    successfulRowsUploaded++;
                }

                updateFileUploadProcess(uplId, "Uploading In Progress");
            }
//            unsuccessfulReason += "</ul>";
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.closeConnection();
        }
    }

    public void updateFileUploadProcess(String id, String complete) {
        String query = "update app_fd_empm_br_file_upl "
                + "set c_total_row = ?, c_total_success = ?, c_total_error = ?, c_error_detail = ?, c_upl_status = ? "
                + "where id = ?;";

        DBHandler db = new DBHandler();

        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));

            String[] val = {
                String.valueOf(totalRows),
                String.valueOf(successfulRowsUploaded),
                String.valueOf(unsuccessfulRowsUploaded),
                "<ul>"+unsuccessfulReason+"</ul>",
                complete
            };
            String[] cond = {id};

            int x = db.update(query, val, cond);

            msg("Upload Complete, upload table row update: " + x);

        } catch (SQLException ex) {
            LogUtil.info("MS Location Upload", "Exception in updating upload status" + ex.getMessage());
        } finally {
            db.closeConnection();
        }
    }

    private HashMap getPostcodeData(DBHandler db, String location, String postcode, String country, String state, String city) {

//        msg("Processing postcode "+postcode);
        String country_id = "", state_id = "", city_id = "";

        String query = "SELECT * FROM app_fd_stp_location WHERE c_postcode = ? and c_location like '%" + location + "%'";
        String[] cond = {postcode};

        if (country.isEmpty()) {
            country = "MALAYSIA";
        }

        ArrayList<HashMap<String, String>> q_result = db.select(query, cond);
        if (q_result != null && q_result.size() > 0) {

            country_id = q_result.get(0).get("c_country");
            state_id = q_result.get(0).get("c_state");
            city_id = q_result.get(0).get("c_city");

        } else {
            //UNUSED
//            country_id = getCountryID(db, country);
//            state_id = getStateID(db, state, country_id);
//            city_id = getCityID(db, city, state_id);
//            
//            msg("New postcode country "+postcode+" detected - "+country_id+" - "+country);
//            msg("New postcode state "+postcode+" detected - "+state_id+" - "+state);
//            msg("New postcode city "+postcode+" detected - "+city_id+" - "+city);
//            
//            HashMap locData = new HashMap();
//                    
//            locData.put("country", country_id);
//            locData.put("state", state_id);
//            locData.put("city", city_id);
//            locData.put("postcode", postcode);
//            locData.put("location", location);
//            locData.put("status", "Active");
//
//            CommonUtils.saveUpdateForm(Constants.APPID_MASTER_STP,"location_setup","", locData); 
        }

        HashMap locHm = new HashMap();
        locHm.put("country_id", country_id);
        locHm.put("state_id", state_id);
        locHm.put("city_id", city_id);

        return locHm;
    }

    private String getCountryID(DBHandler db, String country) {
        String query = "SELECT * FROM app_fd_stp_country WHERE c_country = ?";
        String[] cond = {country};

        HashMap<String, String> q_result = db.selectOneRecord(query, cond);

        if (q_result != null) {
            return q_result.get("id");
        }

        String iso = getCountryISO(country);

        HashMap counHm = new HashMap();
        counHm.put("country", country);
        counHm.put("status", "Active");
        counHm.put("iso_code", iso);
        return CommonUtils.saveUpdateForm2(Constants.APP_ID.MASTER_STP, "country", "", counHm);
        // TODO get country code??
    }

    public String getStateID(DBHandler db, String state, String country_id) {
        String query = "SELECT * FROM app_fd_stp_state WHERE c_state LIKE '%" + state + "%'";

        HashMap<String, String> q_result = db.selectOneRecord(query);

        if (q_result != null) {
            return q_result.get("id");
        }

        HashMap counHm = new HashMap();
        counHm.put("state", state);
        counHm.put("country", country_id);
        counHm.put("status", "Active");
        return CommonUtils.saveUpdateForm2(Constants.APP_ID.MASTER_STP, "state", "", counHm);

    }

    public String getCityID(DBHandler db, String city, String state_id) {
        String query = "SELECT * FROM app_fd_stp_city WHERE c_city like '%" + city + "%'";

        HashMap<String, String> q_result = db.selectOneRecord(query);

        if (q_result != null) {
            return q_result.get("id");
        }

        HashMap counHm = new HashMap();
        counHm.put("state", state_id);
        counHm.put("city", city);
        counHm.put("status", "Active");
        return CommonUtils.saveUpdateForm2(Constants.APP_ID.MASTER_STP, "city", "", counHm);
    }

    private String getCountryISO(String country) {

        JSONObject codeJO = null;
        String code = "";

        HttpUtil http = new HttpUtil();
        
        try {
            String url = "https://restcountries.com/v3.1/name/" + URLEncoder.encode(country, "UTF-8") + "?fields=cca2";
            codeJO = http.sendGetRequest(url);
            LogUtil.info("MS Location Upload", "Country " + country + " json: " + codeJO.toString());
            if (codeJO != null) {
                JSONArray ja = codeJO.getJSONArray("data");
                JSONObject jo = (JSONObject) ja.get(0);
                code = jo.optString("cca2");
            }

        } catch (IOException ex) {
            Logger.getLogger(this.getClassName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(this.getClassName()).log(Level.SEVERE, null, ex);
        } catch (KeyManagementException ex) {
            Logger.getLogger(this.getClassName()).log(Level.SEVERE, null, ex);
        } catch (KeyStoreException ex) {
            Logger.getLogger(this.getClassName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            Logger.getLogger(this.getClassName()).log(Level.SEVERE, null, ex);
        }

        return code;
    }

    private HashMap getMycoid(DBHandler db, String hq_no) {
        String query = "SELECT id, r.c_hrdc_no, r.c_mycoid, (\n"
                + "(SELECT COUNT(id) FROM app_fd_empm_branch b WHERE b.c_hqId = ? ) \n"
                + "+\n"
                + "(SELECT COUNT(id) FROM app_fd_empm_reg b WHERE b.c_branch_mycoid = r.c_mycoid) \n"
                + ")\n"
                + "AS branch_count\n"
                + "FROM app_fd_empm_reg r WHERE r.id = ?;";

        HashMap data = db.selectOneRecord(query, new String[]{hq_no, hq_no});

        if (data != null) {
            return data;
        }

        return null;
    }
    
    private boolean checkName(DBHandler db, String id, String name) {
        boolean found = false;
        String query = "SELECT h.* FROM app_fd_empm_branch h "
                + "INNER JOIN app_fd_empm_reg r ON r.id = h.c_hqId "
                + "WHERE r.id = ? AND h.c_name = ? ";
        
        HashMap data = db.selectOneRecord(query, new String[]{id, name});
        
        if (data != null) {
            found = true;
        }
        
        return found;
    }

}
