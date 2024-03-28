
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginException;
import org.joget.workflow.model.WorkflowAssignment;

/**
 *
 * @author faizr
 */
public class SendEmailTool extends DefaultApplicationPlugin{

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
        return "To send mail and record its details";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Test Mail Sending Tool";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return 
            "[{\n" +
            "    title : 'EMPM Send Email Tool',\n" +
            "    properties : [{\n" +
            "            name:\"type\",\n" +
            "            label: \"Mail Type\",\n" +
            "            type:\"SelectBox\",\n" +
            "            required : \"true\",            \n" +
            "            options : [\n" +
            "                {value: 'mail', label : 'Mail'},\n" +
            "                {value: 'query', label : 'Query'}\n" +
            "            ]\n" +
            "        },{\n" +
            "        name : 'toSpecific',\n" +
            "        label : 'Email',\n" +
            "        type : 'textfield'\n" +
            "        },{ " +
            "        name : 'toParticipantId',\n" +
            "        label : 'Participant ID',\n" +
            "        type : 'textfield'\n" +
            "        }" +
            "    ] " +
            "}]";
    }
    
    @Override
    public Object execute(Map props) {
        WorkflowAssignment wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");     
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        String id = appService.getOriginProcessId(wfAssignment.getProcessId());

        LogUtil.info("== EMP-REG","Mail Send Tool Start "+id);
        
        CommonUtils.sendEmail("faiz.rashidi@tmsasia.com", "", 
                "TEST", "LINKIN PARK", null, null);
        
        return null;
    }

    private HashMap getMailData(DBHandler db, String mailId) {
        String query = "SELECT * FROM app_fd_empm_usr_mail WHERE id = ?";
        HashMap hm = db.selectOneRecord(query, new String[]{mailId});
        
        if(hm==null){
            return new HashMap();
        }
        
        return hm;
    }
    
}
