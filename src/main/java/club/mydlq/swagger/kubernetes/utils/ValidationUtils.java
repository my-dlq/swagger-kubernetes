package club.mydlq.swagger.kubernetes.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import java.util.regex.Pattern;

/**
 * 内容验证工具
 *
 * @author mydlq
 */
public class ValidationUtils {

    private ValidationUtils() {
    }

    static final String REGEX_URL = "^(?:https?://)?[\\w]+(?:\\.?[\\w]+)+[\\w-_/?&=#%:]*$";
    static final String REGEX_URL_PORT = "^\\S*:[0-9]+$";

    public static boolean validateUrl(String str) {
        return Pattern.matches(REGEX_URL, str);
    }

    /**
     * 验证 url 字符串中是否存在端口
     *
     * @param str str 待验证字符串
     * @return 如果返回 true 则表示是一个带端口的 url，否则返回 false 则不是 url 或者不带端口。
     */
    public static boolean validatePort(String str) {
        return validateUrl(str) && Pattern.matches(REGEX_URL_PORT, str);
    }

    /**
     * 验证 String 是否为 Swagger Api
     *
     * @param jsonStr 待验证的 Json 字符串
     * @return 如果返回 true 则表示是一个 json 串，否则返回 false 则表示不是 json 串。
     */
    public static boolean isSwagger(String jsonStr) {
        try {
            JsonElement jsonElement = JsonParser.parseString(jsonStr);
            String swaggerStr = jsonElement.getAsJsonObject().get("swagger").toString();
            return StringUtils.isNotEmpty(swaggerStr);
        } catch (Exception e) {
            return false;
        }
    }

}
