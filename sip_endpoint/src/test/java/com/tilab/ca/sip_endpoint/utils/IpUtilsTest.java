/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tilab.ca.sip_endpoint.utils;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kurento
 */
public class IpUtilsTest {
    
    
    /**
     * Test of getKmsIpFromUrl method, of class IpUtils.
     */
    @Test
    public void testGetKmsIpFromUrl() {
        System.out.println("getKmsIpFromUrl");
        String kmsUrl = "ws://127.0.0.1:8888";
        String expResult = "127.0.0.1";
        String result = IpUtils.getKmsIpFromUrl(kmsUrl);
        assertEquals(expResult, result);
        
        kmsUrl = "ws://localhost:8888";
        expResult = "127.0.0.1";
        result = IpUtils.getKmsIpFromUrl(kmsUrl);
        assertEquals(expResult, result);
        
    }

    /**
     * Test of isPrivateV4OrLoopbackAddress method, of class IpUtils.
     */
    @Test
    public void testIsPrivateV4OrLoopbackAddress() {
        System.out.println("isPrivateV4OrLoopbackAddress");
        
        String ip = "192.168.1.1";
        boolean expResult = true;
        boolean result = IpUtils.isPrivateV4OrLoopbackAddress(ip);
        assertEquals(expResult, result);
        
        ip = "192.169.1.1";
        expResult = false;
        result = IpUtils.isPrivateV4OrLoopbackAddress(ip);
        assertEquals(expResult, result);
        
        ip = "127.0.0.1";
        expResult = true;
        result = IpUtils.isPrivateV4OrLoopbackAddress(ip);
        assertEquals(expResult, result);
    }
    
}
