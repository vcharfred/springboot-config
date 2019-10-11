import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * <p>  TODO 功能描述 </p>
 *
 * @author vchar fred
 * @version 1.0
 * @create_date 2019/10/12 1:56
 */
public class MyTest {

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("1zmk");
        list.add("des");
        System.out.println(Arrays.toString(list.toArray()));
        list.sort(Comparator.naturalOrder());
        System.out.println(Arrays.toString(list.toArray()));
    }
}
