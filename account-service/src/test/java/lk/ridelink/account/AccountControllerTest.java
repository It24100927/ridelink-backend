package lk.ridelink.account;
import org.junit.jupiter.api.Test;import org.bson.Document;import org.springframework.data.mongodb.core.MongoTemplate;import org.springframework.web.server.ResponseStatusException;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;import static org.mockito.ArgumentMatchers.*;
class AccountControllerTest {
 @Test void registersPassengerWithoutReturningPassword(){var db=mock(MongoTemplate.class);var controller=new AccountController(db,"test-secret","","");var result=controller.register(new AccountController.Registration("Passenger","p@example.test","password123","passenger"));assertNotNull(result.get("id"));assertEquals("passenger",result.get("role"));assertFalse(result.containsKey("password_hash"));verify(db).insert(any(Document.class),eq("accounts"));}
 @Test void rejectsUnsupportedRole(){var controller=new AccountController(mock(MongoTemplate.class),"test-secret","","");var ex=assertThrows(ResponseStatusException.class,()->controller.register(new AccountController.Registration("User","u@example.test","password123","admin")));assertEquals(400,ex.getStatusCode().value());}
}
