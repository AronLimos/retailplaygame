package com.cpro.retailplaygame.service.impl;

import com.cpro.retailplaygame.entity.Cart;
import com.cpro.retailplaygame.entity.CartItem;
import com.cpro.retailplaygame.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StripeServiceImpl implements StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${server.port}")
    private int serverPort; // Inject server port

    @Override
    public String createCheckoutSession(Cart cart) throws StripeException {
        // Set the Stripe API Key
        com.stripe.Stripe.apiKey = stripeApiKey;

        // Prepare session items
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();

        // Loop through cart items to prepare them for the checkout session
        for (CartItem item : cart.getCartItems()) {
            // Calculate the price for the item (before discount)
            double itemPrice = item.getProduct().getPrice();

            // Apply the coupon discount if available
            double discountAmount = cart.getCoupon() != null ? cart.getCoupon().getDiscount() : 0.0;
            if (discountAmount > 0) {
                itemPrice = itemPrice - (itemPrice * (discountAmount / 100)); // Apply discount
            }

            lineItems.add(
                    SessionCreateParams.LineItem.builder()
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("cad")
                                            .setUnitAmount((long) (itemPrice * 100)) // Stripe uses cents
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName(item.getProduct().getProductName())
                                                            .build()
                                            )
                                            .build()
                            )
                            .setQuantity((long) item.getQuantity())
                            .build()
            );
        }

        String successUrl = "http://localhost:" + serverPort + "/cart/success";
        String cancelUrl = "http://localhost:" + serverPort + "/products";

        // Create a checkout session with the specified details
        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)  // Add the payment method type
                .setMode(SessionCreateParams.Mode.PAYMENT)  // Set the mode (payment mode)
                .setSuccessUrl(successUrl)  // Success URL
                .setCancelUrl(cancelUrl)    // Cancel URL
                .addAllLineItem(lineItems) // Add all line items at once
                .build();

        // Create the session
        Session session = Session.create(params);

        // Log the session URL to debug
//        System.out.println("Stripe Session URL: " + session.getUrl());

        // Return the checkout session URL
        return session.getUrl();
    }
}
