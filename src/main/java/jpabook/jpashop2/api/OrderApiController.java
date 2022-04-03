package jpabook.jpashop2.api;

import jpabook.jpashop2.domain.Address;
import jpabook.jpashop2.domain.Order;
import jpabook.jpashop2.domain.OrderItem;
import jpabook.jpashop2.domain.OrderStatus;
import jpabook.jpashop2.repository.OrderRepository;
import jpabook.jpashop2.repository.OrderSearch;
import jpabook.jpashop2.repository.order.query.OrderFlatDto;
import jpabook.jpashop2.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop2.repository.order.query.OrderQueryRepository;
import jpabook.jpashop2.repository.order.query.OrderQueryDto;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.*;

@RestController
@RequiredArgsConstructor
public class OrderApiController {
    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    @GetMapping("/api/v1/orders")
    public List<Order> orderV1() {
        List<Order> all = orderRepository.findAll(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> {
                o.getItem().getName();
            });

        }
        return all;
    }

    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAll(new OrderSearch());
        List<OrderDto> collect = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());

        return collect;
    }

    /**
     * order 입장에서는 ~toMany와 fetch join을 하게되면
     * Many의 row의 수 만큼 중복 데이터가 추가되어 join된다.
     * distinct를 추가하여 일단 데이터베이스에서 distinct된 데이터를 가져온 뒤에
     * jpa가 id만을 기준으로 한번 더 distinct 해준다.
     * 하지만 페이징을 추가하게되면 jpa는 일단 모든 데이터를 불러온 뒤에 메모리 상에서 페이징하기 때문에
     * outofmemory 에러가 발생할 수 있다.
     * */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        List<OrderDto> collect = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());

        return collect;
    }

    /**
     * default_batch_fetch_size 값을 설정하여 ~ToOne의 관계는 fetch join으로 가져오고
     * ~toMany 데이터들은 Lazy로 가져오되 엔티티의 연관 관계 개수만큼 각각 쿼리를 날리는 것이 아니라
     * 각 엔티티 마다 쿼리 한개씩만 날려서 데이터를 가져옴. 쿼리 개수가 1 + N + M에서 1 + 1 + 1로 줄어듦.
     * v3에서 fetch join으로 쿼리 한번에 데이터를 가져왔지만 중복 데이터가 많아서 네트워크 비용이 더 발생할 수 있음.
     * */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                        @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);
        List<OrderDto> collect = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());

        return collect;
    }

    /**
     * v3.1의 데이터를 DTO로 조회. ~toMany의 관계 때문에 N + 1 문제가 발생한다.
     * */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    /**
     * in 절을 사용하여 ~toMany 관계의 데이터를 한번에 가져온 뒤 order id를 기준으로 map을 생성하여
     * O(1) 의 시간복잡도로 order와 orderItem 데이터를 합쳐서 전달한다.
     * */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    /**
     * 한번의 쿼리로 ~toMany 데이터를 가져와서 중복을 직접 제거하여 전달하는 방식.
     * 추가 작업이 발생하고 페이징이 불가능하다.
     * */
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();

        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(),
                                o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(),
                                o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(),
                        e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(),
                        e.getKey().getAddress(), e.getValue()))
                .collect(toList());
    }

    @Getter
    static class OrderDto {
        public Long orderId;

        private String name;

        private LocalDateTime orderDate;

        private OrderStatus orderStatus;

        private Address address;

        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(OrderItemDto::new)
                    .collect(toList());
        }
    }

    @Data
    static class OrderItemDto {
        private String itemName;//상품 명

        private int orderPrice; //주문 가격

        private int count; //주문 수량

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}
