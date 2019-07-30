package number2char;

import java.math.BigDecimal;

/**
 * describe:把输入的数字转换为大写
 *
 * @author lichao
 * @date 2019/03/07
 */
public class NumberToChar {
    /**
     * 数字大写
     */
    private static final String[] CN_UPPER_NUMBER = { "零", "一", "二", "三", "四",
            "五", "六", "七", "八", "九" };
    /**
     * 单位大写，类似于占位符
     */
    private static final String[] CN_UPPER_MONETRAY_UNIT = { "", "", "",
            "十", "百", "千", "万", "十", "百", "千", "亿", "十", "百", "千", "兆", "十",
            "百", "千" };
    /**
     * 特殊字符：整
     */
    private static final String CN_FULL = "";
    /**
     * 特殊字符：负
     */
    private static final String CN_NEGATIVE = "负";
    /**
     * 精度，默认值为2
     */
    private static final int MONEY_PRECISION = 2;
    /**
     * 特殊字符：零
     */
    private static final String CN_ZEOR_FULL = "零" + CN_FULL;

    /**
     * 把输入的数字转换为大写
     *
     * @param numberOfMoney
     *
     * @return 对应的大写
     */
    public static String number2CNMontrayUnit(BigDecimal numberOfMoney) {
        StringBuffer sb = new StringBuffer();
        // -1, 0, or 1 as the value of this BigDecimal is negative, zero, or
        // positive.
        int signum = numberOfMoney.signum();
        //
        if (signum == 0) {
            return CN_ZEOR_FULL;
        }
        //这里会进行的四舍五入
        long number = numberOfMoney.movePointRight(MONEY_PRECISION)
                .setScale(0, 4).abs().longValue();
        // 得到小数点后两位值
        long scale = number % 100;
        int numUnit = 0;
        int numIndex = 0;
        boolean getZero = false;
        // 判断最后两位数，一共有四中情况：00 = 0, 01 = 1, 10, 11
        if (!(scale > 0)) {
            numIndex = 2;
            number = number / 100;
            getZero = true;
        }
        if ((scale > 0) && (!(scale % 10 > 0))) {
            numIndex = 1;
            number = number / 10;
            getZero = true;
        }
        int zeroSize = 0;
        while (true) {
            if (number <= 0) {
                break;
            }
            // 每次获取到最后一个数
            numUnit = (int) (number % 10);
            if (numUnit > 0) {
                if ((numIndex == 9) && (zeroSize >= 3)) {
                    sb.insert(0, CN_UPPER_MONETRAY_UNIT[6]);
                }
                if ((numIndex == 13) && (zeroSize >= 3)) {
                    sb.insert(0, CN_UPPER_MONETRAY_UNIT[10]);
                }
                sb.insert(0, CN_UPPER_MONETRAY_UNIT[numIndex]);
                sb.insert(0, CN_UPPER_NUMBER[numUnit]);
                getZero = false;
                zeroSize = 0;
            } else {
                ++zeroSize;
                if (!(getZero)) {
                    sb.insert(0, CN_UPPER_NUMBER[numUnit]);
                }
                if (numIndex == 2) {
                    if (number > 0) {
                        sb.insert(0, CN_UPPER_MONETRAY_UNIT[numIndex]);
                    }
                } else if (((numIndex - 2) % 4 == 0) && (number % 1000 > 0)) {
                    sb.insert(0, CN_UPPER_MONETRAY_UNIT[numIndex]);
                }
                getZero = true;
            }
            // 让number每次都去掉最后一个数
            number = number / 10;
            ++numIndex;
        }
        // 如果signum == -1，则说明输入的数字为负数，就在最前面追加特殊字符：负
        if (signum == -1) {
            sb.insert(0, CN_NEGATIVE);
        }
        // 输入的数字小数点后两位为"00"的情况；
        if (!(scale > 0)) {
            sb.append(CN_FULL);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String input = "0";
        BigDecimal numberOfInput = new BigDecimal("1105");
        String s = NumberToChar.number2CNMontrayUnit(numberOfInput);
        System.out.println("输入为:["+ input +"]输出为:[" +s.toString()+"]");
        input = "1105";
        numberOfInput = new BigDecimal("1105");
        s = NumberToChar.number2CNMontrayUnit(numberOfInput);
        System.out.println("输入为:["+ input +"]输出为:[" +s.toString()+"]");
        input = "1100";
        numberOfInput = new BigDecimal("1105");
        s = NumberToChar.number2CNMontrayUnit(numberOfInput);
        System.out.println("输入为:["+ input +"]输出为:[" +s.toString()+"]");
        input = "100000000000";
        numberOfInput = new BigDecimal("1105");
        s = NumberToChar.number2CNMontrayUnit(numberOfInput);
        System.out.println("输入为:["+ input +"]输出为:[" +s.toString()+"]");
    }
}
