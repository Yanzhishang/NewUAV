package util;

import java.math.BigDecimal;

/**
 * Created by ZS on 17-3-14.
 */

public class StringUtil {
    /**
     * float 转成一位小数
     *
     * @param value
     * @return
     */
    public static String floatFormat(float value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(1, BigDecimal.ROUND_HALF_DOWN);
        return bd.toString();
    }
}