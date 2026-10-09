package lk.ridelink.common;
import org.springframework.http.*;import org.springframework.web.client.*;import org.springframework.web.server.ResponseStatusException;import java.util.*;
public final class Remote {
 private static final RestTemplate HTTP=new RestTemplate();private Remote(){}
 public static Map<String,Object> post(String url,Map<String,Object> body,String bearer){HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_JSON);if(bearer!=null)h.setBearerAuth(bearer);try{var response=HTTP.postForEntity(url,new HttpEntity<>(body,h),Map.class);return normalize((Map<String,Object>)response.getBody());}catch(HttpStatusCodeException ex){throw new ResponseStatusException(ex.getStatusCode(),"Upstream service returned "+ex.getStatusCode().value());}catch(ResourceAccessException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Upstream service unavailable");}}
 public static Map<String,Object> normalize(Map<String,Object> value){if(value==null)return Map.of();var copy=new HashMap<String,Object>();value.forEach((k,v)->copy.put(k.toLowerCase(Locale.ROOT),v instanceof Map<?,?> m?normalize((Map<String,Object>)m):v instanceof List<?> l?l.stream().map(x->x instanceof Map<?,?> m?normalize((Map<String,Object>)m):x).toList():v));return copy;}
}
