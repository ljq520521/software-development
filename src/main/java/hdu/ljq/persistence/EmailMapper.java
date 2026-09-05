package hdu.ljq.persistence;

import java.util.*;
import org.apache.ibatis.annotations.*;

@Mapper
public interface EmailMapper {
  @Insert(
      """
      INSERT INTO email_outbox(
        created_at,updated_at,recipient_email,subject,body_text,template_name,
        related_type,related_id,status,attempts,next_attempt_at
      ) VALUES(
        UTC_TIMESTAMP(3),UTC_TIMESTAMP(3),#{mail.recipient_email},#{mail.subject},
        #{mail.body_text},#{mail.template_name},#{mail.related_type},#{mail.related_id},
        'pending',0,UTC_TIMESTAMP(3)
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "mail.id")
  void insert(@Param("mail") Map<String, Object> mail);

  @Select(
      """
      SELECT * FROM email_outbox
      WHERE status IN ('pending','failed') AND attempts<5 AND next_attempt_at<=UTC_TIMESTAMP(3)
      ORDER BY id ASC LIMIT 10
      """)
  List<Map<String, Object>> deliverable();

  @Update(
      """
      UPDATE email_outbox SET status='sent',attempts=attempts+1,sent_at=UTC_TIMESTAMP(3),
        last_error=NULL,updated_at=UTC_TIMESTAMP(3) WHERE id=#{id} AND status IN ('pending','failed')
      """)
  int markSent(long id);

  @Update(
      """
      UPDATE email_outbox SET status='failed',attempts=attempts+1,
        next_attempt_at=DATE_ADD(UTC_TIMESTAMP(3),INTERVAL 5 MINUTE),last_error=#{error},
        updated_at=UTC_TIMESTAMP(3) WHERE id=#{id} AND status IN ('pending','failed')
      """)
  int markFailed(@Param("id") long id, @Param("error") String error);

  @Select(
      """
      <script>
      SELECT * FROM email_outbox
      WHERE (#{status}='' OR status=#{status})
        AND (#{q}='' OR recipient_email LIKE #{q} ESCAPE '!' OR subject LIKE #{q} ESCAPE '!')
      ORDER BY created_at DESC,id DESC LIMIT #{limit} OFFSET #{offset}
      </script>
      """)
  List<Map<String, Object>> list(
      @Param("q") String q,
      @Param("status") String status,
      @Param("limit") int limit,
      @Param("offset") long offset);

  @Select(
      """
      <script>
      SELECT COUNT(*) FROM email_outbox
      WHERE (#{status}='' OR status=#{status})
        AND (#{q}='' OR recipient_email LIKE #{q} ESCAPE '!' OR subject LIKE #{q} ESCAPE '!')
      </script>
      """)
  long count(@Param("q") String q, @Param("status") String status);
}
