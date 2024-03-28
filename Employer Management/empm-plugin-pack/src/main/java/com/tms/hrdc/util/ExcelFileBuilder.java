/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author faizr
 */
public class ExcelFileBuilder {
    
    
    XSSFCellStyle headerStyle;
    XSSFFont mainFont;
    XSSFCellStyle commonStyle;
            
    public ExcelFileBuilder(){
    }
    
    private void initStyle(XSSFWorkbook workbook){
        
        mainFont = workbook.createFont();
        mainFont.setFontHeightInPoints((short)12);
        mainFont.setFontName("Arial");
        mainFont.setColor(IndexedColors.BLACK.getIndex());
        mainFont.setBold(true);
        mainFont.setItalic(false);
        
        XSSFFont sum_font = workbook.createFont();
        sum_font.setFontHeightInPoints((short)10);
        sum_font.setFontName("Arial");
        sum_font.setColor(IndexedColors.BLACK.getIndex());
        sum_font.setBold(true);
        
        XSSFCellStyle sum_amount_style = workbook.createCellStyle();          
        sum_amount_style.setBorderTop(BorderStyle.THIN);
        sum_amount_style.setBorderBottom(BorderStyle.THIN);
        sum_amount_style.setBorderLeft(BorderStyle.THIN);
        sum_amount_style.setBorderRight(BorderStyle.THIN);
        sum_amount_style.setFont(sum_font);
        sum_amount_style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        
        commonStyle = workbook.createCellStyle();   
        commonStyle.setBorderTop(BorderStyle.THIN);
        commonStyle.setBorderBottom(BorderStyle.THIN);
        commonStyle.setBorderLeft(BorderStyle.THIN);
        commonStyle.setBorderRight(BorderStyle.THIN);
        
        headerStyle = workbook.createCellStyle();  
        headerStyle.setBorderTop(BorderStyle.MEDIUM);
        headerStyle.setBorderBottom(BorderStyle.MEDIUM);
        headerStyle.setBorderLeft(BorderStyle.MEDIUM);
        headerStyle.setBorderRight(BorderStyle.MEDIUM);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setFont(mainFont);
    }
    
    public HashMap buildExcel(String fileName, String path, ArrayList<HashMap<String, String>> data) throws IOException{
        
        XSSFWorkbook workbook = new XSSFWorkbook();
        initStyle(workbook);
        
        String sheetName = "Potential Employer List";//Job Code *month *year
        XSSFSheet sheet = workbook.createSheet(sheetName);
        
        String name = "", section_code = "", amount = "", jt_code = "";
        int rowCount = 0;
        int itemCount = 1; 
        int columnCount = 0;
        int totalColSize = 0;
        
        Row row = sheet.createRow(++rowCount);
        
        Cell cell = row.createCell(++columnCount);        
        cell.setCellValue((String) "No. ");
        cell.setCellStyle(headerStyle);
        
        Iterator<Map.Entry<String, String>> iter = data.get(0).entrySet().iterator();
        
        while(iter.hasNext()){
            Map.Entry<String, String> entry = iter.next();
            
            cell = row.createCell(++columnCount);        
            cell.setCellValue((String) entry.getKey());
            cell.setCellStyle(headerStyle);
            
            totalColSize++;
        }
        
        for(int x=0;x<data.size();x++){
            
            row = sheet.createRow(++rowCount);
            columnCount = 0;
            
            iter = data.get(x).entrySet().iterator();
        
            while(iter.hasNext()){
                Map.Entry<String, String> entry = iter.next();

                cell = row.createCell(++columnCount);        
                cell.setCellValue((String) entry.getValue());
                cell.setCellStyle(commonStyle);
            }            
        }
        
        for(int i=0; i<totalColSize; i++){
            sheet.autoSizeColumn(i);
        }
        
        path = path+File.separator+fileName+".xlsx";
         
        try (FileOutputStream outputStream = new FileOutputStream(path)) {
            workbook.write(outputStream);
        }
        
        File file = new File(path);
        
        if(file.exists()){
            LogUtil.info("EXCEL FILE", "EXIST! "+file.getAbsolutePath());
        }else{
            LogUtil.info("EXCEL FILE", "NOT EXIST! "+path);
        }
        
        HashMap fileHm = new HashMap();
        fileHm.put("fileName", fileName+".xlsx");
        fileHm.put("type", "system");
        fileHm.put("path", file.getAbsolutePath());
        
        return fileHm;
    }
}
