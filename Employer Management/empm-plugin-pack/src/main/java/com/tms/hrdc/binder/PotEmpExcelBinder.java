/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.dao.CurrentUser;
import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.dao.TempPotEmp;
import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.CommonUtils;
import static com.tms.hrdc.util.CommonUtils.set_DT_DateReformatYYYYMMDD;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
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
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.PluginThread;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FileUtil;
import org.joget.apps.form.service.FormService;

/**
 *
 * @author faizr
 */
public class PotEmpExcelBinder extends WorkflowFormBinder{
    
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
        return "Used in Employer Management to import data from external files";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Potential Employer Data Impport Binder";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }
    
    @Override
    public String getPropertyOptions() {
       return "";
    }    
    
    String unsuccessfulReason = "</ul>";
    int unsuccessfulRowsUploaded = 0; 
    int successfulRowsUploaded = 0;
    int totalRows = 0;
    
    public String IS_COMPANY_EXIST = "is_comp_exist";
    public String IS_INSERT = "is_insert";
    public String H_ISEXIST_REASON = "is_exist_reason";
    public String H_BATCH = "is_exist_reason";
    public String H_STATUS = "is_exist_reason";
    public String H_MYCOID = "mycoid";
    public String H_DUPL_EMPID = "dupl_empid";

    public String MSG_UPL_IN_PROGRESS = "File Uploading In Progress";
    public String MSG_UPL_COMPLETE = "File Uploading Complete";
    public String MSG_UPL_ERROR = "Error Encountered";
    public String MSG_UPL_STOPPED = "File Uploading Stopped";
    public static String MSG_DELETED = "Deletion in progress";

    public void msg(String msg){
        LogUtil.info(this.getClassName(), msg);
    }

    class StopUploadException extends Exception
    {
        public StopUploadException(String message)
        {
            super(message);
        }
    }
    
    @Override
    public FormRowSet store(Element element, FormRowSet rowSet, FormData formData) {
        msg("Starting excel binder plugin");
        FormRowSet rows = super.store(element, rowSet, formData);
        
        final FormRowSet copyRowSet = rows;
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        FormDefinitionDao formDefinitionDao = (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");
        FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
        FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        
        final String upload_Id = rows.get(0).getId();
        final String template_id = (String) rows.get(0).get("template_set");
        final String uploader = new CurrentUser().getFullName();
               
        final HashMap appUtilhm = new HashMap();
        appUtilhm.put("appService", appService);
        appUtilhm.put("formDefinitionDao", formDefinitionDao);
        appUtilhm.put("formService", formService);
        appUtilhm.put("formDataDao", formDataDao);
        
        File file = null;
        
        FormRow row = rows.get(0);
        
        final HashMap templateHm = getTemplateData(template_id);
        final String file_type = templateHm.get("c_file_type").toString();
        final String fileName = (String) row.getCustomProperties().get("ptl_uploadData");
        
        try {
            file = FileUtil.getFile(fileName, Constants.TABLE.POT_EMP_UPLOAD.replace("app_fd_", ""), upload_Id);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        msg("File is "+file.getName()+" from "+file.getAbsolutePath());
        
        final String file_path = file.getAbsolutePath();                    
        String curr_batch_no = "<a href='"+Constants.URL.PE_UPLOAD_RESULT+"'>Click to Refresh</a>";
        String status = MSG_UPL_IN_PROGRESS, reason = "";

        if( (file_type.equals(Constants.FIELD_VALUE.POT_EMP.FILETYPE_TEXTFILE) && file_path.contains("xls"))
                || (file_type.equals(Constants.FIELD_VALUE.POT_EMP.FILETYPE_EXCEL) && file_path.contains("txt"))){
            unsuccessfulReason += "<li>Wrong file "+fileName+" for the template "+file_type+"</li></ul>";
            reason += "<li>Wrong file "+fileName+" for the template "+file_type+"</li></ul>";
            status = "Upload Failed";
        }
        
//        final String status_thread = status
                
        String curr_batch_no_in = getRefPrefix()+"/B/"
                    +CommonUtils.get_DT_CurrentDateTime("YYYY")
                    +"/"
                    +CommonUtils.getRefNo("6", "batch_counter");

//        updateUploadBatchNo(upload_Id, curr_batch_no_in);

        if(!status.equals("Upload Failed")){
        
            Thread checkingThread = new PluginThread(new Runnable(){ 
                @Override
                public void run() {                
                    msg("Main reading thread start");
                    if (copyRowSet != null && !copyRowSet.isEmpty()) {

                        try{
                            
                            ArrayList<ArrayList> data = new ArrayList();
                            
                            if(file_type.equals(Constants.FIELD_VALUE.POT_EMP.FILETYPE_EXCEL)){                      
                                Vector dataHolder = CommonUtils.ReadCSV(file_path, 1);
                                if(dataHolder.isEmpty() || dataHolder == null){
                                    msg("data is EMPTY");
                                }
//                                data = CommonUtils.VectorToArrayList(dataHolder);
                                data = VectorToArrayList2(upload_Id, dataHolder);
                            }else if(file_type.equals(Constants.FIELD_VALUE.POT_EMP.FILETYPE_TEXTFILE)){
                                String dataRaw = CommonUtils.ReadTXT(file_path);
                                data = cleanData2(upload_Id, dataRaw, 
                                            templateHm.get("c_delimiter").toString(),
                                            templateHm.get("c_linebreak").toString(),
                                            templateHm.get("c_isFirstRowHeader").toString()
                                        );
//                                 data = CommonUtils.cleanData(dataRaw, 
//                                            templateHm.get("c_delimiter").toString(),
//                                            templateHm.get("c_linebreak").toString(),
//                                            templateHm.get("c_isFirstRowHeader").toString()
//                                        );
                                if(data.isEmpty() || data == null){
                                    msg("data is EMPTY");
                                }
                            }
//                            storeData(data, upload_Id, template_id, curr_batch_no_in, uploader );
                            msg("Main reading thread - reading file complete, processing ");
                            
//                            updateTotalRowsExcel(upload_Id, data.size());
                            splitAndStore(data, upload_Id, template_id, curr_batch_no_in, uploader );

                        }catch(Exception e){
                            e.printStackTrace();
                            updateFileUploadProcess(upload_Id, MSG_UPL_ERROR+" - "+e.getMessage());
                        }
                    } 
                    
                    msg("Main reading thread end");
                }       
            });


            checkingThread.setDaemon(true);
            checkingThread.start();
        }
        
        row = rowSet.get(0);
        row.setProperty("batch", curr_batch_no_in);
        row.setProperty("total_row", "0");
        row.setProperty("total_success", "0");
        row.setProperty("total_error", "0");
        row.setProperty("error_detail", reason);
        row.setProperty("upl_status", status);
        rows.add(row);
        
        rows = super.store(element, rows, formData);
        
        return rows;
    }
    
//    private void updateUploadBatchNo(String upload_Id, String curr_batch_no_in){
//        DBHandler db = new DBHandler();
//        try {
//            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
//        
//            String query = "UPDATE app_fd_empm_pe_file_upl SET c_batch = ? WHERE id = ?";
//            db.update(query, new String[]{curr_batch_no_in}, new String[]{upload_Id});
//        
//        } catch (SQLException ex) {
//            ex.printStackTrace();
//            msg("Error updating batch refno");
//        } finally{
//            db.closeConnection();
//        }
//    }
    
    private HashMap getTemplateData(String template_id) {
        
        HashMap data = new HashMap();
        DBHandler db = new DBHandler();
        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
        
            String query = "SELECT c_file_type, c_delimiter, c_linebreak, "
                    + "c_isFirstRowHeader FROM app_fd_empm_pe_template WHERE id = ?";
            data = db.selectOneRecord(query, new String[]{template_id});
        
        } catch (SQLException ex) {
            ex.printStackTrace();
            msg("Error getting template data");
        } finally{
            db.closeConnection();
        }        
        return data;
    }
    
    public void updateFileUploadProcess(String id, String complete){
        String query = "update app_fd_empm_pe_file_upl "
                + "set dateModified = NOW(), c_total_success = ?, c_total_error = ?, c_error_detail = ?, c_upl_status = ? "
                + "where id = ?;";
         
        DBHandler db = new DBHandler();
        
        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
                        
            String[] val = {
                                String.valueOf(successfulRowsUploaded),
                                String.valueOf(unsuccessfulRowsUploaded),
                                "",
                                complete
                            };
            String[] cond = {id};
            
            int x = db.update(query, val, cond);

//            msg("Upload Complete, upload table row update: "+x);

        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            db.closeConnection();
        }
    }
    
    private void updateUploadStatus(String id, String status){
        DBHandler db = new DBHandler();
        
        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
            
            int x = db.update(
                    "update app_fd_empm_pe_file_upl "
                    + "set dateModified = NOW(),c_upl_status = ? "
                    + "where id = ?",
                    new String[]{String.valueOf(status)},
                    new String[]{id}
                 );

        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            db.closeConnection();
        }
    }
    
    private void updateTotalRowsExcel(String id, int totalRow_){
        DBHandler db = new DBHandler();
        totalRows = totalRow_;
        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
            msg("size is "+Integer.toString(totalRow_));
            int x = db.update(
                    "update app_fd_empm_pe_file_upl "
                    + "set dateModified = NOW(),c_total_row = ? "
                    + "where id = ?",
                    new String[]{String.valueOf(totalRow_)},
                    new String[]{id}
                 );
            msg("update total row "+Integer.toString(x));
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            db.closeConnection();
        }
    }
    
//    private void splitAndStore(ArrayList<ArrayList<String>> originalList, String upload_Id, String template_id, String curr_batch_no_in, String uploader) {
    private void splitAndStore(ArrayList<ArrayList> chunkList, String upload_Id, String template_id, String curr_batch_no_in, String uploader) {
        
        msg("Splitting process start");

//        int chunkSize = (int) Math.ceil(originalList.size() * 0.03); // Calculate 3% chunk size
//        List<ArrayList> smallerLists = new ArrayList<>();
//
//        for (int i = 0; i < originalList.size(); i += chunkSize) {
//            int endIndex = Math.min(i + chunkSize, originalList.size());
//            ArrayList chunk = new ArrayList(originalList.subList(i, endIndex));
//            smallerLists.add(chunk);
//        }

//        msg("Original List: " + Integer.toString(originalList.size()));
//        msg("chunk size "+Integer.toString(chunkSize));
        msg("chunk Lists (8% each): "+Integer.toString(chunkList.size()));
        for (int i = 0; i < chunkList.size(); i++) {            
            
            final int currentNo = i;
            
            Thread checkingThread = new PluginThread(new Runnable(){ 
                @Override
                public void run() {                
                    msg("process - Chunk " + currentNo + ": " + Integer.toString(chunkList.get(currentNo).size())+" start");                 
                    storeData(chunkList.get(currentNo), upload_Id, template_id, curr_batch_no_in, uploader );
                    msg("process - Chunk " + currentNo + " end unsucc-> "+Integer.toString(unsuccessfulRowsUploaded)+", succ -> "
                            +Integer.toString(successfulRowsUploaded)+", total "+Integer.toString(totalRows));
                    
                    if( (unsuccessfulRowsUploaded+successfulRowsUploaded) == totalRows ){
                        updateFileUploadProcess(upload_Id, MSG_UPL_COMPLETE);
                    }
                }       
            });

            checkingThread.setDaemon(true);
            checkingThread.start();
        }
        
        msg("Splitting process end");
    }
    
    public ArrayList VectorToArrayList2(String uploadId, Vector dataVector){
        
        msg("Original List size: " + Integer.toString(dataVector.size()));
        
        
        DataFormatter formatter = new DataFormatter();
        ArrayList rows = new ArrayList();
        
               
        
//        int chunkList_counter = 0;
        
        if(dataVector.size() > 0){
            dataVector.remove(0);
        }
        
        msg("removed header List size : " + Integer.toString(dataVector.size()));
        updateTotalRowsExcel(uploadId, dataVector.size());
        
        int chunkSize = (int) Math.ceil(dataVector.size() * 0.03); // Calculate 3% chunk size
        ArrayList<ArrayList> chunkList = new ArrayList<>();        
        ArrayList chunks = new ArrayList();
        msg("chunk size "+Integer.toString(chunkSize)); 
        
        for (int i = 0; i < dataVector.size(); i++){ //iterate thru rows
            Vector cellStoreVector = (Vector) dataVector.elementAt(i);   
            ArrayList columns = new ArrayList();
            for (int j = 0; j < cellStoreVector.size(); j++) { //iterate thru columns
                Cell myCell = (Cell) cellStoreVector.elementAt(j);
                
                String cell_value = formatter.formatCellValue(myCell); 
                cell_value = cell_value.replace("`", "");
                cell_value = set_DT_DateReformatYYYYMMDD(cell_value);
                
                columns.add(cell_value);
            }
            
            columns.add(Integer.toString(i+2));
//            rows.add(columns);
            
//            eachSmallerLists = new ArrayList();     
             
//            if(chunkList.isEmpty()){
//                chunkList.add(chunks);
//            }
            
            
            if(chunks.size() == chunkSize){
//                chunkList_counter++;
                chunks = new ArrayList();
                chunkList.add(chunks);
            }
            if(chunkList.isEmpty()){
                chunkList.add(chunks);
            }
            chunks.add(columns);   
                       
            
        }
       
        
        return chunkList;
    }
    
    public  ArrayList<ArrayList> cleanData2(String uploadId, String dataRaw, String delimiter, String linebreak, String isFirstRowHeader){
        
        String[] rowsArr = dataRaw.split(Constants.SYMBOLTABLE.get(linebreak));
        msg("Original List: " + Integer.toString(rowsArr.length));
        ArrayList rows = new ArrayList();
        
        updateTotalRowsExcel(uploadId, isFirstRowHeader.equals("true")?rowsArr.length-1:rowsArr.length);
        
        
        int chunkSize = (int) Math.ceil(rowsArr.length* 0.03); // Calculate 3% chunk size
        ArrayList<ArrayList> chunkList = new ArrayList<>();        
        ArrayList chunks = new ArrayList();
        
        msg("chunk size "+Integer.toString(chunkSize));       
        
        int chunkList_counter = 0;
        
        int rowsArr_count = 0;

        for(String row:rowsArr){
            
            if(rowsArr_count==0 && isFirstRowHeader.equals("true")){
                continue;
            }           
            
//            LogUtil.info("data row", row);
            String[] columnsArr = row.split(Constants.SYMBOLTABLE.get(delimiter));
            ArrayList columns = new ArrayList();
            for(String column:columnsArr){
                columns.add(column);
            }
            
            columns.add(Integer.toString(rowsArr_count+2));

            
//            if(chunkList.get(chunkList_counter).size() > chunkSize){
//                chunkList_counter++;
//                chunks = new ArrayList();
//            }
//            
//            chunks.add(columns);
//            chunkList.add(chunks);
            
            
            if(chunks.size() == chunkSize){
//                chunkList_counter++;
                chunks = new ArrayList();
                chunkList.add(chunks);
            }
            if(chunkList.isEmpty()){
                chunkList.add(chunks);
            }
            chunks.add(columns);   
            
//            rows.add(columns);
        }
        
//        if(isFirstRowHeader.equals("true")){
//            rows.remove(0);
//        }
        
        
        return chunkList;
    }
    
    private void storeData(ArrayList rows, String upload_Id, String template_id, String batch_no, String uploader) {
        
        DBHandler db = new DBHandler();
                
        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
                                              
            double ds_float = 0.25*totalRows;            
            int displaySize = (int) Math.floor(ds_float);
            
            TempPotEmp emp;
            
            ArrayList<String> columns_list;
            
            HashMap<String,String> columnData;
            HashMap isMyCoIDExist;
            HashMap hasInserted;
            HashMap updListhm;
            
            String col_value = "";
            String columnName = "";
            String emplId = "";
            String mapCol = "";
            
            boolean isLocFld = false;
            boolean isSectorFld = false;
            boolean isCallingNum = false;
            boolean isDate = false;
            
            ArrayList<HashMap<String,String>> columnsMapList = getColumnNameList(db, template_id);
                    
            for (int rowNum = 0; rowNum < rows.size(); rowNum++){ 
                                
                try{
                    if( displaySize>0 && rowNum%displaySize==0 ){
                        msg(" store data method - now at row "+Integer.toString(rowNum));
                    }
                    if(isStopCalled(db, upload_Id)){
                        throw new StopUploadException(MSG_UPL_STOPPED);
                    }

                    columns_list = (ArrayList) rows.get(rowNum);      
                    String row_in_excel = columns_list.get(columns_list.size() - 1);

                    emp = new TempPotEmp(db);
//                    ArrayList<HashMap<String,String>> columnsMapList = getColumnNameList(db, template_id); ####

                    for(HashMap mapSet:columnsMapList){

                        mapCol = mapSet.getOrDefault("c_col_num","").toString();
                        columnName = mapSet.getOrDefault("c_map_to_field","").toString();
                        isLocFld = mapSet.get("c_isLocationField").equals("true");
                        isSectorFld = mapSet.get("c_isSectorField")==null?false:mapSet.get("c_isSectorField").equals("true");
                        isCallingNum = mapSet.get("c_isCallNumber")==null?false:mapSet.get("c_isCallNumber").equals("true");
                        isDate = mapSet.get("c_is_date")==null?false:mapSet.get("c_is_date").equals("true");
                            
                        if(mapCol.isEmpty()){
                            continue;
                        }

                        int mapCol_int = -1;
                        try{
                            mapCol_int = Integer.parseInt(mapCol);
                        }catch(Exception e){
                            continue;
                        }
                        mapCol_int--;
                        col_value = columns_list.get(mapCol_int)==null?"":columns_list.get(mapCol_int).toString();      

                        if(isCallingNum && !col_value.startsWith("+60") && !StringUtils.isBlank(col_value)){
                            col_value = col_value.startsWith("0")?"+6"+col_value:"+06"+col_value;
                            col_value = col_value.replace("-", "").replace(" ", "");
                        }

                        boolean isNonVital = true;

                        if(columnName.equals("c_mycoid")){
                            emp.setMycoid(col_value);
                            isNonVital = false;
                        }

                        if(columnName.equals("c_comp_name")){
                            emp.setComp_name(col_value);
                            isNonVital = false;
                        }

                        // BUSINESS ADDRESS ----------------------------------------

                        if(isLocFld && columnName.contains("bu_postcode")){
                            emp.setBu_postcode(col_value);
                            isNonVital = false;
                        }

                        if(isLocFld && columnName.contains("bu_country")){      
                            emp.setBu_country(col_value);
                            isNonVital = false;
                        }

                        if(isLocFld && columnName.contains("bu_city")){
                            emp.setBu_city(col_value);
                            isNonVital = false;
                        } 

                        if(isLocFld && columnName.contains("bu_state")){
                            emp.setBu_state(col_value);
                            isNonVital = false;
                        } 

                        if(isLocFld && columnName.contains("bu_address2") && !columnName.contains("email")){
                            emp.setBu_address2(col_value);
                            isNonVital = false;
                        } 

                        if(isLocFld && columnName.contains("bu_address1") && !columnName.contains("email")){
                            emp.setBu_address1(col_value);
                            isNonVital = false;
                        }  

                        // CORRESPONDENCE ADDRESS ----------------------------------------

                        if(isLocFld && columnName.contains("empl_postcode")){
                            emp.setPostcode(col_value);
                            isNonVital = false;
                        }

                        if(isLocFld && columnName.contains("empl_country")){      
                            emp.setCountry(col_value);
                            isNonVital = false;
                        }

                        if(isLocFld && columnName.contains("empl_city")){
                            emp.setCity(col_value);
                            isNonVital = false;
                        } 

                        if(isLocFld && columnName.contains("empl_state")){
                            emp.setState(col_value);
                            isNonVital = false;
                        } 

                        if(isLocFld && columnName.contains("empl_address2") && !columnName.contains("email")){
                            emp.setAddress2(col_value);
                            isNonVital = false;
                        } 

                        if(isLocFld && columnName.contains("empl_address") && !columnName.contains("empl_address2") && !columnName.contains("email")){
                            emp.setAddress1(col_value);
                            isNonVital = false;
                        }  

                        // INDUSTRY SECTOR ------------------------------------------

                        if(isSectorFld && columnName.contains("industry_sector")){
                            emp.setSectorData(Constants.SECTOR_TYPE.INDUSTRY_SECTOR, col_value);
                            isNonVital = false;
                        }

                        if(isSectorFld && columnName.equals("c_industry_sector")){
                            emp.setSectorData(Constants.SECTOR_TYPE.DIV, col_value);      
                            isNonVital = false;         
                        }

                        if(isSectorFld && columnName.equals("c_main_sector_code")){
                            emp.setSectorData(Constants.SECTOR_TYPE.MAIN_SECTOR_CODE, col_value);
                            isNonVital = false;
                        }

                        if(isSectorFld && columnName.equals("c_class_code")){
                            emp.setSectorData(Constants.SECTOR_TYPE.CLASS_SECTOR, col_value);
                            isNonVital = false;
                        }

                        if(isSectorFld && columnName.equals("c_sector_code") && !columnName.contains("main")){
                            emp.setSectorData(Constants.SECTOR_TYPE.SUB_SECTOR_CODE, col_value);
                            isNonVital = false;
                        }

                        if(columnName.equals("c_empl_email_pri")){
                            emp.setComp_email(col_value);
                            isNonVital = false;
                        }

                        if(columnName.equals("c_empl_email_pic_pri")){
                            emp.setPrimary_contact_email(col_value);
                            isNonVital = false;
                        }

                        if(columnName.equals("c_empl_tel_no_pri")){
                            emp.setPrimary_contact_telno(col_value);
                            isNonVital = false;
                        }

                        if(isNonVital){
                            emp.insertData(columnName, col_value);
                        }
                    }

                    //default value for some fields..
                    emp.setOrg_type("ROC");
                    emp.setReg_type("HQ");

                    boolean successInsert = false;

                    if(displaySize>0 && rowNum%displaySize==0){
                        msg(" store data method - Data "+emp.getMycoid()+" - "+emp.getComp_name());
                    }

                    String unsuccessfulReason = "";

//                    isMyCoIDExist = isMyCoIDExist(db, upload_Id, emp);
                    hasInserted = isMycoidAlreadyInserted(db, upload_Id, emp);
                    ArrayList<HashMap<String, String>> oldMycoids = isMyCoIDExistInOtherUploadedBatch(db, upload_Id,  emp );
                    HashMap alreadyInSystem = isMycoidInSystem(db, emp.getMycoid());

//                    String existingMycoidFromOtherBatch = isMyCoIDExistInOtherUploadedBatch(db, upload_Id,  emp );

                    if(emp.getMycoid().isEmpty()){
//                        unsuccessfulReason = "(row "+Integer.toString(rowNum+2)+") Missing MycoID";
                        unsuccessfulReason = "(row "+row_in_excel+") Missing MycoID";
                    }
                    else if(emp.getComp_name().isEmpty()){
//                        unsuccessfulReason = "(row "+Integer.toString(rowNum+2)+") Missing Company Name";
                        unsuccessfulReason = "(row "+row_in_excel+") Missing Company Name";
                    }
                    else if(alreadyInSystem.getOrDefault(IS_COMPANY_EXIST, "").equals("TRUE")){
//                        unsuccessfulReason = "(row " + Integer.toString(rowNum + 2) + ") MyCoID Already In The System, Status: "
//                                + hasInserted.getOrDefault(H_ISEXIST_REASON, "").toString();
                        unsuccessfulReason = "MyCoID "+emp.getMycoid()+" Already In The System, Status: "
                                + alreadyInSystem.getOrDefault(H_ISEXIST_REASON, "").toString();
                    }
                    else if(hasInserted.get(IS_COMPANY_EXIST).toString().toUpperCase().equals("TRUE")) {
                        unsuccessfulReason = hasInserted.getOrDefault(H_ISEXIST_REASON, "").toString();

                        if(hasInserted.get(IS_INSERT).toString().toUpperCase().equals("TRUE")) {
                            
                            successInsert = true;
                            emplId = emp.saveData();
                        }
                    }
                    // merge with other batch
                    else if(oldMycoids.size()>0){

                        String batchesWord = "";

                        for(HashMap<String, String> oldMycoid:oldMycoids){

                            String batch = oldMycoid.getOrDefault("batch","");
                            String tempId = oldMycoid.getOrDefault("emp_temp_id","");
                            String dataId = oldMycoid.getOrDefault("upl_data_id","");

//                            emplId = emp.saveData(tempId);
//                            db.delete(
//                                    "DELETE FROM app_fd_empm_pe_upl_data WHERE id = ?",
//                                    new String[]{dataId}
//                            );
                            batchesWord+="<li><b>"+batch+"</b></li>";
                        }

                        emp.saveData();
                        successInsert = true;

                        new AuditTrailUtil().insertAuditTrail2(db, emplId,
                                uploader, "Employer data from upload batch <ul>"+batchesWord+"</ul> with newly uploaded data", "",
                                true, new ArrayList());


//                        unsuccessfulReason = "(row " + Integer.toString(rowNum + 2) + ") MyCoID "
                        unsuccessfulReason = "(row " + row_in_excel + ") MyCoID "
                                +emp.getMycoid()
                                +" found in batch <ul>"+batchesWord+"</ul> and merged into current batch";
                    }
                    // best case
                    else {
                        emplId = emp.saveData();
                        successInsert = true;                        

                        new AuditTrailUtil().insertAuditTrail2(db, emplId,
                                uploader, "Potential Employer Data Uploaded", "",
                                true, new ArrayList());

                    }

                    updListhm = new HashMap();

                    if(successInsert){ 
                        updListhm.put("status", "SUCCESS");
                        successfulRowsUploaded++;
                    }else{
                        updListhm.put("status", "ERROR");
                        unsuccessfulRowsUploaded++;
                    }
                    
                    updateFileUploadProcess(upload_Id, MSG_UPL_IN_PROGRESS);

                    updListhm.put("error_detail", unsuccessfulReason);
                    updListhm.put("batch", upload_Id);     
                    updListhm.put("emp_fk", emplId);                
                    updListhm.put("isPotEmp", "false");
//                    updListhm.put("rowInFile", Integer.toString(rowNum+2));
                    updListhm.put("rowInFile", row_in_excel);
                    updListhm.put("modifiedByName", uploader);
                    updListhm.put("createdByName", uploader);

                    String id = CommonUtils.saveUpdateForm2("",
                            Constants.FORM_ID.POTEMP_UPLOADED_ITM_FORM,"", 
                            updListhm);   
                    
//                    msg("ITEM "+updListhm);

                }catch(StopUploadException e){
                    e.printStackTrace();

                    updateFileUploadProcess(upload_Id, e.getMessage());
                    break;
                }catch(Exception e){
                    e.printStackTrace();

                    updateFileUploadProcess(upload_Id, MSG_UPL_ERROR+" - "+e.getMessage());
                    db.closeConnection();
                    break;
                }
            }            

        }catch(Exception e){
            e.printStackTrace();
            
            updateFileUploadProcess(upload_Id, MSG_UPL_ERROR+" - "+e.getMessage());
        } finally {
            db.closeConnection();
        } 
    }

    private HashMap isMycoidInSystem(DBHandler db, String mycoid) {
        String status = "";
        HashMap hm = db.selectOneRecord(
                "SELECT * FROM "+Constants.TABLE.EMPREG+" WHERE c_mycoid = '"+mycoid+"' "
//                new String[]{mycoid}
        );

        boolean exist = false;

        if(hm!=null){
            exist = true;
            status = hm.getOrDefault("c_last_move", "").toString();
        }

        HashMap check = new HashMap();
        check.put(IS_COMPANY_EXIST,Boolean.toString(exist).toUpperCase());
        check.put(H_ISEXIST_REASON,status);

        return check;
    }

    public boolean isStopCalled(DBHandler db, String uplId){
        String stop_upload = db.selectOneValueFromTable(
                "SELECT c_stop_upload FROM app_fd_empm_pe_file_upl WHERE id = ?",
                new String[]{uplId}
        );

        boolean isStop = false;

        if(!StringUtils.isBlank(stop_upload) && stop_upload.equals("TRUE")){
            isStop = true;
        }

        return isStop;
    }
    
    public void buildReason(String reason){
        if(unsuccessfulReason.isEmpty()){
            unsuccessfulReason = reason;
        }else{
            unsuccessfulReason += "<br />"+reason;
        }
    }
    
    private ArrayList<HashMap<String, String>> getTemplateHm(DBHandler db, String template_id) {
        String query = "SELECT * FROM app_fd_empm_pe_fld_map WHERE c_fk = ?";
        
        return db.select(query, new String[]{template_id});
    }

    private HashMap getColumnName(DBHandler db, int pos, String template_id) {
        String query = "SELECT c_map_to_field, "
                + "(SELECT c_isLocationField FROM app_fd_empm_keywords WHERE c_columnID = c_map_to_field limit 1) as c_isLocationField, "
                + "(SELECT c_isSectorField FROM app_fd_empm_keywords WHERE c_columnID = c_map_to_field limit 1) as c_isSectorField, "
                + "(SELECT c_isCallNumber FROM app_fd_empm_keywords WHERE c_columnID = c_map_to_field limit 1) as c_isCallNumber, "
                + "(SELECT c_is_date FROM app_fd_empm_keywords WHERE c_columnID = c_map_to_field limit 1) as c_is_date "
                + "FROM app_fd_empm_pe_fld_map WHERE c_fk = ? AND c_col_num = ?";
        HashMap data =db.selectOneRecord(query, new String[]{template_id, Integer.toString(pos)});
        return data;
    }
    
    private ArrayList<HashMap<String, String>> getColumnNameList(DBHandler db,String template_id) {
        String query = "SELECT c_map_to_field, c_col_num, \n" +
                        "(SELECT c_isLocationField FROM app_fd_empm_keywords WHERE c_columnID = c_map_to_field limit 1) as c_isLocationField, \n" +
                        "(SELECT c_isSectorField FROM app_fd_empm_keywords WHERE c_columnID = c_map_to_field limit 1) as c_isSectorField, \n" +
                        "(SELECT c_isCallNumber FROM app_fd_empm_keywords WHERE c_columnID = c_map_to_field limit 1) as c_isCallNumber, \n" +
                        "(SELECT c_is_date FROM app_fd_empm_keywords WHERE c_columnID = c_map_to_field limit 1) as c_is_date \n" +
                        "FROM app_fd_empm_pe_fld_map WHERE c_fk = ? \n" +
                        "order by c_col_num+0 asc";
        ArrayList<HashMap<String, String>> data =db.select(query, new String[]{template_id});
        return data;
    }
    
    private HashMap isMycoidAlreadyInserted(DBHandler db, String uploadId, TempPotEmp emp){
        String mycoid = emp.getMycoid();
        
        if(mycoid.isEmpty()){
            return null;
        }
        
        boolean insert = true;
        boolean exist = false;
        String reason = "";
        
        //check if in the same upload table
        String query = "SELECT a.c_rowInFile, b.id empId FROM app_fd_empm_pe_upl_data a \n" +
                        "inner join app_fd_empm_pe_file_upl u on u.id = a.c_batch\n" +
                        "INNER JOIN app_fd_empm_reg_temp b on a.c_emp_fk = b.id\n" +
                        "WHERE u.id = ? \n" +
                        "and b.c_mycoid = ? ";
        
        ArrayList<HashMap<String, String>> hmList = db.select(query, new String[]{uploadId, mycoid.trim()});
        String duplicate_mycoids = "";
        
        for(HashMap hm:hmList){
            String rowInFile = hm.get("c_rowInFile")==null?"0":hm.get("c_rowInFile").toString();            
            String empId = hm.get("empId")==null?"0":hm.get("empId").toString();


            HashMap sameValueshm = emp.checkIfAllDataSame(new TempPotEmp(db, empId), rowInFile);

            String isAllSame = sameValueshm.getOrDefault("IS_ALL_VALUE_SAME", "false").toString();

            if(isAllSame.equals("true")){
                duplicate_mycoids+= (!duplicate_mycoids.isEmpty()?","+rowInFile:rowInFile);
                reason+= " <li> Found Duplicate Mycoid at row "+rowInFile+" with Same Values </li>";
                insert = false;
            }else{
                reason+= " <li> Found Duplicate Mycoid at row "+rowInFile+" with Different Values "+sameValueshm.getOrDefault("DIFF_VALUES","") +"</li>";
            }

            exist = true;
        }

        if(!reason.isEmpty()){
            reason="<ol>"+reason+"</ol>";
        }
        
        HashMap returnHm = new HashMap();
        returnHm.put(H_ISEXIST_REASON, reason);
        returnHm.put(IS_COMPANY_EXIST, Boolean.toString(exist).toUpperCase());
        returnHm.put(IS_INSERT, Boolean.toString(insert).toUpperCase());

        return returnHm;
    }

    private HashMap isMyCoIDExist(DBHandler db, String uploadId, TempPotEmp emp) {
        
        String mycoid = emp.getMycoid();
        String comp_name = emp.getComp_name();            
        
        HashMap returnHm = new HashMap();
        
        if(mycoid.isEmpty()){
            return null;
        }
        
        boolean exist = false;
        String reason = "";
        String dupl_empId = "";
        
        // ----------------------------------------------------------------------
        EmpmObj eo = new EmpmObj(db,EmpmObj.BY_MYCOID, mycoid.trim());

        if(!eo.getStatus().isEmpty()){
            exist = true;
            reason = eo.getStatus();
            dupl_empId = eo.getDuplEmpId();
        }
        
        // ----------------------------------------------------------------------
        
        returnHm.put(IS_COMPANY_EXIST, Boolean.toString(exist).toUpperCase());
        
        if(exist){
            returnHm.put(H_ISEXIST_REASON, reason);
            returnHm.put(H_MYCOID, mycoid);
            returnHm.put(H_DUPL_EMPID , dupl_empId);            
        }  
        
        return returnHm;
    }

    private ArrayList<HashMap<String,String>> isMyCoIDExistInOtherUploadedBatch(DBHandler db, String uploadId, TempPotEmp emp) {

        String mycoid = emp.getMycoid();
        String comp_name = emp.getComp_name();

        HashMap returnHm = new HashMap();

        if(mycoid.isEmpty()){
            return null;
        }

        String batch = "";
        String tempId = "";
        String dataId = "";
        String batchesWord = "<b>";

        ArrayList<HashMap<String,String> > uploadedMycoid = db.select(
                "select distinct u.c_batch batch, t.id emp_temp_id, d.id upl_data_id from app_fd_empm_reg_temp t\n" +
                    "INNER JOIN app_fd_empm_pe_upl_data d ON d.c_emp_fk = t.id\n" +
                    "INNER JOIN app_fd_empm_pe_file_upl u ON d.c_batch = u.id\n" +
                    "where d.c_batch != ?  and t.c_mycoid = ? order by u.dateCreated desc",
                new String[]{uploadId,mycoid}
        );

        return uploadedMycoid;
    }

    private String getRefPrefix() {
        DBHandler db = new DBHandler();
        String prefix = "";
        
        try{
            db.openConnection((DataSource)AppUtil.getApplicationContext().getBean("setupDataSource"));
        
            HashMap hm = db.selectOneRecord("SELECT c_pe_batch_refCode FROM app_fd_empm_reg_stp WHERE id = ?", 
                    new String[]{Constants.DATA_ID.MAIN_SETUP_ID});
            
            prefix = hm!=null?hm.get("c_pe_batch_refCode").toString():"";
        }catch(Exception e){
        }finally{
            db.closeConnection();
        }
        
        return prefix;
    }
    
}
