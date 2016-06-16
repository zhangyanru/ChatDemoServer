import java.net.Socket;
import java.util.ArrayList;
public class UserInfo {
    public Socket socket;
    public String account;
    public String password;
    public ArrayList<UserInfo> friends = new ArrayList<>();
    public int count;

    public UserInfo(String account,String password,Socket socket){
        this.socket = socket;
        this.account = account;
        this.password = password;
        this.count = 0;
    }

    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("account:" + account + ",");
        stringBuffer.append("password" + password + ",");
        stringBuffer.append("ip:" + socket.getInetAddress().toString());
        stringBuffer.append("friends:" + friends.toString());
        return stringBuffer.toString();
    }
}
