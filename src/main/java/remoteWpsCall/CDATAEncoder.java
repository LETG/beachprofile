package remoteWpsCall;

import java.io.*;

import org.geotools.xsd.EncoderDelegate;
import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

class CDATAEncoder implements EncoderDelegate { 
    String cData; 
 
    public CDATAEncoder(String cData) { 
        this.cData = cData; 
    } 
  
    public void encode(ContentHandler output) throws Exception { 
        ((LexicalHandler) output).startCDATA(); 
        Reader r = new StringReader(cData); 
        char[] buffer = new char[1024]; 
        int read; 
        while ((read = r.read(buffer)) > 0) { 
            output.characters(buffer, 0, read); 
        } 
        r.close(); 
        ((LexicalHandler) output).endCDATA(); 
    } 
} 
