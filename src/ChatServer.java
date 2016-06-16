import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    //定义ServerSocket的端口号

    private static final int SOCKET_PORT = 9000;

    private  ExecutorService executorService = Executors.newFixedThreadPool(5);//创建一个可重用固定线程数的线程池

    private  List<UserInfo> users = new ArrayList<UserInfo>();

    public Server getServer(){
        return new Server();
    }

    public synchronized UserInfo containsUser(String account){
        for(int i=0;i<users.size();i++){
            if(users.get(i).account.equals(account)){
                return users.get(i);
            }
        }
        return null;
    }

    public synchronized boolean updateUserInfo(String account,UserInfo us){
        for(int i=0;i<users.size();i++){
            if(users.get(i).account.equals(account)){
                users.set(i,us);
                return true;
            }
        }
        return false;
    }

    public class Server extends Thread{
        ServerSocket serverSocket = null;
        ObjectOutputStream objectOutputStream = null;
        ObjectInputStream objectInputStream = null;

        @Override
        public void run() {
            super.run();

            try {
                serverSocket = new ServerSocket(SOCKET_PORT);
                System.out.print("启动服务器成功");
                while (true){
                    System.out.println("等待客户端");
                    Socket client = serverSocket.accept();
                    System.out.println("连接成功" + client.toString());
                    ListenerClient listenerClient = new ListenerClient(client);
                    executorService.submit(listenerClient);
                }
            } catch (IOException e) {
                System.out.println("启动服务器失败");
                e.printStackTrace();
            }
        }

        public synchronized void sendMsg(String msg) {
            try {
                for (int i = 0; i < users.size(); i++) {
                    Socket client = users.get(i).socket;
                    objectOutputStream = new ObjectOutputStream(client.getOutputStream());
                    System.out.println("server sendMsg :" + msg);
                }

            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
    }


    public class ListenerClient extends Thread {
        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;
        Socket client;
        UserInfo userInfo;
        public ListenerClient( Socket socket) {
            this.client = socket;
        }
        //为每一个客户端创建线程等待接收信息，然后把信息广播出去
        public void run() {
            while (true) {
                try {
                    dataInputStream = new DataInputStream(client.getInputStream());
                    String data = dataInputStream.readUTF();
                    if(data == null){
                    	return;
                    }
                    JSONObject object = JSONObject.parseObject(data);
                    if(object instanceof JSONObject){
                        JSONObject clientMessageInfo = (JSONObject) object;
                        println(clientMessageInfo == null ? "null" : clientMessageInfo.toString());
                        if(clientMessageInfo != null && clientMessageInfo.isEmpty()){
                        	println(userInfo.count + " 收到心跳包");
                            userInfo.count = 0;
                        }else{
                        	println("收到消息包");
                            if(clientMessageInfo!=null){
                                switch (clientMessageInfo.getIntValue("type")){
                                    case MessageType.LOGIN:
                                        login(clientMessageInfo);
                                        break;
                                    case MessageType.SINGLE_CHAT:
//                                        sendSingleChatMsg(clientMessageInfo);
                                        break;
                                    case MessageType.FRIEND_LIST:
                                        sendFriendList();
                                        break;
                                    case MessageType.REGISTER:
                                        register(clientMessageInfo);
                                        break;
                                }
                            }
                        }
                    }


                } catch (IOException e) {
                    println("read:" + e.toString());
                    break;
                } catch (Exception e){
                    println("read:" + e.toString());
                }
            }
        }

        private void register(JSONObject json) {
            String account = null;
            try {
                account = json.getString("account");
                String password = json.getString("password");
                JSONObject reply = new JSONObject();
                reply.put("type",MessageType.REGISTER);
                userInfo = containsUser(account);
                if(userInfo!=null){
                    println("user has exist");
                    reply.put("status",MessageType.STATUS_FAILED);
                    reply.put("status_msg","user has exist");
                }else{
                    println("register success");
                    reply.put("status",MessageType.STATUS_SUCCESS);
                    reply.put("status_msg","register success");
                    userInfo = new UserInfo(account,password,client);
                    //注册成功，添加到users
                    users.add(userInfo);
                }
                dataOutputStream = new DataOutputStream(client.getOutputStream());
                dataOutputStream.writeUTF(reply.toJSONString());
                dataOutputStream.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void login(JSONObject json) {
            try {
                String account = json.getString("account");
                String password = json.getString("password");
                JSONObject reply = new JSONObject();
                reply.put("type",MessageType.LOGIN);
                userInfo = containsUser(account);
                if(userInfo!=null){
                    println("login success");
                    reply.put("status",MessageType.STATUS_SUCCESS);
                    reply.put("status_msg","login success");
                    //更新ip地址
                    updateUserInfo(account,new UserInfo(account,password,client));
                    startCountAddTimerTask();
                }else{
                    println("user not register");
                    reply.put("status",MessageType.STATUS_FAILED);
                    reply.put("status_msg","user not register");
                }
                dataOutputStream = new DataOutputStream(client.getOutputStream());
                dataOutputStream.writeUTF(reply.toJSONString());
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        //把信息广播到所有用户
        public synchronized void sendMutilChatMsg(String clientMessageInfo) {
            try {
                for (int i = 0; i < users.size(); i++) {
                    Socket client = users.get(i).socket;
                    if(!client.isClosed()){
                    	 dataOutputStream = new DataOutputStream(client.getOutputStream());
                         dataOutputStream.writeUTF(clientMessageInfo);
                         dataOutputStream.flush();
                    }else{
                        println(">>closed!");
                    }
                }

            } catch (Exception e) {
                println(e.toString());
            }
        }

        //
//        public synchronized void sendSingleChatMsg(JSONObject bundle) {
//            try {
//                for (int i = 0; i < users.size(); i++) {
//                    Socket client = users.get(i).socket;
//                    System.out.println("ips :" + client.getInetAddress().toString());
//                    if(client.getInetAddress().equals(bundle.getToAddress())){
//                        if(!client.isClosed()){
//                            objectOutputStream = new ObjectOutputStream(client.getOutputStream());
//                            objectOutputStream.writeObject(clientMessageInfo);
//                            objectOutputStream.flush();
//                        }else{
//                            println(">>closed!");
//                        }
//                    }
//                }
//
//            } catch (Exception e) {
//                println("write:" + e.toString());
//            }
//        }

        public synchronized void sendFriendList() {
            if(!client.isClosed()){
                try {
                    JSONObject reply = new JSONObject();
                    reply.put("type",MessageType.FRIEND_LIST);
                    reply.put("friends",JSON.toJSON(userInfo.friends));
                    dataOutputStream = new DataOutputStream(client.getOutputStream());
                    dataOutputStream.writeUTF(reply.toJSONString());
                    dataOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                println(">>closed!");
            }
        }

        //心跳策略:http://www.cnblogs.com/scy251147/p/3333957.html
        private void startCountAddTimerTask() {
            // 启动心跳线程
            Timer countAddTimer = new Timer();
            TimerTask countAddTask = new TimerTask() {

                @Override
                public void run() {
                	println("count:" + userInfo.count);
                    userInfo.count ++;
                    if(userInfo.count > 3){
                        println("检测到用户：" + client.getInetAddress() + "掉线");
                    }
                }
            };
            countAddTimer.schedule(countAddTask, 10000, 10000);
        }



        public void println(String s) {
            if (s != null) {
                System.out.println(s + "\n");
            }
        }
    }
}
