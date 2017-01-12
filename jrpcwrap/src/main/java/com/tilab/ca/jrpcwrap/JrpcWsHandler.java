package com.tilab.ca.jrpcwrap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tilab.ca.jrpcwrap.exception.AppException;
import com.tilab.ca.jrpcwrap.exception.ErrorCodes;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;


public class JrpcWsHandler<T extends JrpcEventListener> extends DefaultJsonRpcHandler<JsonObject>{

    private static final Logger log = LoggerFactory.getLogger(JrpcWsHandler.class);
    
    private final Map<String,Method> strMethodMap = new HashMap<>();
    private final T handlerObj;
    
    public JrpcWsHandler(T handlerObj) throws Exception{
        this.handlerObj = handlerObj;
        
        for(Method currMethod:handlerObj.getClass().getMethods()){
            JrpcMethod jm = currMethod.getAnnotation(JrpcMethod.class);
            if(jm!=null){
                strMethodMap.put(jm.name(),currMethod);
            }
        }
    }
    
    @Override
    public void handleRequest(Transaction transaction, Request<JsonObject> rqst) throws Exception {
        log.info("received request "+rqst.toString());
        try{
            if(strMethodMap.containsKey(rqst.getMethod())){
                Method calledMethod = strMethodMap.get(rqst.getMethod());
                Parameter[] params = calledMethod.getParameters();
                Object[] paramsVals = new Object[params.length];
                for(int i =0; i< params.length; i++){
                    if(params[i].getType().equals(Transaction.class)){
                        paramsVals[i] = transaction;
                    }else{
                        paramsVals[i] = getParamValueFromParamAnnotation(params[i],rqst);
                    }
                }
                    
                calledMethod.invoke(handlerObj, paramsVals);
            }else{
                throw new AppException(ErrorCodes.UNHANDLED_ERROR,String.format("method %s not supported", rqst.getMethod()));
            }
        }catch(Exception e){
            log.error("failed to handle request",e);
            ErrorCodes iec;
            if (e instanceof AppException) {
                AppException ae = (AppException) e;
                iec = ae.getErrorCode();
            } else {
                iec = ErrorCodes.UNHANDLED_ERROR;
            }
            transaction.sendError(iec.getCode(),iec.getMessage(),e.getMessage());
        }
        
    }

    private Object getParamValueFromParamAnnotation(Parameter param, Request<JsonObject> rqst) throws Exception{
       JsonKey jk = param.getAnnotation(JsonKey.class);
       if(jk == null)
           throw new IllegalArgumentException("missing jkey to identify parameter from request");
       
       if(rqst.getParams()==null)
    	   throw new IllegalArgumentException("request is empty but a content is expected from method");
       
        JsonElement je = rqst.getParams().get(jk.name());
        if(je== null){
            if(!jk.optional())
                throw new IllegalArgumentException("json request does not contain parameter with name "+jk.name());
            
            if(ClassUtils.isPrimitiveOrWrapper(param.getType()) && !ClassUtils.isPrimitiveWrapper(param.getType()))
                return ClassUtils.resolvePrimitiveIfNecessary(param.getType()).getConstructor().newInstance();
            return null;
        }
        if(param.getType().equals(JsonObject.class))
            return je.getAsJsonObject();
        else if(param.getType().equals(JsonArray.class))
            return je.getAsJsonArray();
        else
            return ClassUtils.resolvePrimitiveIfNecessary(param.getType())
                                .getConstructor(String.class).newInstance(je.getAsString());
    }

    @Override
    public void afterConnectionClosed(Session session, String status) throws Exception {
        handlerObj.afterConnectionClosed(session, status);
    }
}
