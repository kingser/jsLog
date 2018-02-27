package com.yc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
 
//该注解用来指定一个URI，客户端可以通过这个URI来连接到WebSocket。类似Servlet的注解mapping。无需在web.xml中配置。
@ServerEndpoint("/logUtil/{username}")
public class LogUtil {
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;
     
    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
    private static Map<String, LogUtil> clients = new ConcurrentHashMap<String, LogUtil>();  
    private Session session;  
    private String username;  
    private static Map<String,String> userList = new HashMap<String,String>();
    {
    	userList.put("admin", "e10adc3949ba59abbe56e057f20f883e");
    }
  
     
    /**
     * 连接建立成功调用的方法
     * @param session  可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    @OnOpen
    public void onOpen(@PathParam("username") String username,Session session){

        this.username = username;  
        this.session = session;  
        clients.put(username, this);  
        addOnlineCount();           //在线数加1
        System.out.println("有新连接加入！当前在线人数为" + getOnlineCount());
        
        
    }
     
    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(){
	    clients.remove(username);  //从map中删除
        subOnlineCount();           //在线数减1    
        System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
    }
     
    /**
     * 收到客户端消息后调用的方法
     * @param message 客户端发送过来的消息
     * @param session 可选的参数
     * @throws IOException 
     */
    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
    	 System.out.println("message:"+message);
    	 JSONObject msg = JSONObject.fromObject(message);
    	 String msgType = msg.getString("msgType");
    	 
    	 if("cmd".equals(msgType)&&checkPermission(msg)){
    		 String  cmd = msg.getString("cmd");
    		 if("getUserList".equals(cmd)){//获取在线的用户列表
	        	 JSONObject sendMsg = new JSONObject();
	             sendMsg.put("msgType", "text");
	             
	             JSONArray userList = new JSONArray();
	        	 for (LogUtil item : clients.values()) { 
	        		JSONObject itetmObj = new JSONObject();
	        		itetmObj.put("username",item.username);
	        		userList.add(itetmObj);
	             } 
	        	 sendMsg.put("userList",userList);
	        	 session.getAsyncRemote().sendText(sendMsg.toString());  
    			 return;
    		 }
    		 if("getLog".equals(cmd)){//获取该用户的日志
    			 String  target= msg.getString("target");
                 JSONObject sendMsg = new JSONObject();
                 sendMsg.put("msgType", "cmd");
                 sendMsg.put("cmd", "getLog");
            	 sendMessageTo(sendMsg.toString(),target);
            	 return ;
    		 }
    	 }else if("logSubmit".equals(msgType)){
    		 JSONObject sendMsg = new JSONObject();
             sendMsg.put("msgType", "log");
             sendMsg.put("log", msg.getString("msg"));
             sendMsg.put("logUser", msg.getString("logUser"));
             sendMessageTo(sendMsg.toString(),"admin"); 
    		
    	 }
    }
     
    /**
     * 发生错误时调用
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error){
        System.out.println("发生错误");
        error.printStackTrace();
    }
     
    /**
     * 这个方法与上面几个方法不一样。没有用注解，是根据自己需要添加的方法。
     * @param message
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException{
        this.session.getBasicRemote().sendText(message);
        //this.session.getAsyncRemote().sendText(message);
    }
 
    public static synchronized int getOnlineCount() {
        return onlineCount;
    }
 
    public static synchronized void addOnlineCount() {
    	LogUtil.onlineCount++;
    }
     
    public static synchronized void subOnlineCount() {
    	LogUtil.onlineCount--;
    }
    public void sendMessageTo(String message, String to) throws IOException {  
    	LogUtil item = clients.get(to);
    	item.session.getAsyncRemote().sendText(message); 
    }  
      
    public void sendMessageAll(String message) throws IOException {  
        for (LogUtil item : clients.values()) {  
            item.session.getAsyncRemote().sendText(message);  
        }  
    }  
    
    public boolean checkPermission(JSONObject msg){
    	String username = msg.getString("username");
    	String password = msg.getString("password");
    	String perPassword = userList.get(username);
    	if(password!=null&&perPassword!=null){
    		String md5Str = MD5Util.md5(password);
    		if(md5Str.equalsIgnoreCase(perPassword)){
    			return true;
    		}
    	}
    	return false;
    	
    }
}