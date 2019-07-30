package chainofResponsibility.entity;

/**
 * describe:用户信息类
 *
 * @author lichao
 * @date 2019/03/07
 */
public class User {
    private final String name;
    private final String phone;

    public User(String name, String phone){
        this.name = name;
        this.phone = phone;
    }
    public String getName() {
        return name;
    }
    public String getPhone() {
        return phone;
    }
}
