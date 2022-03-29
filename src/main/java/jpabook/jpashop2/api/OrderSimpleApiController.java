package jpabook.jpashop2.api;

import jpabook.jpashop2.domain.Order;
import jpabook.jpashop2.repository.OrderRepository;
import jpabook.jpashop2.repository.OrderSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
