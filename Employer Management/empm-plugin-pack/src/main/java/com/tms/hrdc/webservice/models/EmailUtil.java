/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.webservice.models;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class EmailUtil {
    
    HttpServletRequest request;
    HttpServletResponse response;
    DBHandler db;
    
    public EmailUtil(DBHandler d, HttpServletRequest req, HttpServletResponse res) throws SQLException {
        db = d;
        request = req;       
        response = res;
//        db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
    }
    
    public JSONObject trackOpenQR() throws JSONException, IOException, WriterException{  
        
        String hashed = request.getParameter("s")==null?"":URLDecoder.decode(request.getParameter("s"), "UTF-8");
        String link = request.getParameter("l")==null?"":URLDecoder.decode(request.getParameter("l"), "UTF-8");
        String browser = request.getParameter("browser")==null?"":URLDecoder.decode(request.getParameter("browser"), "UTF-8");
        
        if(StringUtils.isBlank(hashed)){
            link = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        }
        LogUtil.info("Email WS", "hash -> "+browser);
        int i = 0;
        if(!browser.isEmpty()){
            LogUtil.info("Email WS", "Has Hash, will update");
            String query = "UPDATE app_fd_empm_usr_mail SET c_is_seen = ? WHERE md5(concat(id, c_mail_fk)) = ?";
            i = db.update(query, new String[]{Constants.STATUS.EMAIL.OPENED}, new String[]{hashed});
        }     
        
        if(i==0){
            link = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        }
        
        LogUtil.info("QR CODE GENERATION", link);
        ServletOutputStream out = response.getOutputStream();
        response.setContentType("image/png");
        
        BufferedImage bi = createQR(link, "UTF-8", 200, 200);
        byte[] imgByte = bufferedImageToByteArray(bi);
        
        if (imgByte.length > 0) {
            LogUtil.info("QR CODE GENERATION", Integer.toString(imgByte.length));
            response.setContentLength(imgByte.length);
            out.write(imgByte);
            
            return new JSONObject().put("data", "SUCCESS");
        } else {
            return new JSONObject().put("data", "FAILED");
        }        
    }
    
    public JSONObject trackOpen() throws JSONException, IOException{  
        
        //update status here
        String hashed = request.getParameter("s")==null?"":URLDecoder.decode(request.getParameter("s"), "UTF-8");
//        String link = request.getParameter("l")==null?"":URLDecoder.decode(request.getParameter("l"), "UTF-8");
        String browser = request.getParameter("browser")==null?"":URLDecoder.decode(request.getParameter("browser"), "UTF-8");
        
        if(!browser.isEmpty()){ //if not viewed from browser, will return #xxxx#
            LogUtil.info("Email WS", "Has Hash, will update");
            String query = "UPDATE app_fd_empm_usr_mail SET c_mail_status= ?,c_is_seen = ?, dateModified = now() WHERE md5(concat(id, c_mail_fk)) = ?";
            int i = db.update(query, 
                    new String[]{Constants.STATUS.EMAIL.OPENED, Constants.STATUS.EMAIL.OPENED}, 
                    new String[]{hashed});
        }   
        
        InputStream tStream = getClass().getClassLoader().getResourceAsStream("img/"+Constants.IMGSRC.HRDC_400);
        
        OutputStream out = response.getOutputStream();
        response.setContentType("image/png");
        
        IOUtils.copy(tStream, out);       
        
        tStream.close();
        out.close();
        
        return new JSONObject().put("data", "SUCCESS");
    }
    
    public BufferedImage createQR(String data, String charset, int height, int width)
        throws WriterException, IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitMatrix matrix = new MultiFormatWriter().encode(
            new String(data.getBytes(charset), charset),
            BarcodeFormat.QR_CODE, width, height);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

        return image;
    }
    
    public byte[] bufferedImageToByteArray(BufferedImage bi) {
        byte[] imageInByte = null;

        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            baos.flush();
            imageInByte = baos.toByteArray();
            baos.close();
            
            Base64.getEncoder().encodeToString(baos.toByteArray());

	} catch(IOException ex){
            ex.printStackTrace();
	}

        return imageInByte;
    }
    
    
    
    protected void writeResponse(HttpServletResponse response, byte[] bytes, String filename, String contentType) throws IOException {
        ServletOutputStream servletOutputStream = response.getOutputStream();
        try {
            String name = URLEncoder.encode(filename, "UTF8").replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename=" + name + "; filename*=UTF-8''" + name);
            response.setContentType(contentType + "; charset=UTF-8");
            if (bytes.length > 0) {
                response.setContentLength(bytes.length);
                servletOutputStream.write(bytes);
            } 
        }
        finally {
            servletOutputStream.flush();
            servletOutputStream.close();
        } 
    }
    
//    public static String generateLink(DBHandler db, String link_type, String keyValue, String label, String mailId) throws UnsupportedEncodingException {
//        
//        String url = Constants.URL.BASE_URL+"/jw/web/userview/empm/emp/_/";        
//        String html_link = "<a href='";
//        
//        switch(link_type){
//            case Constants.LINK_TEMPLATE.LINK_FORM1:
//                url += Constants.URL.FORM1_LINK+"?empRegId="+keyValue;
//            break;
//            case Constants.LINK_TEMPLATE.LINK:
//                url += Constants.URL.FORM1_LINK+"?empRegId="+keyValue;
//            break;            
//            default:
//                url = Constants.URL.DEFAULT_URL;
//        }
//        
//        html_link += url + "'>" + label + "</a>";
//
//        if(label.equals("QR")){
//            //<img> of webservice that returns an img reps of qr
//            // in ws - update is_seen by empl id
//            
//            HashMap hm = db.selectOneRecord("select md5(concat(id,c_mail_fk)) as hashKey from app_fd_empm_usr_mail WHERE id = '"+mailId+"'");
//            
//            
//            String hashed = hm==null?"ERROR":hm.getOrDefault("hashKey", "ERROR").toString();
//            
//            LogUtil.info("email", "mailId "+mailId+", hash "+hashed);
//            String updAPI = Constants.URL.BASE_URL
//                            + "/jw/web/json/plugin/com.tms.hrdc.webservice.EmpmAPI/service?method=trackOpenQR"
//                            + "&s="+URLEncoder.encode(hashed, "UTF-8")
//                            + "&l="+URLEncoder.encode(url, "UTF-8")
//                            + "&browser=#requestParam.id#";
//
//            html_link = "<img src=\""+updAPI+"\" alt=\"Rick-Roll\">";
//        }
//        
//        return html_link;
//    }
    
}
