/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tilab.ca.jrpcwrap;

import org.kurento.jsonrpc.Session;

/**
 *
 * @author kurento
 */
public interface JrpcEventListener {
    void afterConnectionClosed(Session session, String status);
}
