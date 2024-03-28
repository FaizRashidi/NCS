/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.test;

import com.tms.hrdc.util.DBHandler;
import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;

/**
 *
 * @author faizr
 */
public class CreateUnimportedTable extends DefaultApplicationPlugin{
    
    @Override
    public String getName() {
        return "EMPM Test Tool";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Test";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Test Tool";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }
    
    @Override
    public Object execute(Map map) {
       
        DBHandler db = new DBHandler();
        
        return null;
        
    }
}
