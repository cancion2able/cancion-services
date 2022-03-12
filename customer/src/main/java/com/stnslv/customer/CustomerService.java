package com.stnslv.customer;

import com.stnslv.amqp.RabbitMQMessageProducer;
import com.stnslv.clients.fraud.FraudCheckResponse;
import com.stnslv.clients.fraud.FraudClient;
import com.stnslv.clients.notification.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repository;
    private final FraudClient fraudClient;
    private final RabbitMQMessageProducer messageProducer;

    public void registerCustomer(CustomerRegistrationRequest customerRegistrationRequest) {
        Customer customer = Customer.builder()
                .firstName(customerRegistrationRequest.firstName())
                .lastName(customerRegistrationRequest.lastName())
                .email(customerRegistrationRequest.email())
                .build();
        repository.saveAndFlush(customer);
        final FraudCheckResponse response = fraudClient.isFraudster(customer.getId());
        if (response.isFraudster()) {
            throw new IllegalStateException("Fraudster found!");
        }
        final NotificationRequest notificationRequest = new NotificationRequest(
                customer.getId(),
                customer.getEmail(),
                String.format("Hi %s, welcome!", customer.getFirstName())
        );
        messageProducer.publish(notificationRequest,
                "internal.exchange",
                "internal.notification.routing-key");
    }
}
