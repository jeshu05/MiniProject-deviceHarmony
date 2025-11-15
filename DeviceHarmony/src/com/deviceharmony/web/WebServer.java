// WebServer.java
// Location: src/com/deviceharmony/web/WebServer.java
package com.deviceharmony.web;

import com.deviceharmony.database.DatabaseManager;
import com.deviceharmony.agent.AgentDiscovery;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class WebServer {
    private Server server;
    
    public WebServer(int port, DatabaseManager dbManager, AgentDiscovery agentDiscovery) {
        this.server = new Server(port);
        setupServlets(dbManager, agentDiscovery);
    }
    
    private void setupServlets(DatabaseManager dbManager, AgentDiscovery agentDiscovery) {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        
        // Main dashboard
        context.addServlet(new ServletHolder(new DashboardServlet()), "/");
        
        // API endpoints
        context.addServlet(new ServletHolder(new DevicesAPIServlet(dbManager, agentDiscovery)), "/api/devices");
        context.addServlet(new ServletHolder(new FilesAPIServlet(dbManager)), "/api/files/*");
        context.addServlet(new ServletHolder(new TransferAPIServlet(dbManager)), "/api/transfer");
        context.addServlet(new ServletHolder(new LogsAPIServlet(dbManager)), "/api/logs");
        context.addServlet(new ServletHolder(new PrimaryAPIServlet(dbManager)), "/api/primary");
        context.addServlet(new ServletHolder(new AddDeviceServlet(dbManager)), "/api/add-device");
    }
    
    public void start() throws Exception {
        server.start();
    }
    
    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
