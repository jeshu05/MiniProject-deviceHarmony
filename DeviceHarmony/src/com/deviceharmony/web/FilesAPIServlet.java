// FilesAPIServlet.java
// Location: src/com/deviceharmony/web/FilesAPIServlet.java
package com.deviceharmony.web;

import com.deviceharmony.database.DatabaseManager;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import com.google.gson.*;

public class FilesAPIServlet extends HttpServlet {
    private DatabaseManager dbManager;
    private Gson gson = new Gson();
    
    public FilesAPIServlet(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = req.getPathInfo();
        resp.setContentType("application/json;charset=UTF-8");
        
        if ("/list".equals(action)) {
            listFiles(req, resp);
        } else if ("/download".equals(action)) {
            downloadFile(req, resp);
        }
    }
    
    private void listFiles(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String deviceId = req.getParameter("device");
        String path = req.getParameter("path");
        
        try {
            // Get device info from database
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(
                "SELECT * FROM devices WHERE device_id = ?"
            );
            pstmt.setString(1, deviceId);
            ResultSet rs = pstmt.executeQuery();
            
            if (!rs.next()) {
                resp.setStatus(404);
                resp.getWriter().write("{\"error\": \"Device not found\"}");
                return;
            }
            
            String deviceType = rs.getString("device_type");
            String ipAddress = rs.getString("ip_address");
            Integer port = rs.getObject("port") != null ? rs.getInt("port") : null;
            String sharePath = rs.getString("share_path");
            
            List<Map<String, Object>> fileList = new ArrayList<>();
            
            if ("agent".equals(deviceType) && ipAddress != null && port != null) {
                // Fetch from agent
                String url = String.format("http://%s:%d/files/list?path=%s", 
                    ipAddress, port, java.net.URLEncoder.encode(path, "UTF-8"));
                fileList = fetchFromAgent(url);
            } else if ("share".equals(deviceType) || "manual".equals(deviceType)) {
                // Access local/network path
                String fullPath = sharePath != null ? sharePath + path : path;
                fileList = listLocalFiles(fullPath);
            }
            
            resp.getWriter().write(gson.toJson(fileList));
            
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    private void downloadFile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String deviceId = req.getParameter("device");
        String filePath = req.getParameter("path");
        
        long startTime = System.currentTimeMillis();
        String transactionId = UUID.randomUUID().toString();
        
        System.out.println("Download request - Device: " + deviceId + ", Path: " + filePath);
        
        try {
            // Get device info
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(
                "SELECT * FROM devices WHERE device_id = ?"
            );
            pstmt.setString(1, deviceId);
            ResultSet rs = pstmt.executeQuery();
            
            if (!rs.next()) {
                resp.setStatus(404);
                return;
            }
            
            String deviceType = rs.getString("device_type");
            String ipAddress = rs.getString("ip_address");
            Integer port = rs.getObject("port") != null ? rs.getInt("port") : null;
            String sharePath = rs.getString("share_path");
            
            File file = null;
            InputStream inputStream = null;
            
            if ("agent".equals(deviceType) && ipAddress != null && port != null) {
                // Download from agent
                String url = String.format("http://%s:%d/files/download?path=%s",
                    ipAddress, port, java.net.URLEncoder.encode(filePath, "UTF-8"));
                java.net.URL agentUrl = new java.net.URL(url);
                inputStream = agentUrl.openStream();
            } else {
                // Access local/network file
                String fullPath = sharePath != null ? sharePath + filePath : filePath;
                file = new File(fullPath);
                if (!file.exists() || file.isDirectory()) {
                    resp.setStatus(404);
                    return;
                }
                inputStream = new FileInputStream(file);
            }
            
            String fileName = new File(filePath).getName();
            long fileSize = file != null ? file.length() : 0;
            
            resp.setContentType("application/octet-stream");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            if (fileSize > 0) {
                resp.setContentLengthLong(fileSize);
            }
            
            try (OutputStream os = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                fileSize = totalBytes;
            } finally {
                inputStream.close();
            }
            
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("Download successful - File: " + fileName + ", Size: " + fileSize + ", Duration: " + duration + "ms");
            
            // Log transaction (target is null since file is downloaded to client/browser)
            try {
                dbManager.logTransaction(transactionId, deviceId, null, 
                    fileName, filePath, fileSize, "download", "success", duration, null);
                System.out.println("Transaction logged successfully");
            } catch (Exception logEx) {
                System.err.println("Failed to log transaction: " + logEx.getMessage());
                logEx.printStackTrace();
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("Download failed: " + e.getMessage());
            e.printStackTrace();
            
            try {
                dbManager.logTransaction(transactionId, deviceId, null,
                    new File(filePath).getName(), filePath, 0, "download", 
                    "error", duration, e.getMessage());
                System.out.println("Error transaction logged");
            } catch (Exception ex) {
                System.err.println("Failed to log error transaction: " + ex.getMessage());
                ex.printStackTrace();
            }
            resp.setStatus(500);
        }
    }
    
    private List<Map<String, Object>> fetchFromAgent(String url) throws IOException {
        try {
            java.net.URL agentUrl = new java.net.URL(url);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(agentUrl.openStream())
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            return gson.fromJson(response.toString(), List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    private List<Map<String, Object>> listLocalFiles(String path) {
        List<Map<String, Object>> fileList = new ArrayList<>();
        
        try {
            File dir = new File(path);
            if (!dir.exists() || !dir.isDirectory()) {
                return fileList;
            }
            
            File[] files = dir.listFiles();
            if (files == null) return fileList;
            
            for (File file : files) {
                if (file.isHidden()) continue;
                
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("name", file.getName());
                fileInfo.put("path", file.getAbsolutePath());
                fileInfo.put("isDirectory", file.isDirectory());
                fileInfo.put("size", file.length());
                fileInfo.put("lastModified", file.lastModified());
                fileList.add(fileInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return fileList;
    }
}