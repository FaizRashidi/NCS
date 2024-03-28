/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.util;

import java.io.File;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.SetupManager;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;

/**
 *
 * @author faizr
 */
public class Constants {
    
    static final AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
    public static final Long appVersion = appService.getPublishedVersion(APP_ID.EMPM);
    
    public static final String EMPM_JASPER_PDF_PATH = CommonUtils.getBaseURL()+"/jw/web/json/plugin/org.joget.plugin.enterprise.JasperReportsMenu/service?"
            + "appVersion=" + appVersion.toString()
            + "&userviewId="+URL.JASPER_UV_ID+"&appId="+APP_ID.EMPM+"&action=report&key=_&type=pdf";
        
    public static class PROCESS_DEFKEYS{
        public static final String EMP_REG = "emp_registration";
        public static final String EMP_DEREG = "emp_deregistration";
        public static final String PE_ENGAGEMENT = "pe_engagement";
    }
    
    public static class CHANGE_KEYS{
        public static final String FIELD = "FIELD";
        public static final String FIELD_ID = "FIELD_ID";
        public static final String OLD = "OLD";
        public static final String NEW = "NEW";
        public static final String EMP_DATA_TYPE = "EMP_DATA_TYPE";
    }
    
    public static class FLOW_TYPE{
        public static final String EMP_REG = "EMP_REG";
        public static final String EMP_DEREG = "EMP_DEREG";
        public static final String EMP_DEREG_WD = "EMP_DEREG_WD";
        public static final String STKHLDR = "STAKEHOLDER";
        public static final String POTENTIAL_EMP = "EMP_POT";
    }    
    
    public static class APP_ID{
        public static final String EMPM = "empm";
        public static final String MASTER_STP = "master_setup";
    }

    public static class DATA_ID{
        public static final String CRITERIA_SETUP_ID = "criteria-hrdc-emp";
        public static final String BYPASS_SETUP_ID = "dereg-bypass-hrdc-emp";
        public static final String MAIN_SETUP_ID = "epm_stp";
        public static final String DEV_CTRL_STP_ID = "dev_ctrl_2023";
        public static final String APPROVER_EMPREG_STP_ID = "cd35b5a3-b37c-4b3c-930c-3f1816a2b682";
        public static final String APPROVER_PE_STP_ID = "epm_approver_stp_2023";
    }
    
    public static class MAIL_SENT_AS{
        public static final String LETTER = "LETTER";
        public static final String EMAIL = "EMAIL";
    }
    
    public static class MAIL_TYPE{
        public static class REG{
            public static final String DRAFT = "ET018";
            public static final String SUBMIT_ACK = "ET017";
            public static final String PENDING_OFFICER = "ET025";
            public static final String PENDING_APPROVER = "ET019";
            public static final String QUERY = "ET011";
            public static final String QUERY_RESPONSE = "XXX";
            public static final String REJECTED = "ET023";
            public static final String APPROVED = "ET024";            
            public static final String FORWARD = "XXX";            
            public static final String REG_WELCOME_EMAIL = "ET045";            
        }
        public static class POT_EMP{
            public static final String ORDER = "ET022";
//            public static final String SUBMIT_ACK = "ET017";
//            public static final String PENDING_OFFICER = "ET025";
//            public static final String PENDING_APPROVER = "ET019";
//            public static final String QUERY = "ET011";
//            public static final String QUERY_RESPONSE = "XXX";
//            public static final String REJECTED = "ET023";
//            public static final String APPROVED = "ET024";            
        }
    }    
    
    public static class FIELD_VALUE{
        public static final class POT_EMP{
            public static final String FILETYPE_EXCEL = "Excel";
            public static final String FILETYPE_TEXTFILE = "TextFile";
        }
    }
    
    public  static class CACHE_TYPE{
        public static final String MAIN_EMP_DATA = "main_emp_data";
        public static final String OTHER_CONTACT_PERSON = "other_contact_person";
        public static final String DOC_SSM = "doc_ssm";
        public static final String DOC_EPF_PAYROLL = "doc_epf_payroll";
        public static final String DOC_FINANCT_STMT = "doc_finance_stmt";
    }
    
    public static class FORM_ID{
        
        public static final String EMP_LEVY = "empm_levy";
        
        public static final String EMP_ONBOARD = "empm_onboardStatus";
        
        public static final String EMP_BRANCH_SETUP = "empm_branch";
        public static final String EMP_REG_FORM = "reg_form";
        public static final String EMP_REG_DRAFT_FORM = "draft_sent_form";
        public static final String EMP_REG_VERIFY_FORM = "verification_form_v2";
        public static final String EMP_REG_APPROVAL_FORM = "approval_form_v2";
        public static final String EMP_REG_SUBFORM_DOCS = "docs_subform";
        public static final String EMP_REG_SUBFORM_OTHERCONTACTS = "other_cntct_person";
        
        public static final String EMP_DEREG_VERIFY_FORM = "dereg_verify_base";
        public static final String EMP_DEREG_VERIFY_M_FORM = "dereg_verifyMerger_base";
        public static final String EMP_DEREG_APPROVAL_M_FORM = "dereg_approval_base_m";
        public static final String EMP_DEREG_APPROVAL_FORM = "dereg_approval_base";
        public static final String EMP_DEREG_DEREG_DOCS = "dereg_attachmentSub";
        
        public static final String EMP_ASGN_DATA = "asgn_data";
        public static final String EMP_BRANCH_RUNNO = "comp_branch_runno";

        public static final String EMP_MAIN_FORM = "employer";
        public static final String EMP_REGAPPL_FORM = "reg_appl_archive";
        public static final String EMP_TEMP_MAIN_FORM = "employer_temp_data";
        public static final String EMP_TEMP_DATA = "employer_data_view_temp";
        public static final String EMP_CACHE = "emp_cache_data";

        public static final String ARCHIVE_FORM1 = "view_form1_main_regForm";        
        public static final String ARCHIVE_FORM4 = "arc_f4";        
        public static final String ARCHIVE_FORM5 = "arc_f5";        
        
        public static final String POTEMP_UPLOADED_ITM_FORM = "successfully_uploaded";
        public static final String POTEMP_ENFORCEMENT_SUBMISSION = "complain_form";
        public static final String POTEMP_ENGAGEMENT = "pe_egmnt_form";
        public static final String POTEMP_ENG_TEMPREG_FORM = "pe_parent_processing_eng_prev";
        public static final String POTEMP_WO_APPROVAL = "wo_main_form";
        public static final String POTEMP_WRITEOFF = "pe_writeoff";
        public static final String POTEMP_DETAIL = "pot_emp_data";
        public static final String POTEMP_FILE_UPL = "pe_upload_form";
        public static final String APPLICATION = "emp_reg";
        public static final String APPLICATION_EMPDETAILS = "emp_deets";
        public static final String APPROVAL = "ereg_approve";
        public static final String VERIFICATION = "ereg_verify";
        public static final String QUERY = "ereg_empl_query";
        public static final String DATA_UPDATE = "profile_update";
        public static final String AUDIT_TRAIL = "audit_trail";
        public static final String AUDIT_TRAIL_SUB = "audit_trail_sub";
        public static final String APPROVER_STP = "approver";
        public static final String POTEMP_MAIL_LIST = "potential_employer_mail_list";
        public static final String POTEMP_MAIL_PREVIEW = "potential_employer_mail_preview";
        public static final String MAIL_PREVIEW = "mail_list_preview_view"; //empm_usr_mail
        public static final String MAIL_PREVIEW2 = "approve_email_sub";
        public static final String FORMID_EMPM_BRANCH_EXCEL = "branch_file_upl";
        public static final String FORMID_EMPM_BRANCH_MANUAL = "branch_data";
        public static final String FORMID_PARENT_SUB = "inmaking";
        public static final String EMPLOYER_DEREG_FORM = "emp_dereg";
        public static final String EMPLOYER_DEREGWD_FORM = "empm_dereg_wd";
        public static final String COMPANY_MERGE = "comp_merge_info_subform";
    }
    
    public static final String FIELD_EMPM_FILE_UPLOAD = "mail_attachment";
    
    public static final String SEE_DETAIL_WORD = "<br /><br /><b><i>Processing form updated, click the document icon to view details</i></b>";
    
    public static final String EMAIL_FROM = "noreply@ncs-dev.hrdcorp.gov.my";
    public static final String EMAIL_TRACK = " [LINK{TRACK}] ";
    
    public static final String PROCESS_ADMIN_USERID = "admin";
    public static final String PROCESS_ADMIN_PW = "dev_jLc85eNV";
    
    public static final String API_SECRET = "API-949bcad4-ba9e-4bda-9ebb-88cb3df29ccb";
    
    public static final String USERID_PREFIX_APPROVED = "emp-";
    public static final String USERID_PREFIX_TEMP = "temp-";
    
    public static class TABLE{        
        public static final String STP_APPROVER = "app_fd_empm_appvr_stp";
        public static final String STP_APPROVER_USERS = "app_fd_empm_appvr_usr_stp";
        public static final String STP_EMPREG = "app_fd_empm_reg_stp";
        public static final String STP_SUB_SECTOR = "app_fd_stp_sub_sector";
        
        public static final String CACHEDATA = "app_fd_empm_data_cache";
        
        public static final String USERMAP = "app_fd_empm_usermap";
        public static final String EMPREG = "app_fd_empm_reg";
        public static final String EMPREG_TEMP = "app_fd_empm_reg_temp";
        public static final String EMP_OTHER_CONTACT_DETAILS = "app_fd_empm_cntct_oth";
        public static final String EMPREG_APPL = "app_fd_empm_regAppl";
        public static final String STK = "app_fd_empm_reg";
        public static final String DEREG = "app_fd_empm_dereg";
        public static final String DEREG_WD = "app_fd_empm_dereg_wd";
        public static final String DEREG_F5 = "app_fd_empm_dereg_f5";
        public static final String MERGE = "app_fd_empm_comp_merge";
        
        public static final String POT_EMP_UPLOAD = "app_fd_empm_pe_file_upl";        
        public static final String POT_EMP_UPLOAD_DATA = "app_fd_empm_pe_upl_data";    
        
        public static final String POT_EMP = "app_fd_empm_pe_potEmp";        
        public static final String POT_EMP_MAIL_LIST = "app_fd_empm_mail_list";        
            
        public static final String POT_EMP_WRITEOFF = "app_fd_empm_pe_writeoff";        
        public static final String POT_EMP_ENGAGEMENT = "app_fd_empm_pe_egmnt";        
        public static final String POT_EMP_ENG_EVENT = "app_fd_empm_pe_event";        
        public static final String POT_EMP_EMPREG_TEMP = "app_fd_empm_reg_temp";        
        public static final String POT_EMP_ENFORCEMENT = "app_fd_empm_pe_compl_enf";        
        public static final String POT_EMP_CKSP = "app_fd_empm_pe_compl_cksp";        
        public static final String AUDIT = "app_fd_empm_audit";        
        public static final String AUDIT_SUB  = "app_fd_empm_audit_sub";        
        public static final String EMAIL  = "app_fd_empm_usr_mail";        
        public static final String REQUEST_CHANGES  = "app_fd_empm_reg_changes";        
        public static final String SUBMIT_TABLE_FORM1  = "app_fd_empm_form1";        
        public static final String SUBMIT_TABLE_FORM1A  = "app_fd_empm_form1A";
        public static final String SUBMIT_TABLE_FORM4  = "app_fd_empm_form4";
        public static final String SUBMIT_TABLE_FORM5  = "app_fd_empm_form5";
        public static final String DIR_USER = "dir_user";
        public static final String ASSIGNEES_DATA = "app_fd_empm_asgn_data";
        public static final String BRANCH_RUNNO = "app_fd_empm_branch_runno";
        public static final String DOCS = "app_fd_empm_docs";
        public static final String DREG_FILE = "app_fd_empm_dreg_file";
        public static final String EMPREG_DISBURSE = "app_fd_empm_disburse";
    }
        
    //STATUS is system-dependant checking
    public static class STATUS{  
        
        public static class VIEW_STATUS{
            public static final String NEW = "NEW";
            public static final String RETURNED = "RETURNED";
            public static final String QUERY_REPLIED = "QUERY REPLIED";
            public static final String PENDING = "PENDING";
        }
        
        public static class EMP{
            public static final String ACTIVE = "ACTIVE";
            public static final String INACTIVE = "INACTIVE";

            public static final String DRAFT = "DRAFT";
            public static final String UPLOADED = "UPLOADED";
            public static final String POTENTIAL_EMPLOYER = "POTENTIAL_EMPLOYER";
            public static final String TRUE_POTENTIAL_EMPLOYER = "TRUE_POTENTIAL_EMPLOYER";
            public static final String WRITTEN_OFF = "WRITTEN_OFF";
            public static final String DIRTY_LISTED = "DIRTY_LISTED";
            public static final String REGISTERING = "REGISTERING";
            public static final String REREGISTERING = "REREGISTERING";
            public static final String REGISTER_APPROVED = "REGISTER_APPROVED";
            public static final String REREGISTER_APPROVED = "REGISTER_APPROVED";
            public static final String REGISTER_REJECTED = "REGISTER_REJECTED";
            public static final String REREGISTER_REJECTED = "REGISTER_REJECTED";
            public static final String DEREGISTERING = "DEREGISTERING";
            public static final String DEREGISTER_APPROVED = "DEREGISTER_APPROVED";
            public static final String DEREGISTER_REJECTED = "DEREGISTER_REJECTED";
            public static final String MERGING = "MERGING";
            public static final String MERGING_APPROVED = "MERGING_APPROVED";
            public static final String MERGING_REJECTED = "MERGING_REJECTED";
            public static final String STKH_REGISTERING = "STKH_REGISTERING";
            public static final String STKH_REG_APPROVED = "STKH_REG_APPROVED";
            public static final String STKH_REG_REJECTED = "STKH_REG_REJECTED";
            
        }
        
        public static class POT_EMP{
            public static final String NEW = "NEW";
            public static final String POTENTIAL = "POTENTIAL";
            public static final String TRUE = "TRUE";
            public static final String REGISTERED = "REGISTERED";
            public static final String DIRTY = "DIRTY";
            public static final String REJECT = "REJECT";
            public static final String DISMISS = "DISMISS";    
            public static final String ENGAGEMENT = "ENGAGEMENT";    
            public static final String ENFORCEMENT = "ENFORCEMENT";    
            public static final String PENDING_WRITE_OFF = "PENDING_WRITE_OFF";    
            public static final String WRITTEN_OFF = "WRITTEN_OFF";    
            public static final String CKSP = "CKSP";    

        }        
        
        public static class EMAIL{
            public static final String PENDING_SEND = "PENDING SEND";
            public static final String SENT = "SENT";
            public static final String OPENED = "OPENED";
            public static final String RESPONDED = "RESPONDED";
        }
        
        public static class LETTER{
            public static final String PENDING_SEND = "PENDING SEND";
            public static final String LETTER_SENT = "LETTER SENT";            
            public static final String UNDELIVERED = "LETTER UNDELIVERED";
            public static final String ATTEMPTED = "LETTER ATTEMPTED";
            public static final String RESPONDED = "RESPONDED";
        }
        
        public static class EGMNT{
            public static final String ONGOING = "ONGOING";
            public static final String SAVED = "SAVED";
            public static final String COMPLETED = "ENGAGEMENT COMPLETED";
            public static final String QUERY = "QUERY";
            public static final String ACKNOWLEDGED = "ACKNOWLEDGED";   
            public static final String REJECTED = "REJECTED";
            public static final String FORWARD = "FORWARD";
            public static final String RETURN = "RETURN";
        } 
        
        public static class EGMNT_CHANGE_STATUS{
            public static final String NEW = "NEW";
            public static final String QUERY = "QUERY";
            public static final String APPROVED = "APPROVED";   
            public static final String REJECTED = "REJECTED";
        } 
        
        public static class ENF_STATUS{
            public static final String COMPLAINT_TO_ENFORCEMENT = "COMPLAINT TO ENFORCEMENT";
            public static final String VISITED_BY_ENFORCEMENT = "VISITED BY ENFORCEMENT";
            public static final String NOTICE_ENFORCEMENT = "NOTICE ENFORCEMENT";
            public static final String OPS_VISIT = "OPS VISIT";
        }
        
        public static class ASGN_STATUS{
            public static final String FORWARDED = "FORWARDED";
            public static final String RETURNED = "RETURNED";
            public static final String CURRENT = "CURRENT";
            public static final String CURRENT_BASE = "CURRENT_BASE";
        }
    }
    
    public static class IMGSRC {
        public static String HRDC_400 = "HRDC-logo.png";
    }
    
    //Last movement - different status for end-user to see
    public static class LAST_MOVEMENT{  
        public static final String  NEW = "NEW";
        public static final String  DIRTY_LIST = "DIRTY LIST";
        public static final String  POTENTIAL = "POTENTIAL";
        public static final String  TRUE_POTENTIAL = "TRUE POTENTIAL";
        
        public static final String  LETTER_SENT = "LETTER SENT";
        public static final String  LETTER_UNDELIVERED = "LETTER UNDELIVERED";
        public static final String  LETTER_DELIVERED = "LETTER DELIVERED";
        public static final String  LETTER_ATTEMPTED = "LETTER ATTEMPTED";
        
        public static final String  EMAIL_SENT = "EMAIL SENT";
        public static final String  EMAIL_OPENED = "EMAIL OPENED";
        public static final String  EMAIL_FAIL = "EMAIL FAILED";
        public static final String  EMAIL_BOUNCE = "EMAIL BOUNCE";
        
        public static final String  COMPLAINT_TO_ENFORCEMENT = "COMPLAINT TO ENFORCEMENT";
        public static final String  VISITED_BY_ENFORCEMENT = "VISITED BY ENFORCEMENT";        
        public static final String  NOTICE_ENFORCEMENT = "NOTICE ENFORCEMENT";
        
        public static final String  COMPLAINT_TO_CKSP = "COMPLAINT TO CKSP";
        public static final String  NOTICE_CKSP = "NOTICE CKSP";
        public static final String  VISITED_BY_CKSP = "VISITED BY CKSP";        
        public static final String  COMPOUND_BY_CKSP = "COMPOUND BY CKSP";
        public static final String  NFA = "NFA";
        public static final String  PROSECUTION = "PROSECUTION";
        
        public static final String  FORM_1_RECEIVED = "FORM 1 RECEIVED";                
        public static final String  FORM_1_APPROVED = "FORM 1 APPROVED";
        public static final String  FORM_1_REJECTED = "FORM 1 REJECTED";
        public static final String  FORM_1_QUERY = "FORM 1 QUERY";
        public static final String  FORM_1A_RECEIVED = "FORM 1A RECEIVED";
        public static final String  FORM_1A_APPROVED = "FORM 1A APPROVED";
        public static final String  FORM_1A_REJECTED = "FORM 1A REJECTED";
        public static final String  FORM_1A_QUERY = "FORM 1A QUERY";
        public static final String  FORM_1_AMENDMENT = "FORM 1 AMENDMENT";
        public static final String  FORM_1_AMENDMENT_REJECTED = "FORM 1 AMENDMENT REJECTED";
        public static final String  FORM_1_AMENDMENT_APPROVED = "FORM 1 AMENDMENT APPROVED";
        public static final String  FORM_1_AMENDMENT_QUERY = "FORM 1 AMENDMENT QUERY";
        
        public static final String  REGISTERED = "REGISTERED";        
        public static final String  REREGISTERED = "RE-REGISTERED";
        
        public static final String  DEREGISTRATION_APPLY_FORM4A = "APPLY DEREGISTRATION FORM 4A";
        public static final String  DEREGISTRATION_APPLY_FORM4 = "APPLY DEREGISTRATION FORM 4";
        public static final String  DEREGISTRATION_APPLY_FORM4_5 = "APPLY DEREGISTRATION FORM 4 (5)";         
        public static final String  DEREGISTRATION_FORM_4A = "DEREGISTRATION FORM 4A";
        public static final String  DEREGISTRATION_FORM_4 = "DEREGISTRATION FORM 4";
        public static final String  DEREGISTRATION_FORM_4_5 = "DEREGISTRATION FORM 4 (5)";
        public static final String  DEREGISTRATION_REJECTED = "DEREGISTRATION REJECTED";   
        public static final String  DEREGISTRATION_QUERY = "QUERY DEREGISTRATION";
        
        public static final String  FORM_5_RECEIVED = "FORM 5 RECEIVED";
        public static final String  FORM_5_APPROVED = "FORM 5 APPROVED";
        public static final String  FORM_5_REJECTED = "FORM 5 REJECTED";
        public static final String  FORM_5_QUERY = "FORM 5 QUERY";
        
        public static final String  FORM_4_WITHDRAWAL_RECEIVED = "FORM 4 WITHDRAWAL RECEIVED";
        public static final String  FORM_4_WITHDRAWAL_APPROVED = "FORM 4 WITHDRAWAL APPROVED";
        public static final String  FORM_4_WITHDRAWAL_REJECTED = "FORM 4 WITHDRAWAL REJECTED";
    }
    
    public static class ENV_VAR{
        public static class EMPREG{
            public static final String EMPREG_COUNTER = "empreg_ref_counter";    
        }
        public static class DEREG{
            public static final String DEREG_COUNTER = "dereg_ref_counter";
            public static final String DEREG_WD_COUNTER = "wd_ref_counter";
        }
        public static class POT_EMP{
            public static final String WRITE_OFF_COUNTER = "pe_wo_counter";
            public static final String BATCH_COUNTER = "batch_counter";
            public static final String PE_MAIL_LIST_COUNTER = "potEmp_mail_counter";
            public static final String PE_WRITEOFF_COUNTER = "pe_wo_counter";
        }
        public static class EGMNT{
            public static final String BATCH_COUNTER = "eng_batch_counter";
            public static final String EVENT_COUNTER = "event_counter";            
        }
        public static class ENF{
            public static final String ENF_COMPLAIN_COUNTER = "enf_complain_counter";
            public static final String VISIT_COUNTER = "visit_counter"; 
            public static final String IVTG_COUNTER = "ivtg_counter";
        }
        public static class CKSP{
        }       
            
        public static final String LETTER_COUNTER = "letter_counter";
        public static final String MAIL_COUNTER = "mail_id_counter";  
        public static final String APS_ITG_ID = "aps_itg_id";  
        public static final String APS_ITG_DATE = "aps_itg_date";  
    }
    
    public static class USER_GROUP{
        public static final String DRAFT_USERS = "hrdc_empm_draft";
        public static final String REJECTED_USERS = "hrdc_empm_rejected";
        public static final String TEMP_USERS = "hrdc_empm_temp";
        public static final String EMPLOYERS = "hrdc_empm_employers";
        public static final String BRANCH = "hrdc_empm_branch_employers";
        public static final String STAFF = "hrdc_empm_staff";
        public static final String APPROVERS = "hrdc_empm_approver";
        public static final String OFFICERS = "hrdc_empm_officer";
        public static final String SV = "hrdc_empm_sv";
        public static final String PE_SV = "hrdc_empm_pe_sv";
        public static final String PE_APPROVERS = "hrdc_empm_pe_officer";
        public static final String PE_OFFICERS = "hrdc_empm_pe_approvers";
        public static final String PE_CSKP = "hrdc_empm_pe_cksp";
        public static final String PE_ENG_ADMIN = "hrdc_empm_pe_eng_admin";
        public static final String PE_ENG_OFFICER = "hrdc_empm_pe_eng_officer";
        public static final String PE_ENF_OFFICER = "hrdc_empm_pe_enf_officer";
        public static final String ERD_ADMIN = "hrdc_empm_erdAdmin";
        
        public static final String VIEW_PERMIT_LEVY = "hrdc_staffPermit_view_levy";
        public static final String VIEW_PERMIT_GRANT = "hrdc_staffPermit_view_grant";
        public static final String VIEW_PERMIT_CLAIM = "hrdc_staffPermit_view_claim";
        public static final String VIEW_PERMIT_EMPM = "hrdc_staffPermit_view_empm";
        public static final String VIEW_PERMIT_EVENT = "hrdc_staffPermit_view_event";
        public static final String VIEW_PERMIT_TP = "hrdc_staffPermit_view_tp";
    }
    
    public static class SECTOR_TYPE{
        public static final String INDUSTRY_SECTOR = "industry_sector";    
        public static final String DIV = "div";    
        public static final String MAIN_SECTOR_CODE = "main_sector_code";    
        public static final String CLASS_SECTOR = "class_code";    
        public static final String SUB_SECTOR_CODE = "sector_code";    
    }
    
    public static class URL{
        
        public static final String JASPER_UV_ID = "empm_jr";
        public static final String JASPER_FORM1_URL = "employee_reg";               
        
        public static final String FORM1_LINK = "REGISTER_FORM1";    
        public static final String FORM1_DRAFT = "DRAFT";    
        public static final String FORM1_VIEW = "emp_list_f1";    
        public static final String LINK_EXPIRED = "LINK_EXPIRED";    
        public static final String DEFAULT_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";    
        public static final String PE_UPLOAD_RESULT = "pe_upload_result";    
          
        public static final String LEVY_PERCENTAGE = CommonUtils.getBaseURL()+"/jw/web/json/plugin/id.co.itasoft.hrdc.plugin.LevyPercentage/service";  
        public static final String LEVY_TRANSFER = CommonUtils.getBaseURL()+"/jw/web/json/plugin/hrdc.levm.service.LevyDeRegistration/service";  
        public static final String LEVY_SUMMARY = CommonUtils.getBaseURL()+"/jw/web/json/plugin/id.co.itasoft.hrdc.plugin.LevySummary/service";  
        public static final String LEVY_PENDING = CommonUtils.getBaseURL()+"/jw/web/json/plugin/id.co.itasoft.hrdc.plugin.PendingApplication/service";
        
        public static final String APS_PROFILE_REG = CommonUtils.getBaseURL()+"/jw/api/NCSgateway/inbound/APS/profile-reg";
        public static final String GRANT_SUMMARY = CommonUtils.getBaseURL()+"/jw/web/json/plugin/org.joget.plugin.ClaimsAPIWebservice/service";
        public static final String GRANT_DEREG_NOTI = CommonUtils.getBaseURL()+"/jw/api/process/processGrantByHRDCNo/startProcess";
        
        public static final String CLAIM_SUMMARY = CommonUtils.getBaseURL()+"/jw/web/json/plugin/com.tms.hrdc.webservice.ClaimAPI/service?action=claimSummaryEmployer";
    }
    
    public static class LINK_TEMPLATE{
        public static final String LINK_FORM1 = "LINK_FORM1";    
        public static final String LINK_FORM1_SUBMITTED = "LINK_FORM1_SUBMITTED";    
        public static final String LINK_FORM1A = "LINK_FORM1A";             
        public static final String LINK_FORM1_VERIFY = "LINK_FORM1_VERIFY";    
        public static final String LINK_FORM1_APPROVAL = "LINK_FORM1_APPROVAL";    
        public static final String LINK_FORM1_VIEW = "LINK_FORM1_VIEW";   
        public static final String LINK_FORM4 = "LINK_FORM4";    
        public static final String LINK_FORM4A = "LINK_FORM4A";    
        public static final String LINK_FORM5 = "LINK_FORM5";    
        public static final String LINK_FORM5A = "LINK_FORM5A";   
        public static final String LINK_FORM1_DRAFT = "LINK_FORM1_DRAFT";
        public static final String LINK_FORM1_REG = "LINK_FORM1_REG";
        public static final String LINK_LOGIN = "LINK_LOGIN";
        public static final String LINK_ONBOARDING = "LINK_ONBOARDING";
    }
    
    public static class API{
        
        public static class JOGETAPI{
            public static final String MASTERAPILOGIN = "masterApiAdmin";
//            public static final String MASTERAPIPW = "dev_jLc85eNV";
            public static final String MASTERAPIPW = "dev_jLc85eNV24gu";
//            public static final String MASTERAPIPW = "masterApiAdmin";
        }
        
        public static class STATUS{
            public static final String SUCCESS = "SUCCESS";
            public static final String FAILED = "FAILED";
            public static final String ERROR = "ERROR";

            public static final String ERROR_INCORRECT_PARAM = "Incorrect parameter";
            public static final String ERROR_CREDENTIALS = "Incorrect credentials";
            public static final String ERROR_USEPOST = "Incorrect HTTP method, please use POST";
            public static final String ERROR_USEGET = "Incorrect HTTP method, please use GET";
            public static final String ERROR_SPECIFYAPIMETHOD = "Incorrect API method";
        }
    }
    
    public static class PROCESS{
        public static class DEFID{
            public static final String EREG = "empm#"+WorkflowManager.LATEST+"#emp_registration";
            public static final String EDEREG = "empm#"+WorkflowManager.LATEST+"#emp_deregistration";
        }
    }
    
    public static class APS{
        public static final String APS_PROFILE_REG = Constants.URL.APS_PROFILE_REG;
        public static class TXNTYPE{
            public static final String CREATE = "Create";
            public static final String MODIFY = "Modify";
            public static final String CLOSE = "Close";
        }
        
//        public static final String ACTIVE = "Y";
        public static final String ACTIVE = "N";
        public static final String INACTIVE = "N";
//        public static final String ACTIVE = "Active";
//        public static final String INACTIVE = "Inactive";
        
        public static class CUSTYPE{
            public static final String EMPR = "EMPR";
        }
    }
    
    public static class B2C{
        public static final String USERAPI = CommonUtils.getBaseURL()+"/jw/api/NCSgateway/inbound/B2C/create-user";
        public static final String USERDELETE = CommonUtils.getBaseURL()+"/jw/api/NCSgateway/inbound/B2C/delete-user";
        public static final String USERUPDATE = CommonUtils.getBaseURL()+"/jw/api/NCSgateway/inbound/B2C/update-user";
    }
    
    public static class LEVYAPI{
        public static final String LEVY_PERCENTAGE = Constants.URL.LEVY_PERCENTAGE;
        public static final String LEVY_TRANSFER = Constants.URL.LEVY_TRANSFER;
        public static final String SUMMARY = Constants.URL.LEVY_SUMMARY;
        public static final String PENDING = Constants.URL.LEVY_PENDING;
    }
    
    public static class GRANTAPI{
        public static final String NOTIFY_DEREG = Constants.URL.GRANT_DEREG_NOTI;
        public static final String SUMMARY = Constants.URL.GRANT_SUMMARY;
    }
    
    public static class CLAIMAPI{
        public static final String SUMMARY = Constants.URL.CLAIM_SUMMARY;
    }
    
    public static String BASE_DATA_PREFIX = "BASE_DATA_";
    public static String SEPARATOR = ";";
    public static String JOGET_BASE_UPL_PATH = SetupManager.getBaseDirectory() + File.separator + "app_formuploads" ;
    
    public static final Map<String,String> SYMBOLTABLE;
    static{
        Hashtable<String,String> tmp = 
            new Hashtable<String,String>();
        tmp.put("at","@");
        tmp.put("hash","#");
        tmp.put("dollar","$");
        tmp.put("percentage","%");
        tmp.put("ampersand","&");
        tmp.put("semicolon",";");
        tmp.put("comma",",");
        tmp.put("hyphen","-");
        tmp.put("plus","");
        tmp.put("slash","/");
        tmp.put("backslash","\\");
        tmp.put("up_arrow","^");
        tmp.put("left_arrow","<");
        tmp.put("right_arrow",">");
        tmp.put("question_mark","?");
        SYMBOLTABLE = Collections.unmodifiableMap(tmp);
    }
}
