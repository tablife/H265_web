package gan.h265.websocket;

import android.os.JSONObject;
import gan.core.DebugLog;
import gan.core.TextUtils;
import gan.h265.H265Server;
import gan.h265.utils.HUtils;
import gan.network.NetParamsMap;
import gan.system.SystemUtils;
import gan.system.server.BaseServer;
import gan.system.server.SystemServer;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.UUID;

public class WebSocketServer extends BaseServer {

    final static String Tag = WebSocketServer.class.getName();
    final static String end = "\r\n";

    protected WebSocketSession mSession;
    private String mSessionId;
    private String mToken;
    private boolean mOutputStreaming;

    protected void onCreate(WebSocketSession session) {
        super.onCreate();
        DebugLog.i(Tag,"onCreate:"+session.getId());
        mSession = session;
        mSessionId  =  session.getId();
    }

    @Override
    protected void onDestory() {
        super.onDestory();
        SystemUtils.close(mSession);
        DebugLog.i(Tag,"onDestory:"+mSessionId);
        if(mOutputStreaming){
            stopOutputStream();
        }
    }

    protected void onMessage(String message)throws IOException{
        DebugLog.d(Tag,"session:"+mSession.getId()+",recevie message:"+message);
        if(mOutputStreaming){
            return;
        }

        try{
            JSONObject jo = new JSONObject(message);
            mToken = jo.optString("url");
            startOutputStream(mToken,jo);
        }catch (Exception e){
            mToken = message;
            startOutputStream(mToken,new JSONObject());
        }
    }

    public synchronized void startOutputStream(String token,final JSONObject jo)throws IOException{
        if(mOutputStreaming){
            return;
        }

        sendMessage(new MessageResult()
                .asOk());

        mOutputStreaming = true;

        if(!TextUtils.isEmpty(token)){
            mFrameBuffer = ByteBuffer.allocate(1024000);
            mTempBuffer = new byte[1024000];
            FileInputStream fis=null;
            try{
                String filePath = H265Server.getPublicPath(token);
                fis = new FileInputStream(filePath);
                int len;
                byte[] buf = new byte[4096];
                while (mOutputStreaming
                        && ((len=fis.read(buf))!=-1)){
                    putData(buf,0,len);
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                SystemUtils.close(fis);
            }
        }
    }

    public void stopOutputStream(){
        mOutputStreaming = false;
        mFrameBuffer.clear();
    }

    ByteBuffer mFrameBuffer;
    byte[] mTempBuffer;
    int tempLen;
    private void putData(byte[] packet, int offset,int len){
        putData((byte)0,packet,offset,len, System.currentTimeMillis());
        mFrameBuffer.put(packet,0,len);
        int frameLen = HUtils.find2frameLen(mFrameBuffer.array(), 0, mFrameBuffer.position());
        if(frameLen>0){
            putFrame((byte)0, mFrameBuffer.array(), 0, frameLen, 0);
            System.arraycopy(mFrameBuffer.array(), frameLen, mTempBuffer, 0, tempLen = (mFrameBuffer.position() - frameLen));
            mFrameBuffer.clear();
            mFrameBuffer.put(mTempBuffer,0, tempLen);
        }
    }

    public final void putData(byte channel, byte[] packet, int offset,int len,long time){
//        try {
//            sendMessage(packet,offset,len);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public final void putFrame(byte channel, byte[] packet, int offset,int len,long time){
        try {
            sendMessage(packet,offset,len);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Object object)throws IOException{
        sendMessage(SystemServer.getObjectMapper().writeValueAsString(object));
    }

    public void sendMessage(String text)throws IOException{
        mSession.sendMessage(new TextMessage(text));
    }

    @Override
    public void sendMessage(int b) throws IOException {
    }

    @Override
    public void sendMessage(byte[] b) throws IOException {
    }

    @Override
    public void sendMessage(byte[] b, int off, int len) throws IOException {
    }

    public final static String formatWebsocketMediaSessionId(String id){
        return "session_websocket_"+id+UUID.randomUUID().toString();
    }

    public static String parseRequest(String request, NetParamsMap params) throws IOException {
        BufferedReader sr = new BufferedReader(new StringReader(request));
        String str = "";
        String fun="";
        while (!TextUtils.isEmpty(str=sr.readLine())){
            if(str.startsWith("WSP")){
                fun = str.trim();
                continue;
            }
            if(str.contains(":")){
                String[] map = str.split(":");
                if(map.length>1){
                    params.put(map[0].trim(),map[1].trim());
                }
            }
        }
        return fun;
    }

    public int responseRequest(int code,String message)throws IOException{
        return responseRequest(mSession, code, message,null);
    }

    public int responseRequest(int code,String message,String content)throws IOException{
        return responseRequest(mSession, code, message,content);
    }

    public static int responseRequest(WebSocketSession session,int code,String message,String content)throws IOException{
        StringBuffer sb = new StringBuffer();
        String responseHead = "WSP/1.1 "+ code + " "+ code(code) + end;
        sb.append(responseHead)
                .append(message)
                .append(end);
        if(!TextUtils.isEmpty(content)){
            sb.append(content);
        }
        session.sendMessage(new TextMessage(sb.toString()));
        return code;
    }

    public static String code(int code){
        switch (code){
            case 200:
                return "OK";
            case 400:
                return "400 means error";
            default:
                return "Error";
        }
    }

    public String getChannel(){
        return mSession.getId();
    }

}
