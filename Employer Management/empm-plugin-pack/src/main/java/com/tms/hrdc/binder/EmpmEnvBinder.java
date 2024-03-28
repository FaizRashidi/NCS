package com.tms.hrdc.binder;

import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import org.apache.commons.lang.StringUtils;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;

public class EmpmEnvBinder extends WorkflowFormBinder {

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
        return "Used in Employer Management to update Wflow Env Var from Setup";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Env Var Mgmt Binder";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    public void msg(String msg) {
        LogUtil.info(getName(), msg);
    }

    @Override
    public FormRowSet store(Element element, FormRowSet rowSet, FormData formData) {
        FormRow row = rowSet.get(0);

        String envId = row.get("id")==null?"":row.get("id").toString();
        String value = row.get("value")==null?"":row.get("value").toString();

        if(envId.isEmpty()){
            return null;
        }

        CommonUtils.setEnvVar(Constants.APP_ID.EMPM, envId, value);
        return null;
    }

}
