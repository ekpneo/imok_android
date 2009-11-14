package com.jeztek.imok;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import android.util.Log;

public class HttpPost {
    
	private static final String DEBUG_ID = "IMOk.HttpPost";
	
    public String BOUNDARY = "--------multipart_formdata_boundary$--------";
    public String CRLF = "\r\n";

    private String url = null;
    private Map<String,String> vars = null;
    private Map<String,File> files = null;

    public HttpPost() {
    }
    
    public HttpPost(String url) {
        this.url = url;
    }

    public HttpPost(String url, Map<String,String> vars, Map<String,File> files) {
        this.url = url;
        this.vars = vars;
        this.files = files;
    }

    public String post() {
        if (this.url == null || this.vars == null || this.files == null)
            return null;
        return post(this.url, this.vars, this.files);
    }
    
    public String post(Map<String,String> vars, Map<String,File> files) {
        if (this.url == null) 
            return null;
        return post(this.url, vars, files);
    }

    public String post(String url, Map<String,String> vars, Map<String,File> files) {
        try {
            HttpURLConnection conn = (HttpURLConnection)(new URL(url)).openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            assembleMultipart(out, vars, files);
            
            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
        catch (UnsupportedEncodingException e) {
            return "Encoding exception: " + e;
        }
        catch (IOException e) {
            return "IOException: " + e;
        }
        catch (IllegalStateException e) {
            return "IllegalState: " + e;
        }
    }

    public void assembleMultipart(DataOutputStream out, Map<String,String> vars, Map<String,File> files) 
        throws IOException {
        for (String key : vars.keySet()) {
            out.writeBytes("--" + BOUNDARY + CRLF);
            out.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + CRLF);
            out.writeBytes(CRLF);
            out.writeBytes(vars.get(key) + CRLF); 
        }

        for (String key : files.keySet()) { 
            File f = files.get(key);
            out.writeBytes("--" + BOUNDARY + CRLF);
            out.writeBytes("Content-Disposition: form-data; name=\"" + key
                    + "\"; filename=\"" + f.getName() + "\"" + CRLF);
            out.writeBytes("Content-Type: application/octet-stream" + CRLF);
            out.writeBytes(CRLF);

            // write file
            int maxBufferSize = 1024;
            FileInputStream fin = new FileInputStream(f);
            int bytesAvailable = fin.available();
            int bufferSize = Math.min(maxBufferSize, bytesAvailable);
            byte[] buffer = new byte[bufferSize];

            int bytesRead = fin.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                out.write(buffer, 0, bufferSize);
                bytesAvailable = fin.available();
                bufferSize = Math.min(maxBufferSize, bytesAvailable);
                bytesRead = fin.read(buffer, 0, bufferSize);
            }
        }
        out.writeBytes(CRLF);
        out.writeBytes("--" + BOUNDARY + "--" + CRLF);
        out.writeBytes(CRLF);
    }
    
    // ---------------------------------------------------------------------------------------
    
    public void assembleMultipart(DataOutputStream out, Map<String,String> vars, String fileKey, String fileName, InputStream istream) 
        throws IOException {
        
        for (String key : vars.keySet()) {
            out.writeBytes("--" + BOUNDARY + CRLF);
            out.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + CRLF);
            out.writeBytes(CRLF);
            out.writeBytes(vars.get(key) + CRLF); 
        }

        if (fileName != null) {
        	out.writeBytes("--" + BOUNDARY + CRLF);
        	out.writeBytes("Content-Disposition: form-data; name=\"" + fileKey
        			+ "\"; filename=\"" + fileName + "\"" + CRLF);
        	out.writeBytes("Content-Type: application/octet-stream" + CRLF);
        	out.writeBytes(CRLF);

        	// write stream
        	//http://getablogger.blogspot.com/2008/01/android-how-to-post-file-to-php-server.html
        	int maxBufferSize = 1024;
        	int bytesAvailable = istream.available();
        	int bufferSize = Math.min(maxBufferSize, bytesAvailable);
        	byte[] buffer = new byte[bufferSize];

        	int bytesRead = istream.read(buffer, 0, bufferSize);
        	while (bytesRead > 0) {
        		out.write(buffer, 0, bufferSize);
        		out.flush();
        		bytesAvailable = istream.available();
        		bufferSize = Math.min(maxBufferSize, bytesAvailable);
        		bytesRead = istream.read(buffer, 0, bufferSize);
        	}
        }
        out.writeBytes(CRLF);
        out.writeBytes("--" + BOUNDARY + "--" + CRLF);
        out.writeBytes(CRLF);
    }
    
    public Map<String,String> post(String url, boolean useSSL, Map<String,String> vars, String fileKey, String fileName, InputStream istream) throws IOException {
        HttpURLConnection conn;
        
        try {
            if (useSSL) {
                DisableSSLCertificateCheckUtil.disableChecks();
                conn = (HttpsURLConnection)(new URL(url)).openConnection();
            }
            else {
                conn = (HttpURLConnection)(new URL(url)).openConnection();
            }
        } catch (IOException e) {
            throw new IOException("HttpPost - IOException: " + e);
        } catch (GeneralSecurityException e) {
            throw new IOException("HttpPost - disable ssl: " + e);
        }
                
        try {
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setAllowUserInteraction(true);

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            assembleMultipart(out, vars, fileKey, fileName, istream);
            if(istream != null) {
            	istream.close();
            }
            out.flush();
            
            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            
            StringBuilder sb = new StringBuilder();
            
            // Set postedSuccess to true if there is a line in the HTTP response in
            // the form "GEOCAM_SHARE_POSTED <file>" where <file> equals fileName.
            // Our old success condition was just checking for HTTP return code 200; turns
            // out that sometimes gives false positives.
            Boolean postedSuccess = false;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            	sb.append(line);
                if (!postedSuccess && line.startsWith("GEOCAM_SHARE_POSTED")) {
                    String[] vals = line.trim().split("\\s+");
                    String filePosted = vals[1];
                    if (filePosted.equals(fileName)) {
                        Log.d(DEBUG_ID, line);
                        postedSuccess = true;
                    }
                }
            }
            out.close();
        
            HashMap<String,String> retVal = new HashMap<String,String>();
            retVal.put("code", Integer.toString(conn.getResponseCode()));
            retVal.put("response", sb.toString());
            
            return retVal;
        }
        catch (UnsupportedEncodingException e) {
            throw new IOException("HttpPost - Encoding exception: " + e);
        }
        
        // ??? when would this ever be thrown?
        catch (IllegalStateException e) {
            throw new IOException("HttpPost - IllegalState: " + e);
        } 
        
        catch (IOException e) {
            try {
                HashMap<String,String> retVal = new HashMap<String,String>();
                retVal.put("code", Integer.toString(conn.getResponseCode()));
                return retVal;
            } catch(IOException f) {
                throw new IOException("HttpPost - IOException: " + e);
            }
        }
    }
}

