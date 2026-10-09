package lk.ridelink.common;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> status(ResponseStatusException ex){HttpStatusCode code=ex.getStatusCode();return ResponseEntity.status(code).body(Map.of("detail",ex.getReason()==null?"Request failed":ex.getReason()));}
 @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class) public ResponseEntity<?> duplicate(Exception ex){return ResponseEntity.status(409).body(Map.of("detail","A record with this unique value already exists"));}
}
