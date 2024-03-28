/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.DBHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.PluginThread;
import org.joget.plugin.base.DefaultApplicationPlugin;

/**
 *
 * @author faizr
 */
public class MigrationTool extends DefaultApplicationPlugin{

    @Override
    public String getName() {
        return "Migration Tool";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "To migrate data";
    }

    @Override
    public String getLabel() {
        return "HRDC - TEST - ARCHIE MIGRATION Tool";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }
    
    private void msg(String msg){
        LogUtil.info(this.getClassName(), msg);
    }
    
    private int LIMIT_ACCOUNT_CREATED = 5;
    
    @Override
    public Object execute(Map props) {
        
        DBHandler db = new DBHandler();
        
        Thread checkingThread = new PluginThread(new Runnable() {
            @Override
            public void run() {
                int i = 0;

                try{
                    db.openConnection();

                    msg("RUNNING ARCHIVE");

                    HashMap cntHm = db.selectOneRecord(
                            "SELECT COUNT(*) as count FROM archive.levy_statements "
                            );

                    String count = cntHm!=null?cntHm.getOrDefault("count", "0").toString():"0";
                    msg("Archvie stmt size "+count);
                    
                    int countIn = Integer.parseInt(count);
                    
                    int offset = 0;
                    int limit = 1000000;
                    
                    for(int ic=0; ic<countIn; ic += limit){
                        
                        String sql = "select * from archive.levy_statements\n" +
                                    "order by LEVY_STATEMENT_CREATED_DATE desc\n" +
                                    "LIMIT 0 OFFSET 1000000";                        
                        
                        
                        ArrayList<HashMap<String, String>> arch = db.select(
                            "select * from archive.levy_statements " +
                            "order by LEVY_STATEMENT_CREATED_DATE desc\n" +
                            "LIMIT "+Integer.toString(limit)+" OFFSET "+Integer.toString(offset)
                        );
                        
                        msg("Updating archive.. loop: "+Integer.toString(ic) + " limit" + Integer.toString(limit) + " offset" + Integer.toString(offset));
                        
//                        if( (countIn-limit)<limit ){
//                            limit = countIn-limit;
//                        }

                        offset = limit;
                        
                        for(HashMap countHm:arch){

                            i++;

                            if(i%10000==0){
                                msg("Updating archive.. current size: "+Integer.toString(i));
                            }

                            HashMap mgData = new HashMap();            
                            mgData.put("lvy_fk", countHm.getOrDefault("LEVY_MASTER_ID", "").toString());
                            mgData.put("emp_id", countHm.getOrDefault("EMPLOYER_ID", "").toString());
                            mgData.put("lvy_details_fk", countHm.getOrDefault("LEVY_DTLS_ID", "").toString());
                            mgData.put("lvy_master_start_dt", countHm.getOrDefault("LEVY_MASTER_START_DATE", "").toString());
                            mgData.put("lvy_master_end_dt", countHm.getOrDefault("LEVY_MASTER_END_DATE", "").toString());
                            mgData.put("lvy_percent", countHm.getOrDefault("EFFECTIVE_LEVY_PERCENTAGE", "").toString());
                            mgData.put("last_calc_lvy_percent", countHm.getOrDefault("LAST_CALCULATED_LEVY_PERCENTAGE", "").toString());
                            mgData.put("createdBy", countHm.getOrDefault("LEVY_STATEMENT_CREATED_BY_ID", "").toString());
                            mgData.put("createdByName", countHm.getOrDefault("LEVY_STATEMENT_CREATED_BY_NAME", "").toString());
                            
                            if(!countHm.getOrDefault("LEVY_STATEMENT_CREATED_DATE", "").toString().isEmpty()){
                                mgData.put("dateCreated", countHm.getOrDefault("LEVY_STATEMENT_CREATED_DATE", "").toString());
                            }
                            
                            if(!countHm.getOrDefault("LEVY_STATEMENT_UPDATED_DATE", "").toString().isEmpty()){
                                mgData.put("dateModified", countHm.getOrDefault("LEVY_STATEMENT_UPDATED_DATE", "").toString());
                            }
                            
                            mgData.put("modifiedBy", countHm.getOrDefault("LEVY_STATEMENT_UPDATED_BY_ID", "").toString());
                            mgData.put("modifiedByName", countHm.getOrDefault("LEVY_STATEMENT_UPDATED_BY_NAME", "").toString());
                            mgData.put("lvy_master_status", countHm.getOrDefault("LEVY_MASTER_STATUS", "").toString());
                            mgData.put("lvy_stmt_status", countHm.getOrDefault("LEVY_STATEMENT_STATUS", "").toString());
                            mgData.put("bank_id", countHm.getOrDefault("BANK_ID", "").toString());
                            mgData.put("bank_name", countHm.getOrDefault("BANK_NAME", "").toString());
                            mgData.put("bsr_code", countHm.getOrDefault("BSR_CODE", "").toString());
                            mgData.put("cheque_no", countHm.getOrDefault("CHEQUE_NO", "").toString());
                            mgData.put("total_employees", countHm.getOrDefault("TOTAL_EMPLOYEES", "").toString());
                            mgData.put("total_wages", countHm.getOrDefault("TOTAL_WAGES", "").toString());
                            mgData.put("stmt_dt", countHm.getOrDefault("STATEMENT_DATE", "").toString());
                            mgData.put("pymnt_dt", countHm.getOrDefault("PAYMENT_DATE", "").toString());
                            mgData.put("lvy_month", countHm.getOrDefault("LEVY_MONTH", "").toString());
                            mgData.put("lvy_amt", countHm.getOrDefault("LEVY_AMOUNT", "").toString());
                            mgData.put("lvy_balance", countHm.getOrDefault("LEVY_BALANCE", "").toString());
                            mgData.put("txn_type", countHm.getOrDefault("TRANSACTION_TYPE", "").toString());
                            mgData.put("is_exempt_forfeit", countHm.getOrDefault("IS_EXEMPTFORFIET", "").toString());
                            mgData.put("lvy_pay_fk", countHm.getOrDefault("PK_LVY_PAY", "").toString());
                            mgData.put("closing_balance", countHm.getOrDefault("CLOSING_BALANCE", "").toString());
                            mgData.put("lvy_year", countHm.getOrDefault("YEAR", "").toString());
                            mgData.put("effective_oneffective_on", countHm.getOrDefault("EFFECTIVE_ON", "").toString());
                            mgData.put("payout_amt", countHm.getOrDefault("PAYOUT_AMOUNT", "").toString());
                            mgData.put("offset_amt", countHm.getOrDefault("OFFSET_AMOUNT", "").toString());
                            mgData.put("bank_txn_id", countHm.getOrDefault("BANK_TRANSACTION_ID", "").toString());

                            CommonUtils.saveUpdateForm("test","lvy_stmt_test", "", mgData);
                        }
                    }

//                    ArrayList<HashMap<String, String>> arch = db.select(
//                            "select * from archive.levy_statements "
//                    );

//                    for(HashMap countHm:arch){
//
//                        i++;
//
//                        if(i%500000==0){
//                            msg("Updating archive.. current size: "+Integer.toString(i));
//                        }
//
//                        HashMap mgData = new HashMap();            
//                        mgData.put("lvy_fk", countHm.getOrDefault("LEVY_MASTER_ID", "").toString());
//                        mgData.put("emp_id", countHm.getOrDefault("EMPLOYER_ID", "").toString());
//                        mgData.put("lvy_details_fk", countHm.getOrDefault("LEVY_DTLS_ID", "").toString());
//                        mgData.put("lvy_master_start_dt", countHm.getOrDefault("LEVY_MASTER_START_DATE", "").toString());
//                        mgData.put("lvy_master_end_dt", countHm.getOrDefault("LEVY_MASTER_END_DATE", "").toString());
//                        mgData.put("lvy_percent", countHm.getOrDefault("EFFECTIVE_LEVY_PERCENTAGE", "").toString());
//                        mgData.put("last_calc_lvy_percent", countHm.getOrDefault("LAST_CALCULATED_LEVY_PERCENTAGE", "").toString());
//                        mgData.put("createdBy", countHm.getOrDefault("LEVY_STATEMENT_CREATED_BY_ID", "").toString());
//                        mgData.put("createdByName", countHm.getOrDefault("LEVY_STATEMENT_CREATED_BY_NAME", "").toString());
//                        mgData.put("dateCreated", countHm.getOrDefault("LEVY_STATEMENT_CREATED_DATE", "").toString());
//                        mgData.put("dateModified", countHm.getOrDefault("LEVY_STATEMENT_UPDATED_DATE", "").toString());
//                        mgData.put("modifiedBy", countHm.getOrDefault("LEVY_STATEMENT_UPDATED_BY_ID", "").toString());
//                        mgData.put("modifiedByName", countHm.getOrDefault("LEVY_STATEMENT_UPDATED_BY_NAME", "").toString());
//                        mgData.put("lvy_master_status", countHm.getOrDefault("LEVY_MASTER_STATUS", "").toString());
//                        mgData.put("lvy_stmt_status", countHm.getOrDefault("LEVY_STATEMENT_STATUS", "").toString());
//                        mgData.put("bank_id", countHm.getOrDefault("BANK_ID", "").toString());
//                        mgData.put("bank_name", countHm.getOrDefault("BANK_NAME", "").toString());
//                        mgData.put("bsr_code", countHm.getOrDefault("BSR_CODE", "").toString());
//                        mgData.put("cheque_no", countHm.getOrDefault("CHEQUE_NO", "").toString());
//                        mgData.put("total_employees", countHm.getOrDefault("TOTAL_EMPLOYEES", "").toString());
//                        mgData.put("total_wages", countHm.getOrDefault("TOTAL_WAGES", "").toString());
//                        mgData.put("stmt_dt", countHm.getOrDefault("STATEMENT_DATE", "").toString());
//                        mgData.put("pymnt_dt", countHm.getOrDefault("PAYMENT_DATE", "").toString());
//                        mgData.put("lvy_month", countHm.getOrDefault("LEVY_MONTH", "").toString());
//                        mgData.put("lvy_amt", countHm.getOrDefault("LEVY_AMOUNT", "").toString());
//                        mgData.put("lvy_balance", countHm.getOrDefault("LEVY_BALANCE", "").toString());
//                        mgData.put("txn_type", countHm.getOrDefault("TRANSACTION_TYPE", "").toString());
//                        mgData.put("is_exempt_forfeit", countHm.getOrDefault("IS_EXEMPTFORFIET", "").toString());
//                        mgData.put("lvy_pay_fk", countHm.getOrDefault("PK_LVY_PAY", "").toString());
//                        mgData.put("closing_balance", countHm.getOrDefault("CLOSING_BALANCE", "").toString());
//                        mgData.put("lvy_year", countHm.getOrDefault("YEAR", "").toString());
//                        mgData.put("effective_oneffective_on", countHm.getOrDefault("EFFECTIVE_ON", "").toString());
//                        mgData.put("payout_amt", countHm.getOrDefault("PAYOUT_AMOUNT", "").toString());
//                        mgData.put("offset_amt", countHm.getOrDefault("OFFSET_AMOUNT", "").toString());
//                        mgData.put("bank_txn_id", countHm.getOrDefault("BANK_TRANSACTION_ID", "").toString());
//
//                        CommonUtils.saveUpdateForm("test","lvy_stmt_test", "", mgData);
//                    }

                }catch(Exception e){
                    e.printStackTrace();
                }finally{
                    db.closeConnection();
                }
        
            }
        });
        
        checkingThread.setDaemon(true);
        checkingThread.start();
        
        msg("ARCHIVE ENDED");
        
        
        return null;
    }
    
}
