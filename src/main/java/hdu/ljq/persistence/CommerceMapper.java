package hdu.ljq.persistence;

import java.util.*;
import org.apache.ibatis.annotations.*;

@Mapper
public interface CommerceMapper {
  Map<String, Object> purchasableProduct(@Param("id") long id);

  void insertOrder(@Param("row") Map<String, Object> row);

  void insertItem(@Param("row") Map<String, Object> row);

  void insertPayment(@Param("row") Map<String, Object> row);

  Map<String, Object> orderByNumber(@Param("number") String number);

  Map<String, Object> lockedOrderByNumber(@Param("number") String number);

  Map<String, Object> orderById(@Param("id") long id);

  List<Map<String, Object>> items(@Param("orderId") long orderId);

  List<Map<String, Object>> payments(@Param("orderId") long orderId);

  int markPaid(@Param("id") long id, @Param("paidAt") java.sql.Timestamp paidAt);

  int updateOrder(
      @Param("id") long id,
      @Param("version") int version,
      @Param("status") String status,
      @Param("paymentStatus") String paymentStatus,
      @Param("note") String note);

  int refundPayments(@Param("orderId") long orderId);

  long countOrders(@Param("q") String q, @Param("status") String status);

  List<Map<String, Object>> listOrders(
      @Param("q") String q,
      @Param("status") String status,
      @Param("limit") int limit,
      @Param("offset") long offset);

  long countPayments(@Param("q") String q, @Param("status") String status);

  List<Map<String, Object>> listPayments(
      @Param("q") String q,
      @Param("status") String status,
      @Param("limit") int limit,
      @Param("offset") long offset);

  @Select("SELECT COALESCE(SUM(total_cents),0) FROM customer_order WHERE payment_status='paid'")
  long paidRevenue();

  @Select("SELECT COUNT(*) FROM customer_order WHERE status='pending_payment'")
  long pendingOrders();
}
