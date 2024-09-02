import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;

import java.util.HashMap;

/**
 * @author whut2024
 * @since 2024-09-02
 */
public class JwtTest {


    public static void main(String[] args) {
        HashMap<String, Object> payloadMap = new HashMap<>();

        payloadMap.put("expire_time", System.currentTimeMillis() + 1000L * 60 * 30);
        payloadMap.put("version", "0");


        String token = JWTUtil.createToken(payloadMap, "123".getBytes());
        System.out.println(token);

        JWT jwt = JWTUtil.parseToken(token);
        System.out.println(jwt.getPayload().getClaimsJson());
    }
}
