package hdu.ljq.persistence;

import java.util.*;
import org.apache.ibatis.annotations.*;

@Mapper
public interface RecordMapper {
  List<Map<String, Object>> select(
      @Param("table") String table,
      @Param("where") String where,
      @Param("p") Map<String, Object> p,
      @Param("order") String order,
      @Param("limit") int limit,
      @Param("offset") long offset);

  long count(
      @Param("table") String table,
      @Param("where") String where,
      @Param("p") Map<String, Object> p);

  Map<String, Object> find(@Param("table") String table, @Param("id") long id);

  void insert(@Param("table") String table, @Param("row") Map<String, Object> row);

  int update(
      @Param("table") String table,
      @Param("row") Map<String, Object> row,
      @Param("id") long id,
      @Param("version") int version);

  @Select("SELECT lock_name FROM app_lock WHERE lock_name=#{key} FOR UPDATE")
  String lock(String key);

  @Select("SELECT 1")
  int ping();

  @Select(
      "SELECT request_hash,response_data FROM idempotency_record WHERE endpoint=#{endpoint} AND"
          + " key_value=#{key} AND expires_at>UTC_TIMESTAMP(3)")
  Map<String, Object> receipt(@Param("endpoint") String endpoint, @Param("key") String key);

  @Delete(
      "DELETE FROM idempotency_record WHERE endpoint=#{endpoint} AND key_value=#{key} AND"
          + " expires_at<=UTC_TIMESTAMP(3)")
  void expireReceipt(@Param("endpoint") String endpoint, @Param("key") String key);

  @Insert(
      "INSERT INTO idempotency_record(endpoint,key_value,request_hash,response_data,expires_at)"
          + " VALUES(#{endpoint},#{key},#{hash},#{data},DATE_ADD(UTC_TIMESTAMP(3),INTERVAL 24"
          + " HOUR))")
  void saveReceipt(
      @Param("endpoint") String endpoint,
      @Param("key") String key,
      @Param("hash") String hash,
      @Param("data") String data);
}
