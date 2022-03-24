package jpabook.jpashop2.service;

import jpabook.jpashop2.domain.Address;
import jpabook.jpashop2.domain.Member;
import jpabook.jpashop2.domain.Order;
import jpabook.jpashop2.domain.OrderStatus;
import jpabook.jpashop2.domain.item.Book;
import jpabook.jpashop2.domain.item.Item;
import jpabook.jpashop2.exception.NotEnoughStockException;
import jpabook.jpashop2.repository.OrderRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@SpringBootTest
@Transactional
class OrderServiceTest {
    @Autowired
    EntityManager em;

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @Test
    public void order() throws Exception {
        Member member = createMember();

        Item book = createBook(10000, 10, "시골 JPA");

        Long orderId = orderService.order(member.getId(), book.getId(), 2);

        Order getOrder = orderRepository.findOne(orderId);

        Assertions.assertEquals(OrderStatus.ORDER, getOrder.getStatus());
        Assertions.assertEquals(1, getOrder.getOrderItems().size());
        Assertions.assertEquals(10000 * 2, getOrder.getTotalPrice());
        Assertions.assertEquals(8, book.getStockQuantity());

    }



    @Test
    public void quantityError() throws Exception {
        Member member = createMember();
        Item book = createBook(10000, 10, "시골 JPA");

        int orderCount = 11;

        Assertions.assertThrows(NotEnoughStockException.class, () -> {
            orderService.order(member.getId(), book.getId(), orderCount);
        });

    }

    @Test
    public void cancel() throws Exception {
        Member member = createMember();
        Item book = createBook(10000, 10, "시골 JPA");
        int orderCount = 2;

        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

        orderService.cancelOrder(orderId);

        Order getOrder = orderRepository.findOne(orderId);

        Assertions.assertEquals(OrderStatus.CANCEL, getOrder.getStatus());
        Assertions.assertEquals(10, book.getStockQuantity());
    }

    @Test
    public void countError() throws Exception {

    }

    private Item createBook(int price, int stockQuantity, String bookName) {
        Item book = new Book();
        book.setName(bookName);
        book.setPrice(price);
        book.setStockQuantity(stockQuantity);
        em.persist(book);
        return book;
    }

    private Member createMember() {
        Member member = new Member();
        member.setName("member1");
        member.setAddress(new Address("서울", "경기", "123-123"));
        em.persist(member);
        return member;
    }
}