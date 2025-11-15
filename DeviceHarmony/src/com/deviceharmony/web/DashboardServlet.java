// DashboardServlet.java
// Location: src/com/deviceharmony/web/DashboardServlet.java
package com.deviceharmony.web;

import javax.servlet.http.*;
import java.io.IOException;

public class DashboardServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write(getHTML());
    }
    
    private String getHTML() {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>DeviceHarmony - Central Dashboard</title>" +
            "<style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#0f172a;color:#e2e8f0}" +
            ".header{background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:2rem;text-align:center;box-shadow:0 4px 6px rgba(0,0,0,0.3)}" +
            ".header h1{font-size:2.5rem;margin-bottom:0.5rem}.header p{font-size:1.1rem;opacity:0.9}" +
            ".container{max-width:1600px;margin:2rem auto;padding:0 1rem}" +
            ".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(350px,1fr));gap:1.5rem;margin-bottom:2rem}" +
            ".card{background:#1e293b;border-radius:12px;padding:1.5rem;box-shadow:0 4px 6px rgba(0,0,0,0.2)}" +
            ".card h2{font-size:1.4rem;margin-bottom:1rem;color:#94a3b8;border-bottom:2px solid #334155;padding-bottom:0.75rem;display:flex;justify-content:space-between;align-items:center}" +
            ".device{background:#334155;padding:1.25rem;border-radius:8px;margin-bottom:0.75rem;transition:all 0.2s;position:relative}" +
            ".device:hover{background:#475569;transform:translateY(-2px)}" +
            ".device.primary{border:2px solid #667eea;background:#2d3e5f}" +
            ".device.agent{border-left:4px solid #10b981}.device.share{border-left:4px solid #f59e0b}.device.manual{border-left:4px solid#64748b}" +
            ".device-name{font-weight:600;margin-bottom:0.5rem;font-size:1.15rem;display:flex;align-items:center;gap:0.5rem}" +
            ".device-info{font-size:0.9rem;color:#94a3b8;margin:0.25rem 0}" +
            ".device-type{display:inline-block;padding:0.25rem 0.5rem;border-radius:4px;font-size:0.75rem;font-weight:600;margin-left:0.5rem}" +
            ".type-agent{background:#10b981;color:white}.type-share{background:#f59e0b;color:white}.type-manual{background:#64748b;color:white}" +
            ".status{display:inline-block;width:10px;height:10px;border-radius:50%;margin-right:0.5rem}" +
            ".status.online{background:#10b981;box-shadow:0 0 8px #10b981}.status.offline{background:#ef4444}" +
            ".btn{background:#667eea;color:white;border:none;padding:0.6rem 1.2rem;border-radius:6px;cursor:pointer;font-size:0.9rem;margin:0.25rem;font-weight:500;transition:all 0.2s}" +
            ".btn:hover{background:#5568d3;transform:translateY(-1px)}.btn-sm{padding:0.4rem 0.8rem;font-size:0.85rem}" +
            ".btn-danger{background:#ef4444}.btn-danger:hover{background:#dc2626}" +
            ".btn-add{background:#10b981}.btn-add:hover{background:#059669}" +
            ".file-list{max-height:450px;overflow-y:auto}" +
            ".file-item{background:#334155;padding:0.85rem;border-radius:6px;margin-bottom:0.5rem;display:flex;justify-content:space-between;align-items:center}" +
            ".file-item:hover{background:#3f4f66}" +
            ".log-item{background:#334155;padding:0.85rem;border-radius:6px;margin-bottom:0.5rem;font-size:0.9rem}" +
            ".log-status{display:inline-block;padding:0.3rem 0.6rem;border-radius:4px;font-size:0.75rem;font-weight:600;margin-right:0.5rem}" +
            ".log-status.success{background:#10b981;color:white}.log-status.error{background:#ef4444;color:white}.log-status.pending{background:#f59e0b;color:white}" +
            ".empty{text-align:center;padding:3rem;color:#64748b;font-size:1.1rem}" +
            "#selectedDevice{margin-bottom:1rem;padding:1.2rem;background:#334155;border-radius:8px;font-size:1.05rem}" +
            ".modal{display:none;position:fixed;z-index:1000;left:0;top:0;width:100%;height:100%;background:rgba(0,0,0,0.7);backdrop-filter:blur(4px)}" +
            ".modal-content{background:#1e293b;margin:5% auto;padding:2rem;border-radius:12px;max-width:600px;box-shadow:0 8px 16px rgba(0,0,0,0.3)}" +
            ".modal-header{font-size:1.5rem;margin-bottom:1.5rem;color:#e2e8f0}" +
            ".form-group{margin-bottom:1.25rem}.form-group label{display:block;margin-bottom:0.5rem;color:#94a3b8;font-weight:500}" +
            ".form-group input,.form-group select{width:100%;padding:0.75rem;border-radius:6px;border:1px solid #334155;background:#0f172a;color:#e2e8f0;font-size:1rem}" +
            ".form-actions{display:flex;gap:1rem;justify-content:flex-end;margin-top:1.5rem}" +
            ".close{color:#94a3b8;float:right;font-size:2rem;font-weight:bold;cursor:pointer}.close:hover{color:#e2e8f0}" +
            "::-webkit-scrollbar{width:8px}::-webkit-scrollbar-track{background:#1e293b}::-webkit-scrollbar-thumb{background:#475569;border-radius:4px}" +
            "</style></head><body>" +
            "<div class='header'><h1>🔗 DeviceHarmony</h1><p>Centralized Multi-Device File Management System</p></div>" +
            "<div class='container'><div class='grid'>" +
            "<div class='card'><h2>Connected Devices<button class='btn btn-sm btn-add' onclick='showAddDeviceModal()'>+ Add Device</button></h2><div id='deviceList'></div></div>" +
            "<div class='card'><h2>File Browser</h2><div id='selectedDevice'>Select a device to browse files</div>" +
            "<div id='currentPath'></div><div class='file-list' id='fileList'></div></div></div>" +
            "<div class='card'><h2>Recent Transactions</h2><div id='logList'></div></div></div>" +
            "<div id='addDeviceModal' class='modal'><div class='modal-content'><span class='close' onclick='closeAddDeviceModal()'>&times;</span>" +
            "<div class='modal-header'>Add New Device</div>" +
            "<div class='form-group'><label>Device Type:</label><select id='deviceType' onchange='updateFormFields()'>" +
            "<option value='share'>Network Share (SMB/NFS)</option><option value='manual'>Manual Path</option></select></div>" +
            "<div class='form-group'><label>Device Name:</label><input type='text' id='deviceName' placeholder='My Laptop'/></div>" +
            "<div class='form-group' id='sharePathGroup'><label>Network Share Path:</label>" +
            "<input type='text' id='sharePath' placeholder='//192.168.1.100/Share or /mnt/share'/></div>" +
            "<div class='form-group' id='ipGroup' style='display:none'><label>IP Address (optional):</label>" +
            "<input type='text' id='ipAddress' placeholder='192.168.1.100'/></div>" +
            "<div class='form-actions'><button class='btn' onclick='closeAddDeviceModal()'>Cancel</button>" +
            "<button class='btn btn-add' onclick='addDevice()'>Add Device</button></div></div></div>" +
            "<script>let selectedDevice=null,currentPath='/';function loadDevices(){fetch('/api/devices').then(r=>r.json()).then(data=>{" +
            "const html=data.map(d=>`<div class='device ${d.is_primary?'primary':''} ${d.device_type}' onclick='selectDevice(${JSON.stringify(d)})'>" +
            "<div class='device-name'><span class='status ${d.status}'></span>${d.device_name}<span class='device-type type-${d.device_type}'>${d.device_type.toUpperCase()}</span>" +
            "${d.is_primary?'⭐':''}</div><div class='device-info'>${d.device_type=='agent'?d.ip_address+':'+d.port:d.share_path||'Local'}</div>" +
            "<div class='device-info'>Storage: ${d.available_storage?formatBytes(d.available_storage)+' / '+formatBytes(d.total_storage):'N/A'}</div>" +
            "<div style='margin-top:0.5rem'>${!d.is_primary?`<button class='btn btn-sm' onclick='setPrimary(\"${d.device_id}\",event)'>Set Primary</button>`:''}" +
            "<button class='btn btn-sm btn-danger' onclick='deleteDevice(\"${d.device_id}\",event)'>Remove</button></div></div>`).join('');" +
            "document.getElementById('deviceList').innerHTML=html||'<div class=\"empty\">No devices connected<br><small>Click \"+ Add Device\" to get started</small></div>'})};" +
            "function selectDevice(device){selectedDevice=device;currentPath='/';document.getElementById('selectedDevice').innerHTML=" +
            "`<strong>Browsing:</strong> ${device.device_name} (${device.device_type})`;loadFiles()};" +
            "function loadFiles(){if(!selectedDevice)return;document.getElementById('currentPath').innerHTML=`<div style='margin-bottom:1rem'>" +
            "<strong>Path:</strong> ${currentPath} <button class='btn btn-sm' onclick='refreshFiles()'>🔄 Refresh</button></div>`;" +
            "fetch(`/api/files/list?device=${selectedDevice.device_id}&path=${encodeURIComponent(currentPath)}`).then(r=>r.json()).then(data=>{" +
            "let html='';if(currentPath!=='/'){html+=`<div class='file-item'><span>📁 ..</span>" +
            "<button class='btn btn-sm' onclick='navigateUp()'>⬆ Back</button></div>`}" +
            "html+=data.map(f=>`<div class='file-item'><span>${f.isDirectory?'📁':'📄'} ${f.name} " +
            "${!f.isDirectory?'('+formatBytes(f.size)+')':''}</span><div>${f.isDirectory?" +
            "`<button class='btn btn-sm' onclick='openFolder(\"${f.path.replace(/\\\\/g,'\\\\\\\\')}\")'>Open</button>`:" +
            "`<button class='btn btn-sm' onclick='downloadFile(\"${f.path.replace(/\\\\/g,'\\\\\\\\')}\")'>⬇ Download</button>`}</div></div>`).join('');" +
            "document.getElementById('fileList').innerHTML=html||'<div class=\"empty\">No files found</div>'})};" +
            "function openFolder(path){currentPath=path;loadFiles()}function navigateUp(){const idx=currentPath.lastIndexOf('/');currentPath=idx>0?currentPath.substring(0,idx):'/';loadFiles()}" +
            "function refreshFiles(){loadFiles()}function downloadFile(path){const url=`/api/files/download?device=${selectedDevice.device_id}&path=${encodeURIComponent(path)}`;" +
            "const a=document.createElement('a');a.href=url;a.download=path.split('/').pop();document.body.appendChild(a);a.click();document.body.removeChild(a);setTimeout(loadLogs,500)}" +
            "function setPrimary(id,e){e.stopPropagation();fetch('/api/primary',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({deviceId:id})}).then(()=>loadDevices())}" +
            "function deleteDevice(id,e){e.stopPropagation();if(confirm('Remove this device?')){fetch('/api/devices?id='+id,{method:'DELETE'}).then(()=>{loadDevices();if(selectedDevice&&selectedDevice.device_id===id){selectedDevice=null;" +
            "document.getElementById('selectedDevice').innerHTML='Select a device to browse files';document.getElementById('fileList').innerHTML='';document.getElementById('currentPath').innerHTML=''}})}}" +
            "function loadLogs(){fetch('/api/logs').then(r=>r.json()).then(data=>{const html=data.map(l=>`<div class='log-item'>" +
            "<div><span class='log-status ${l.status}'>${l.status.toUpperCase()}</span>${l.operation} ${l.duration_ms?'('+l.duration_ms+'ms)':''}</div>" +
            "<div style='margin-top:0.5rem;color:#94a3b8'>${l.source_name||'N/A'} → ${l.target_name||'Server'}: ${l.file_name} (${formatBytes(l.file_size)})</div>" +
            "<div style='margin-top:0.25rem;color:#64748b;font-size:0.8rem'>${new Date(l.timestamp).toLocaleString()}</div>" +
            "${l.error_message?`<div style='margin-top:0.5rem;color:#ef4444;font-size:0.85rem'>Error: ${l.error_message}</div>`:''}</div>`).join('');" +
            "document.getElementById('logList').innerHTML=html||'<div class=\"empty\">No transactions yet</div>'})};" +
            "function formatBytes(bytes){if(!bytes)return '0 B';const k=1024,sizes=['B','KB','MB','GB','TB']," +
            "i=Math.floor(Math.log(bytes)/Math.log(k));return(bytes/Math.pow(k,i)).toFixed(2)+' '+sizes[i]}" +
            "function showAddDeviceModal(){document.getElementById('addDeviceModal').style.display='block'}" +
            "function closeAddDeviceModal(){document.getElementById('addDeviceModal').style.display='none'}" +
            "function updateFormFields(){const type=document.getElementById('deviceType').value;" +
            "document.getElementById('sharePathGroup').style.display=type==='share'?'block':'none';" +
            "document.getElementById('ipGroup').style.display=type==='manual'?'block':'none'}" +
            "function addDevice(){const type=document.getElementById('deviceType').value," +
            "name=document.getElementById('deviceName').value,sharePath=document.getElementById('sharePath').value," +
            "ipAddress=document.getElementById('ipAddress').value;if(!name){alert('Please enter device name');return}" +
            "if(type==='share'&&!sharePath){alert('Please enter share path');return}" +
            "fetch('/api/add-device',{method:'POST',headers:{'Content-Type':'application/json'}," +
            "body:JSON.stringify({deviceName:name,deviceType:type,sharePath:sharePath||null,ipAddress:ipAddress||null})})" +
            ".then(r=>r.json()).then(data=>{if(data.success){closeAddDeviceModal();loadDevices();document.getElementById('deviceName').value='';" +
            "document.getElementById('sharePath').value='';document.getElementById('ipAddress').value=''}else{alert('Error: '+data.error)}})};" +
            "loadDevices();loadLogs();setInterval(loadDevices,5000);setInterval(loadLogs,5000)</script></body></html>";
    }
}