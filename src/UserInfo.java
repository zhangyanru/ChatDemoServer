import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class UserInfo implements Serializable{
    public Socket socket;
    public String account;
    public String password;
    public List<UserInfo> friends = new ArrayList<UserInfo>();
    public int count;

    public UserInfo(String account,String password,Socket socket){
        this.socket = socket;
        this.account = account;
        this.password = password;
        this.count = 0;
        friends.add(this);
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
