package jpabook.jpashop2.api;

import jpabook.jpashop2.domain.Address;
import jpabook.jpashop2.domain.Order;
import jpabook.jpashop2.domain.OrderStatus;
import jpabook.jpashop2.repository.OrderRepository;
import jpabook.jpashop2.repository.OrderSearch;
import jpabook.jpashop2.repository.OrderSimpleQueryDto;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ~~ToOne(ManyToOne, OneToOne)
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {
    private final OrderRepository orderRepository;

    /**
     * 객체를 json으로 만드는 jackson 입장에서는
     * 객체가 양방향이기 때문에 계속 번갈아가며 참조하다가 에러가 발생한다.
     * JsonIgnore로 양방향 참조를 끊어도 Lazy로 연결된 entity는
     * Proxy 객체로 가져오기 때문에 jackson에서 다시 오류가 발생한다.
     * 좋지 않은 방법이지만 Jackson DataType Hibernate5를 사용하여 해결.
     * 성능 상에도 문제가 있고 entity를 직접 반환하기 때문에 좋지 않음.
     * -------------DTO를 사용하자-------------
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAll(new OrderSearch());
        return all;
    }

    /**
     * DTO를 사용하여 필요한 데이터만 외부에 노출하기 때문에 장점이다.
     * 하지만 LAZY 로딩으로 인해 쿼리가 여러번 발생된다.
     * */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAll(new OrderSearch());
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());

        return result;
    }

    /**
     * Fetch Join을 사용하여 쿼리 하나로 데이터를 가져온다.
     * */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> orderV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());

        return result;
    }

    /**
     * JPA 내에서 바로 DTO를 생성하여 리턴한다.
     * DTO 자체로 조회되기 때문에 API 스펙에 맞춰져서 사용된다.
     * 오로지 Entity만 조회하는 Repository와는 별도로 사용하여
     * 유지보수를 용이하게 하는게 좋다.
     * */
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> orderV4() {
        return orderRepository.findOrderDtos();
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;

        private String name;

        private LocalDateTime orderDate;

        private OrderStatus orderStatus;

        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
        }
    }
}
