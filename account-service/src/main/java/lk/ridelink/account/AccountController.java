package lk.ridelink.account;

import lk.ridelink.common.Auth;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/accounts")
public class AccountController {
 private final MongoTemplate db; private final String secret; private final String adminEmail; private final String adminPassword;
 public AccountController(MongoTemplate db,@Value("${security.jwt.secret}") String secret,
                          @Value("${security.admin.email:}") String adminEmail,
                          @Value("${security.admin.password:}") String adminPassword){this.db=db;this.secret=secret;this.adminEmail=adminEmail;this.adminPassword=adminPassword;}
 @EventListener(ApplicationReadyEvent.class) public void indexes(){
  db.indexOps("accounts").ensureIndex(new Index().on("email", Sort.Direction.ASC).unique());
  boolean hasEmail=adminEmail!=null&&!adminEmail.isBlank();boolean hasPassword=adminPassword!=null&&!adminPassword.isBlank();
  if(hasEmail!=hasPassword)throw new IllegalStateException("Set both ADMIN_EMAIL and ADMIN_PASSWORD to bootstrap the administrator account");
  if(!hasEmail)return;
  if(!adminEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")||adminPassword.length()<12)throw new IllegalStateException("ADMIN_EMAIL must be valid and ADMIN_PASSWORD must contain at least 12 characters");
  String normalizedEmail=adminEmail.trim().toLowerCase();
  var existing=db.findOne(Query.query(Criteria.where("email").is(normalizedEmail)),org.bson.Document.class,"accounts");
  if(existing==null){db.insert(new org.bson.Document("id",UUID.randomUUID().toString()).append("name","RideLink Administrator").append("email",normalizedEmail).append("password_hash",hash(adminPassword)).append("role","admin").append("status","active"),"accounts");}
  else if(!"admin".equals(existing.getString("role")))throw new IllegalStateException("ADMIN_EMAIL belongs to a non-admin account; choose a different bootstrap email");
  else db.updateFirst(Query.query(Criteria.where("email").is(normalizedEmail)),Update.update("password_hash",hash(adminPassword)).set("status","active"),"accounts");
 }
 public record Registration(String name,String email,String password,String role){}
 public record Login(String email,String password){}
 public record Profile(String name){}
 public record StatusChange(String id,String status){}
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
 public Map<String,Object> register(@RequestBody Registration b){
  if(b.name()==null||b.name().isBlank()||b.name().length()>100||b.email()==null||!b.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")||b.password()==null||b.password().length()<8||!("passenger".equals(b.role())||"driver".equals(b.role())))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Valid name, email, password (8+ characters), and role required");
  String id=UUID.randomUUID().toString();db.insert(new org.bson.Document("id",id).append("name",b.name().trim()).append("email",b.email().toLowerCase()).append("password_hash",hash(b.password())).append("role",b.role()).append("status","active"),"accounts");
  return Map.of("id",id,"name",b.name().trim(),"email",b.email().toLowerCase(),"role",b.role());
 }
 @PostMapping("/login") public Map<String,Object> login(@RequestBody Login b){
  if(b==null||b.email()==null||b.password()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"email and password required");
  var row=db.findOne(Query.query(Criteria.where("email").is(b.email().toLowerCase())),org.bson.Document.class,"accounts");if(row==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials");String[] split=row.getString("password_hash").split(":",2);
  if(!MessageDigest.isEqual(split[1].getBytes(StandardCharsets.UTF_8),hashWithSalt(b.password(),split[0]).getBytes(StandardCharsets.UTF_8))||!"active".equals(row.getString("status")))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials or inactive account");
  var user=Map.of("id",row.getString("id"),"name",row.getString("name"),"role",row.getString("role"));return Map.of("access_token",Auth.issue(row.getString("id"),row.getString("role"),secret),"token_type","bearer","user",user);
 }
 @GetMapping("/me") @SecurityRequirement(name="bearerAuth") public Map<String,Object> me(HttpServletRequest req){var c=Auth.require(req,secret);var row=db.findOne(Query.query(Criteria.where("id").is(c.subject())),org.bson.Document.class,"accounts");if(row==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Account not found");return Map.of("id",row.getString("id"),"name",row.getString("name"),"email",row.getString("email"),"role",row.getString("role"),"status",row.getString("status"));}
 @PatchMapping("/me") @SecurityRequirement(name="bearerAuth") public Map<String,String> update(HttpServletRequest req,@RequestBody Profile b){var c=Auth.require(req,secret);if(b.name()==null||b.name().isBlank()||b.name().length()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"name must be 1 to 100 characters");if(db.updateFirst(Query.query(Criteria.where("id").is(c.subject())),Update.update("name",b.name().trim()),"accounts").getMatchedCount()==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Account not found");return Map.of("name",b.name().trim());}
 @PatchMapping("/status") @SecurityRequirement(name="bearerAuth") public Map<String,String> status(HttpServletRequest req,@RequestBody StatusChange b){Auth.require(req,secret,"admin");if(!"active".equals(b.status())&&!"suspended".equals(b.status()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"status must be active or suspended");var result=db.updateFirst(Query.query(Criteria.where("id").is(b.id())),Update.update("status",b.status()),"accounts");if(result.getMatchedCount()==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Account not found");return Map.of("status",b.status());}
 private static String hash(String password){byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);String s=Base64.getEncoder().encodeToString(salt);return s+":"+hashWithSalt(password,s);}
 private static String hashWithSalt(String password,String salt){try{byte[] bytes=Base64.getDecoder().decode(salt);var spec=new javax.crypto.spec.PBEKeySpec(password.toCharArray(),bytes,200000,256);return Base64.getEncoder().encodeToString(javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded());}catch(Exception ex){throw new IllegalStateException(ex);}}
}
