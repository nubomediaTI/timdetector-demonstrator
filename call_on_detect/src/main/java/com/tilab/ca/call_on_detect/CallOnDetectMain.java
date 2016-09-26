package com.tilab.ca.call_on_detect;

import com.tilab.ca.jrpcwrap.JrpcWsHandler;
import org.kurento.jsonrpc.internal.server.config.JsonRpcConfiguration;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;


@Import(JsonRpcConfiguration.class)
@SpringBootApplication
public class CallOnDetectMain implements JsonRpcConfigurer{
    
    @Bean
    public CallOnDetectHandler handler() throws Exception{
        return new CallOnDetectHandler();
    }
    
    @Bean
    public JrpcWsHandler<CallOnDetectHandler> handlerWrapper() throws Exception{
        return new JrpcWsHandler<>(handler());
    }
    
    public static void main(String[] args){
        new SpringApplication(CallOnDetectMain.class).run(args);
    }

    @Override
    public void registerJsonRpcHandlers(JsonRpcHandlerRegistry jrhr) {
        try {
            jrhr.addHandler(handlerWrapper(),"/callOnDetect");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
