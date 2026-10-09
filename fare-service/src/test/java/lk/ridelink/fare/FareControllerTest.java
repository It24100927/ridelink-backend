package lk.ridelink.fare;
import lk.ridelink.common.Auth;import org.junit.jupiter.api.Test;import org.springframework.mock.web.MockHttpServletRequest;import org.springframework.web.server.ResponseStatusException;import java.math.BigDecimal;import static org.junit.jupiter.api.Assertions.*;
class FareControllerTest {
 @Test void estimateUsesDocumentedBaseAndDistanceRates(){var controller=new FareController(null,"test-secret","http://localhost:8003");var req=new MockHttpServletRequest();req.addHeader("Authorization","Bearer "+Auth.issue("p1","passenger","test-secret"));assertEquals(new BigDecimal("23.00"),controller.estimate(req,new FareController.Estimate(10.0)).get("estimated_fare"));}
 @Test void rejectsZeroDistance(){var controller=new FareController(null,"test-secret","http://localhost:8003");var req=new MockHttpServletRequest();req.addHeader("Authorization","Bearer "+Auth.issue("p1","passenger","test-secret"));assertEquals(400,assertThrows(ResponseStatusException.class,()->controller.estimate(req,new FareController.Estimate(0.0))).getStatusCode().value());}
}
