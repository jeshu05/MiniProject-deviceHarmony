// LogsAPIServlet.java
// Location: src/com/deviceharmony/web/LogsAPIServlet.java
package com.deviceharmony.web;

import com.deviceharmony.database.DatabaseManager;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import com.google.gson.*;

public class LogsAPIServlet extends HttpServlet {
    private DatabaseManager dbManager;
    private Gson gson = new Gson();
    
    public LogsAPIServlet(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            ResultSet rs = dbManager.getTransactionLogs(100);
            List<Map<String, Object>> logs = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> log = new HashMap<>();
                log.put("transaction_id", rs.getString("transaction_id"));
                log.put("source_name", rs.getString("source_name"));
                log.put("target_name", rs.getString("target_name"));
                log.put("file_name", rs.getString("file_name"));
                log.put("file_size", rs.getLong("file_size"));
                log.put("operation", rs.getString("operation"));
                log.put("status", rs.getString("status"));
                log.put("duration_ms", rs.getObject("duration_ms"));
                log.put("timestamp", rs.getTimestamp("timestamp").toString());
                log.put("error_message", rs.getString("error_message"));
                logs.add(log);
            }
            resp.getWriter().write(gson.toJson(logs));
        } catch (SQLException e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
