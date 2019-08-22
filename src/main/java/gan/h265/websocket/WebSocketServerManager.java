package gan.h265.websocket;

import gan.core.DebugLog;
import gan.core.FileLogger;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketServerManager {

    private final static String Tag = WebSocketServerManager.class.getName();

    static {
        sInstance = new WebSocketServerManager();
    }
    private static WebSocketServerManager sInstance;

    ConcurrentHashMap<WebSocketSession,WebSocketServer>  mMapSesstions = new ConcurrentHashMap<>();

    public static WebSocketServerManager getsInstance() {
        return sInstance;
    }

    private WebSocketServerManager(){
    }

    public final static void fileLog(String tag,String msg){
        DebugLog.i(tag,msg);
        FileLogger.getInstance("websocket").log(tag+":"+msg);
    }

    /**
     * 连接建立成功调用的方法
     * @param session  可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    protected void onOpen(WebSocketSession session){
        DebugLog.i(Tag,"onOpen WebSocketSession id:"+session.getId());
        WebSocketServer server = new WebSocketServer();
        server.onCreate(session);
        mMapSesstions.put(session,server);
    }

    /**
     * 连接关闭调用的方法
     */
    protected void onClose(WebSocketSession session){
        WebSocketServer server = mMapSesstions.remove(session);
        if(server!=null){
            server.onDestory();
        }
    }

    /**
     * 收到客户端消息后调用的方法
     * @param message 客户端发送过来的消息
     * @param session 可选的参数
     */
    protected void onTextMessage(String message, WebSocketSession session) {
        DebugLog.d(Tag,"onTextMessage sessionid:"+session.getId()+";message:"+message);
        try {
            WebSocketServer server = mMapSesstions.get(session);
            if(server!=null){
                server.onMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
            DebugLog.d(Tag,"onTextMessage e:"+e.getMessage());
        }
    }

    protected void onBinaryMessage(BinaryMessage message, WebSocketSession session) {
        DebugLog.d(Tag,"onBinaryMessage sessionid:"+session.getId());
    }

    /**
     * 发生错误时调用
     * @param session
     * @param error
     */
    protected void onError(WebSocketSession session, Throwable error){
        error.printStackTrace();
        DebugLog.i(Tag,"onError sessionid:"+session.getId());
        onClose(session);
    }

}
