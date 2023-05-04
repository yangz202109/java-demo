package enum_demo;

/**
 * @author yangz
 * @date 2022/12/30 - 16:12
 */
public class TestEnum {
    public static void main(String[] args) {
        /*for (Week value : Week.values()) {
            System.out.println(value);
        }*/

        Week friday = Week.friday;
        Week sunday = Week.sunday;

        // ordinal()方法：返回枚举常量的序数，注意从0开始
        System.out.println("sunday序号:"+sunday.ordinal());

        // compareTo()方法：枚举常量间的比较
        System.out.println(friday.compareTo(sunday));

        // name()方法：获得枚举常量的名称
        System.out.println(sunday.name());

        // valueOf()方法：返回指定名称的枚举常量
        System.out.println(Week.valueOf("monday"));

        switch (friday){
            case monday:
                System.out.println("星期一");
                break;
            case tuesday:
                System.out.println("星期二");
                break;
            case wednesday:
                System.out.println("星期三");
                break;
            case thursday:
                System.out.println("星期四");
                break;
            case friday:
                System.out.println("星期五");
                break;
            case saturday:
                System.out.println("星期六");
                break;
            case sunday:
                System.out.println("星期日");
                break;
        }

        System.out.println(Role.ROLE_ADMIN);

    }
}
