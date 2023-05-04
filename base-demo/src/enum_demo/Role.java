package enum_demo;

/**
 * @author yangz
 * @date 2022/12/30 - 16:30
 */
public enum Role {
    ROLE_ADMIN("管理员",1111),
    ROLE_SALE("销售",2222),
    role_test("测试",3333);

    // 以下为自定义属性

    private final String roleName;  //角色名称

    private final Integer roleCode; //角色编码

    // 以下为自定义构造函数

    Role( String roleName, Integer roleCode ) {
        this.roleName = roleName;
        this.roleCode = roleCode;
    }
}
