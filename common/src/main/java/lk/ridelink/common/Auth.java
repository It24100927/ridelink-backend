package lk.ridelink.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class Auth {
    private Auth() {}
    private static String b64(byte[] value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
    private static byte[] signature(String value, String secret) {
        try { Mac mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256")); return mac.doFinal(value.getBytes(StandardCharsets.UTF_8)); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    public static String issue(String subject,String role,String secret) {
        String header=b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload=b64((subject+"|"+role+"|"+(System.currentTimeMillis()/1000+86400)).getBytes(StandardCharsets.UTF_8));
        String input=header+"."+payload; return input+"."+b64(signature(input,secret));
    }
    public static Claims claims(HttpServletRequest request,String secret) {
        String value=request.getHeader("Authorization");
        if(value==null||!value.startsWith("Bearer ")) return null;
        try {
            String[] p=value.substring(7).split("\\."); if(p.length!=3)return null;
            String input=p[0]+"."+p[1]; if(!java.security.MessageDigest.isEqual(Base64.getUrlDecoder().decode(p[2]),signature(input,secret)))return null;
            String[] parts=new String(Base64.getUrlDecoder().decode(p[1]),StandardCharsets.UTF_8).split("\\|",-1);
            if(parts.length!=3||Long.parseLong(parts[2])<=System.currentTimeMillis()/1000)return null;
            return new Claims(parts[0],parts[1]);
        } catch(Exception ex) { return null; }
    }
    public static Claims require(HttpServletRequest request,String secret,String... roles) {
        Claims claims=claims(request,secret);
        if(claims==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Valid bearer token required");
        if(roles.length>0&&java.util.Arrays.stream(roles).noneMatch(claims.role()::equals))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Insufficient role");
        return claims;
    }
    public record Claims(String subject,String role) {}
}
