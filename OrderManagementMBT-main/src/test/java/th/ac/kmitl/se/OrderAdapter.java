package th.ac.kmitl.se;

import org.graphwalker.core.machine.ExecutionContext;
import org.graphwalker.java.annotation.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;

// Update the filename of the saved file of your model here.
@Model(file  = "model2.json")
public class OrderAdapter extends ExecutionContext {
    // The following method add some delay between each step
    // so that we can see the progress in GraphWalker player.
    public static int delay = 0;
    @AfterElement
    public void afterEachStep() {
        try
        {
            Thread.sleep(delay);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }

    OrderDB orderDB;
    ProductDB productDB;
    PaymentService paymentService;
    ShippingService shippingService;
    Order order;
    @BeforeExecution
    public void setUp() {
        // Add the code here to be executed before
        // GraphWalk starts traversing the model.
        orderDB = mock(OrderDB.class);
        productDB = mock(ProductDB.class);
        shippingService = mock(ShippingService.class);
        paymentService = mock(PaymentService.class);
        order = Mockito.spy(new Order(orderDB, productDB, paymentService, shippingService));

    }

    @Edge()
    public void reset() {
        System.out.println("Edge reset");
        setUp();
    }

    @Edge()
    public void place() {
        System.out.println("Edge place");

        when(orderDB.getOrderID()).thenReturn(1);

        Address address = new Address("name", "line1", "line2", "district", "city", "postcode");
        order.place("John", "Apple Watch", 2,
                address);
        assertEquals(order.getStatus(), Order.Status.PLACED);
    }

    @Edge()
    public void cancel() {
        System.out.println("Edge cancel");

        assertEquals(order.getStatus(), Order.Status.PLACED);
        order.cancel();
        assertEquals(order.getStatus(), Order.Status.CANCELED);
    }

    @Edge()
    public void pay() {
        System.out.println("Edge pay");

        Card card = new Card("cardID", "nameOnCard",  10,  2030);
        order.pay(card);
        assertEquals(order.getStatus(), Order.Status.PAYMENT_CHECK);
    }

    @Edge()
    public void retryPay() {
        System.out.println("Edge retryPay");
        pay();
    }

    @Edge()
    public void paySuccess() {
        System.out.println("Edge paySuccess");
//        when(shippingService.getPrice(any(Address.class), anyFloat())).thenReturn(50f);

        Card card = new Card("cardID", "nameOnCard",  10,  2030);
        order.pay(card);

        ArgumentCaptor<PaymentCallback> paymentCallbackCaptor = ArgumentCaptor.forClass(PaymentCallback.class);
        verify(paymentService).pay(any(Card.class), anyFloat(), paymentCallbackCaptor.capture());

        paymentCallbackCaptor.getValue().onSuccess("SUCCESS CODE");

        assertEquals(order.getStatus(), Order.Status.PAID);
        assertEquals(order.paymentConfirmCode, "SUCCESS CODE");
    }

    @Edge()
    public void payError() {
        System.out.println("Edge payError");
//        when(shippingService.getPrice(any(Address.class), anyFloat())).thenReturn(50f);
        Card card = new Card("cardID", "nameOnCard",  10,  2030);
        order.pay(card);
        ArgumentCaptor<PaymentCallback> paymentCallbackCaptor = ArgumentCaptor.forClass(PaymentCallback.class);
        verify(paymentService).pay(any(Card.class), anyFloat(), paymentCallbackCaptor.capture());
        paymentCallbackCaptor.getValue().onError("ERROR CODE");

        assertEquals(order.getStatus(), Order.Status.PAYMENT_ERROR);
    }

    @Edge()
    public void ship() {
        System.out.println("Edge ship");

        when(shippingService.ship(any(Address.class), anyFloat())).thenReturn("TRACKING CODE");
        order.ship();
        assertEquals(order.getStatus(), Order.Status.SHIPPED);
        assertEquals(order.trackingCode, "TRACKING CODE");
    }

    //additional edge(used to be "cancel")
    @Edge()
    public void refund() {
        System.out.println("Edge cancel");

        assertEquals(order.getStatus(), Order.Status.PAID);
        order.cancel();
        assertEquals(order.getStatus(), Order.Status.AWAIT_REFUND);
    }

    @Edge()
    public void refundSuccess() {
        System.out.println("Edge refundSuccess");

        order.refund();

        ArgumentCaptor<PaymentCallback> paymentCallbackCaptor = ArgumentCaptor.forClass(PaymentCallback.class);
        verify(paymentService).refund(anyString(), paymentCallbackCaptor.capture());
        paymentCallbackCaptor.getValue().onSuccess("REFUND SUCCESS");

        assertEquals(order.getStatus(), Order.Status.REFUNDED);
    }

    @Edge()
    public void refundError() {
        System.out.println("Edge refundError");

        order.refund();

        ArgumentCaptor<PaymentCallback> paymentCallbackCaptor = ArgumentCaptor.forClass(PaymentCallback.class);
        verify(paymentService).refund(anyString(), paymentCallbackCaptor.capture());
        paymentCallbackCaptor.getValue().onError("REFUND ERROR");

        assertEquals(order.getStatus(), Order.Status.REFUND_ERROR);
    }
}
