/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.KeywordDictionary;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.service.FileUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;

/**
 *
 * @author faizr
 */
public class FormManagerTool extends DefaultApplicationPlugin{

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
        return "To persist data for submitted Form1/Form1A";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Submission Form Archiver";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return
                "[{\n" +
                "    title : '"+getLabel()+"',\n" +
                "    properties : [{\n" +
                "            name:\"formType\",\n" +
                "            label: \"Form\",\n" +
                "            type:\"SelectBox\",\n" +
                "            required : \"true\",            \n" +
                "            options : [\n" +
                "                {value: 'form1', label : 'Form 1'},\n" +
                "                {value: 'form1A', label : 'Form 1A'},\n" +
                "                {value: 'form4', label : 'Form 4'},\n" +
                "                {value: 'form5', label : 'Form 5'}\n" +
                "            ]\n" +
                "        },{" +
                "            name:\"action\",\n" +
                "            label: \"Action\",\n" +
                "            type:\"SelectBox\",\n" +
                "            required : \"true\",            \n" +
                "            options : [\n" +
                "                {value: 'update', label : 'UPDATE'},\n" +
                "                {value: 'insert', label : 'INSERT_'},\n" +
                "                {value: 'saveDataOnComplete', label : 'Save Employer Data (On Approved)'}\n" +
                "            ]\n" +
                "    }] " +
                "}]";
    }

//    private void saveTempData(EmpmObj eo) {
//        String prefix = "c_";
//        String tempId = eo.getId();
//  
//        HashMap newHm = new HashMap();        
//        Iterator hmIterator = eo.getEmpData().entrySet().iterator();        
//        while (hmIterator.hasNext()) { 
//            Map.Entry hm
//                = (Map.Entry)hmIterator.next();        
//            String fieldName = hm.getKey().toString();
//            
//            if(fieldName.equals("dateCreated") || fieldName.equals("dateModified")){
//                continue;
//            }
//            
//            if(hm.getKey().toString().startsWith("c_")){
//                fieldName = fieldName.substring(prefix.length());
//            }
//            newHm.put(fieldName, hm.getValue());
//        }          
//        
//        tempId = CommonUtils.saveUpdateForm2("",
//                Constants.FORM_ID.EMP_TEMP_DATA,tempId, newHm);
//    }


    public class FORMTYPE{
        final static String FORM1 = "form1";
        final static String FORM1A = "form1A";
        final static String FORM4 = "form4";
        final static String FORM5 = "form5";
        final static String SAVEEMPLOYER_DATAONCOMPLETE = "saveDataOnComplete";
    }

    @Override
    public Object execute(Map props) {
        WorkflowAssignment wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        String id = appService.getOriginProcessId(wfAssignment.getProcessId());

        DBHandler db = new DBHandler();

        try{
            db.openConnection();

            //get refno
            String refno = "";
            String modifiedBy = "";
            String formType = props.get("formType").toString();
            String action = props.get("action").toString();
            String empId = "";
            
            EmpmObj eo;
            HashMap data = new HashMap();
            LogUtil.info("form type form manager: ",formType);
            switch(formType){
                case FORMTYPE.FORM4:
                    data = db.selectOneRecord(
                            "SELECT * FROM "+Constants.TABLE.DEREG+" WHERE id = ? ",
                            new String[]{id}
                    );
                    modifiedBy = data.get("createdByName").toString();
                    refno = data.get("c_dereg_refno").toString();
                    empId = data.get("c_dreg_emp_id").toString();
                    
                    eo = new EmpmObj(db, EmpmObj.BY_ID, empId);
                    break;
                case FORMTYPE.FORM5:
                    
                    data = db.selectOneRecord(
                            "SELECT createdByName, c_merge_comp_id, c_dereg_refno FROM "+Constants.TABLE.DEREG+" WHERE id = ? ",
                            new String[]{id}
                    );
                    
                    String f5_id = data.get("c_f5_fk").toString();                    
                    modifiedBy = data.get("createdByName").toString();
                    empId = data.get("c_merge_comp_id").toString();
                    refno = data.get("c_dereg_refno").toString();
                    
                    data = db.selectOneRecord(
                            "SELECT * FROM "+Constants.TABLE.DEREG_F5+" WHERE id = ? ",
                            new String[]{f5_id}
                    );
                    eo = new EmpmObj(db, EmpmObj.BY_ID, empId);
                    break;
                default:
                    data = db.selectOneRecord(
                            "SELECT * FROM "+Constants.TABLE.EMPREG_APPL+" WHERE id = ? ",
                            new String[]{id}
                    );
                    modifiedBy = data.get("createdByName").toString();
                    refno = data.get("c_ref_no").toString();
                    
                    id = CommonUtils.getEmpId_empReg(db, id);
                    eo = new EmpmObj(db, EmpmObj.BY_ID, id);
                    data = eo.getEmpData();
            }

            HashMap tblHm = getArcProp(db, formType);

            String tbl = tblHm.get("TABLE").toString();
            LogUtil.info("form table form manager: ",tbl);
            ArrayList colsList = (ArrayList) tblHm.get("COLUMNS");
            String formArcId = db.selectOneValueFromTable(
                    "SELECT id FROM "+tbl+" WHERE c_fk = ?", new String[]{id} //if for update
            );
            
            switch(action){
                case FORMTYPE.SAVEEMPLOYER_DATAONCOMPLETE:
                    //redundant but needed to get old data when updating the company profile later
                    AuditTrailUtil.saveTempData(db, eo);
                    break;
                case "update":
                    updateArchiveForm(db, modifiedBy, data, formArcId,
                            tbl, colsList);
                    
                    //redundant but needed to get old data when updating the company profile later
                    AuditTrailUtil.saveEmpCacheData(db, eo);
//                    AuditTrailUtil.saveTempData(db, eo);
                    break;
                case "insert":
                    formArcId = saveToArchiveForm(db, data, refno, tbl, colsList);
                    if(formType.equals(FORMTYPE.FORM1) || formType.equals(FORMTYPE.FORM1A)){
                        copySubTbl(db, id, formArcId);
                        copySubTblAndFiles(db, id, formArcId);
                    }

                    if(formType.equals(FORMTYPE.FORM4)){
                        copyDeregSubTblAndFiles(db, id, formArcId);
                    }

                    if(formType.equals(FORMTYPE.FORM5)){
                        copyMergerSubTblAndFiles(db, id, formArcId);
                    }
                    
                    //redundant but needed to get old data when updating the company profile later
//                    AuditTrailUtil.saveTempData(db, eo);
                    AuditTrailUtil.saveEmpCacheData(db, eo);
                    break;
            }

        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }

        return null;
    }

    public HashMap getArcProp(DBHandler db, String formType){
        String table = "";

        switch(formType){
            case FORMTYPE.FORM1:
                table = Constants.TABLE.SUBMIT_TABLE_FORM1;
                break;
            case FORMTYPE.FORM1A:
                table = Constants.TABLE.SUBMIT_TABLE_FORM1A;
                break;
            case FORMTYPE.FORM4:
                table = Constants.TABLE.SUBMIT_TABLE_FORM4;
                break;
            case FORMTYPE.FORM5:
                table = Constants.TABLE.SUBMIT_TABLE_FORM5;
                break;
        }

        HashMap hm = new HashMap();
        hm.put("TABLE", table);
        hm.put("COLUMNS", getFormColumnList(db, table));

        return hm;
    }

    public void updateArchiveForm(DBHandler db, String modifiedBy, HashMap data, String arcId, String table, ArrayList<HashMap<String, String>> columnsArray){

        String col = "";

        ArrayList chgeList = new ArrayList();
        ArrayList<String> valueList = new ArrayList();

        HashMap newData = db.selectOneRecord("SELECT * FROM "+table+" WHERE id = ?", new String[]{arcId});

        for(HashMap colHm:columnsArray){
            String colName = colHm.get("COLUMN_NAME").toString();
            String value = "";

            if(colName.equals("id")
                    || colName.equals("c_fk")
                    || !data.containsKey(colName)){
                continue;
            }else{
                value = data.get(colName)==null?"":data.get(colName).toString(); //new data
                String oldVal = newData.getOrDefault(colName, "").toString();
                //audit trail
                chgeList = AuditTrailUtil.buildChangeAuditHm(db, colName, value, oldVal, chgeList, "");
            }

            col += (col.isEmpty()?colName:", "+colName)+"=?";
            valueList.add(value);
        }

        String[] valueArr = valueList.toArray(new String[valueList.size()]);
        String query = "UPDATE "+table+" SET " + col +" WHERE id = ?";
        int i = db.update(query, valueArr, new String[]{arcId});

        //insert changes trail
        insertQueryAuditTrail(
                db, chgeList,
                data.get("id").toString(),
                modifiedBy,
                (data.containsKey("c_q_remarks")?
                        data.getOrDefault("c_q_remarks", "").toString():
                        "")
        );

        LogUtil.info(this.getClassName(), "Archive form result update: "+Integer.toString(i));

    }
    
    
    
    private String saveToArchiveForm(DBHandler db, HashMap data,String refno, String table, ArrayList<HashMap> colArr) {

        String col = "c_ref_no";
        String val = "'"+refno+"'";
        String id = "";
        String archiveForm = "";

        ArrayList<HashMap<String, String>> arcArr = new ArrayList();

        HashMap<String, String> arcData = new HashMap();

        for(HashMap colHm:colArr){
            String colName = colHm.get("COLUMN_NAME").toString();
            String value = "";

            if(colName.equals("id")){
//                id = UuidGenerator.getInstance().getUuid();
//                value = id;
                continue;
            }else if(colName.equals("c_fk")){
                value = (String) data.get("id");
            }else if(colName.equals("c_ref_no")){
                value = refno;
            }else if(!data.containsKey(colName)){
                continue;
            }else{
                value = data.get(colName)==null?"":data.get(colName).toString();
            }

            colHm.put("COLUMN_VALUE", value);

            if(colName.startsWith("c_")){
                colName = colName.replaceFirst("c_", "");
            }

            arcData.put(colName, value);
        }

        if(!arcData.keySet().stream()
                .anyMatch(k -> k.equals("c_ref_no") )
                && (table.equals(Constants.TABLE.SUBMIT_TABLE_FORM1A)
                || table.equals(Constants.TABLE.SUBMIT_TABLE_FORM1)   )
        )
        {
            archiveForm = Constants.FORM_ID.ARCHIVE_FORM1;
            arcData.put("c_ref_no", refno);
        }

        if(!arcData.keySet().stream()
                .anyMatch(k -> k.equals("c_dereg_refno"))
                && table.equals(Constants.TABLE.SUBMIT_TABLE_FORM5) )
        {
            archiveForm = Constants.FORM_ID.ARCHIVE_FORM5;
            arcData.put("c_dereg_refno", refno);
        }

        if(!arcData.keySet().stream()
                .anyMatch(k -> k.equals("c_dereg_refno"))
                && table.equals(Constants.TABLE.SUBMIT_TABLE_FORM4)  )
        {
            archiveForm = Constants.FORM_ID.ARCHIVE_FORM4;
            arcData.put("c_dereg_refno", refno);
        }

        id = CommonUtils.saveUpdateForm2("", archiveForm, "", arcData);

        LogUtil.info(this.getClassName(), "Archive form result insert: "+id);

        return id;
    }

    private void copySubTbl(DBHandler db, String oriId, String formArcId) {

        int i = db.insert(
                "INSERT INTO "+Constants.TABLE.EMP_OTHER_CONTACT_DETAILS+" ("
                        + "id, dateCreated, dateModified, modifiedBy, modifiedByName, "
                        + "c_fk, c_tel_no, c_name, c_designation, c_email"
                        + ") "
                        + "SELECT "
                        + "UUID(), dateCreated, dateModified, modifiedBy, modifiedByName, "
                        + "?, c_tel_no, c_name, c_designation, c_email "
                        + "FROM "+Constants.TABLE.EMP_OTHER_CONTACT_DETAILS+" WHERE c_fk = ?",
                new String[]{formArcId, oriId}
        );
        LogUtil.info(this.getClassName(), "Archive subform result insert: "+Integer.toString(i));
    }

    private void copySubTblAndFiles(DBHandler db, String oriId, String formArcId) throws IOException {

        ArrayList<HashMap<String, String>> filesList = db.select(
                "SELECT * FROM "+Constants.TABLE.DOCS+" WHERE c_fk = ?",
                new String[]{oriId});

        HashMap hm = new HashMap();

        for(HashMap files: filesList){
            hm = new HashMap();

            hm.put("modifiedBy", files.get("modifiedBy").toString());
            hm.put("modifiedByName", files.get("modifiedByName").toString());
            hm.put("file", files.get("c_file").toString());
            hm.put("descr", files.get("c_descr").toString());
            hm.put("fk", formArcId);

            String duplId = CommonUtils.saveUpdateForm2("",
                    Constants.FORM_ID.EMP_REG_SUBFORM_DOCS,
                    "",
                    hm
            );

            CommonUtils.duplicateDir(files.get("id").toString(),
                    duplId, Constants.TABLE.DOCS);

            File file = FileUtil.getFile(
                    files.get("c_file").toString(),
                    Constants.TABLE.DOCS.replace("app_fd_", ""),
                    duplId);

            if(file.exists() && !file.isDirectory()) {
                LogUtil.info(this.getClassName(),
                        duplId+" file "+file.getName()+" exist at "+file.getAbsolutePath());
            }
        }

        filesList = db.select(
                "SELECT * FROM "+Constants.TABLE.DOCS+" WHERE c_fk_doc_a = ?",
                new String[]{oriId});

        for(HashMap files: filesList){
            hm = new HashMap();

            hm.put("modifiedBy", files.get("modifiedBy").toString());
            hm.put("modifiedByName", files.get("modifiedByName").toString());
            hm.put("file", files.get("c_file").toString());
            hm.put("descr", files.get("c_descr").toString());
            hm.put("fk_doc_a", formArcId);

            String duplId = CommonUtils.saveUpdateForm2("",
                    Constants.FORM_ID.EMP_REG_SUBFORM_DOCS,
                    "",
                    hm
            );

            CommonUtils.duplicateDir(files.get("id").toString(),
                    duplId, Constants.TABLE.DOCS);

            File file = FileUtil.getFile(
                    files.get("c_file").toString(),
                    Constants.TABLE.DOCS.replace("app_fd_", ""),
                    duplId);

            if(file.exists() && !file.isDirectory()) {
                LogUtil.info(this.getClassName(),
                        duplId+" file "+file.getName()+" exist at "+file.getAbsolutePath());
            }
        }

        filesList = db.select(
                "SELECT * FROM "+Constants.TABLE.DOCS+" WHERE c_fk_doc_b = ?",
                new String[]{oriId});

        for(HashMap files: filesList){
            hm = new HashMap();

            hm.put("modifiedBy", files.get("modifiedBy").toString());
            hm.put("modifiedByName", files.get("modifiedByName").toString());
            hm.put("file", files.get("c_file").toString());
            hm.put("descr", files.get("c_descr").toString());
            hm.put("fk_doc_b", formArcId);

            String duplId = CommonUtils.saveUpdateForm2("",
                    Constants.FORM_ID.EMP_REG_SUBFORM_DOCS,
                    "",
                    hm
            );

            CommonUtils.duplicateDir(files.get("id").toString(),
                    duplId, Constants.TABLE.DOCS);

            File file = FileUtil.getFile(
                    files.get("c_file").toString(),
                    Constants.TABLE.DOCS.replace("app_fd_", ""),
                    duplId);

            if(file.exists() && !file.isDirectory()) {
                LogUtil.info(this.getClassName(),
                        duplId+" file "+file.getName()+" exist at "+file.getAbsolutePath());
            }
        }
    }

    private void copyDeregSubTblAndFiles(DBHandler db, String oriId, String formArcId) throws IOException {

        ArrayList<HashMap<String, String>> filesList = db.select(
                "SELECT * FROM "+Constants.TABLE.DREG_FILE+" WHERE c_fk_doc_a = ?",
                new String[]{oriId});

        HashMap hm = new HashMap();

        for(HashMap files: filesList){
            hm = new HashMap();

            hm.put("modifiedBy", files.get("modifiedBy").toString());
            hm.put("modifiedByName", files.get("modifiedByName").toString());
            hm.put("file", files.get("c_file").toString());
            hm.put("descr", files.get("c_descr").toString());
            hm.put("fk_doc_a", formArcId);

            String duplId = CommonUtils.saveUpdateForm2("",
                    Constants.FORM_ID.EMP_DEREG_DEREG_DOCS,
                    "",
                    hm
            );

            CommonUtils.duplicateDir(files.get("id").toString(),
                    duplId, Constants.TABLE.DREG_FILE);

            File file = FileUtil.getFile(
                    files.get("c_file").toString(),
                    Constants.TABLE.DREG_FILE.replace("app_fd_", ""),
                    duplId);

            if(file.exists() && !file.isDirectory()) {
                LogUtil.info(this.getClassName(),
                        duplId+" file "+file.getName()+" exist at "+file.getAbsolutePath());
            }
        }
    }

    private void copyMergerSubTblAndFiles(DBHandler db, String oriId, String formArcId) throws IOException {

        ArrayList<HashMap<String, String>> filesList = db.select(
                "SELECT * FROM "+Constants.TABLE.DREG_FILE+" WHERE c_fk_doc_form5_a = ?",
                new String[]{oriId});

        HashMap hm = new HashMap();

        for(HashMap files: filesList){
            hm = new HashMap();

            hm.put("modifiedBy", files.get("modifiedBy").toString());
            hm.put("modifiedByName", files.get("modifiedByName").toString());
            hm.put("file", files.get("c_file").toString());
            hm.put("descr", files.get("c_descr").toString());
            hm.put("fk_doc_form5_a", formArcId);

            String duplId = CommonUtils.saveUpdateForm2("",
                    Constants.FORM_ID.EMP_DEREG_DEREG_DOCS,
                    "",
                    hm
            );

            CommonUtils.duplicateDir(files.get("id").toString(),
                    duplId, Constants.TABLE.DREG_FILE);

            File file = FileUtil.getFile(
                    files.get("c_file").toString(),
                    Constants.TABLE.DREG_FILE.replace("app_fd_", ""),
                    duplId);

            if(file.exists() && !file.isDirectory()) {
                LogUtil.info(this.getClassName(),
                        duplId+" file "+file.getName()+" exist at "+file.getAbsolutePath());
            }
        }

        filesList = db.select(
                "SELECT * FROM "+Constants.TABLE.DREG_FILE+" WHERE c_fk_doc_form5_b = ?",
                new String[]{oriId});

        for(HashMap files: filesList){
            hm = new HashMap();

            hm.put("modifiedBy", files.get("modifiedBy").toString());
            hm.put("modifiedByName", files.get("modifiedByName").toString());
            hm.put("file", files.get("c_file").toString());
            hm.put("descr", files.get("c_descr").toString());
            hm.put("fk_doc_form5_b", formArcId);

            String duplId = CommonUtils.saveUpdateForm2("",
                    Constants.FORM_ID.EMP_DEREG_DEREG_DOCS,
                    "",
                    hm
            );

            CommonUtils.duplicateDir(files.get("id").toString(),
                    duplId, Constants.TABLE.DREG_FILE);

            File file = FileUtil.getFile(
                    files.get("c_file").toString(),
                    Constants.TABLE.DREG_FILE.replace("app_fd_", ""),
                    duplId);

            if(file.exists() && !file.isDirectory()) {
                LogUtil.info(this.getClassName(),
                        duplId+" file "+file.getName()+" exist at "+file.getAbsolutePath());
            }
        }
    }

    public static ArrayList getFormColumnList(DBHandler db, String tbl){
        String sql =
                "SELECT COLUMN_NAME, '' as COLUMN_VALUE\n" +
                        "FROM INFORMATION_SCHEMA.COLUMNS\n" +
                        "WHERE TABLE_NAME = ? "
                        + "AND TABLE_SCHEMA = DATABASE()";
        return db.select(sql, new String[]{tbl});
    }

//    public static ArrayList buildChangeAuditHm(DBHandler db, String colName, String value, String oldVal, ArrayList chgeList) {
//        HashMap newData = new HashMap();
//        HashMap fieldProp = null;
//
//        if(!StringUtils.isBlank(oldVal) && !oldVal.equals(value)){
//            fieldProp = KeywordDictionary.getFieldProperty(db, colName);
//        }
//
//        if(fieldProp!=null){
//            HashMap chge = new HashMap();
//
//            String isSect = fieldProp.get("c_isSectorField").toString();
//            String isLoc = fieldProp.get("c_isLocationField").toString();
//
//            chge.put(Constants.CHANGE_KEYS.FIELD, fieldProp.getOrDefault("c_columnName", "NODATA").toString());
//            chge.put(Constants.CHANGE_KEYS.OLD, KeywordDictionary.formatVal(db, isLoc, isSect, oldVal));
//            chge.put(Constants.CHANGE_KEYS.NEW, KeywordDictionary.formatVal(db, isLoc, isSect, value));
//
//            chgeList.add(chge);
//        }
//
//        return chgeList;
//    }

//    private void insertQueryAuditTrail(DBHandler db, ArrayList<HashMap<String, String>>  chgeList, String id, String modifiedBy) {
//
//        String status = "Query Replied, Data Updated";
//        String remarks = "<ol><li>"+buildRemarksString(chgeList)+"</li></ol>";
//
//        new AuditTrailUtil().insertAuditTrail2(db, id, modifiedBy, status, remarks, false, "");
//    }

    public static void insertQueryAuditTrail(DBHandler db, ArrayList<HashMap<String, String>>  chgeList,
                                             String id, String modifiedBy, String remarks) {

        String status = "Query Replied, Data Updated";
        String chgeValue = "";
//        String remarks = "";

        if(chgeList.size()>0){
            remarks+=Constants.SEE_DETAIL_WORD;
            chgeValue = buildRemarksString(chgeList);
        }

        new AuditTrailUtil().insertAuditTrail2(db, id, modifiedBy, status, remarks, true, chgeList);
    }

    public static String buildRemarksString(ArrayList<HashMap<String, String>>  chgeList){

        if(chgeList.size()==0){
            return "";
        }
        String title = " <br /> <br /><b>Changed values: </b> <br />";
        String remarks = "<tr>"+chgeList.stream()
                .map(m -> "<td>"
                        + m.get(Constants.CHANGE_KEYS.FIELD).toString()
                        + "</td><td>"
                        + m.get(Constants.CHANGE_KEYS.OLD).toString()
                        + "</td><td>"
                        + m.get(Constants.CHANGE_KEYS.NEW).toString()
                        + "</td>"
                ).collect(Collectors.joining("</tr><tr>"))
                +"</tr>";

        String table = title +
                "<table>"
                + "<thead>"
                + "<th>Field</th>"
                + "<th>Old Value</th>"
                + "<th>New Value</th>"
                + "</thead>"
                + "<tbody>"
                + remarks
                + "</tbody>"
                + "</table>";

        return table;
    }

}
