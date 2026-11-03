package org.example.micro.events;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductLockedEvent {
    private String orderId;
}