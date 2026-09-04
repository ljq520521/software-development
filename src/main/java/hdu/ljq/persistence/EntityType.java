package hdu.ljq.persistence;

import java.util.*;

/** SQL identifiers are compile-time allowlists, never request parameters. */
public enum EntityType {
  PRODUCT(
      "product",
      "name:STRING",
      "slug:STRING",
      "sku:STRING",
      "category_id:ID",
      "short_description:STRING",
      "description_markdown:STRING",
      "age_min:NUMBER",
      "age_max:NUMBER",
      "environments:JSON",
      "features:JSON",
      "specifications:JSON",
      "images:JSON",
      "featured:BOOLEAN",
      "seo:JSON",
      "status:STRING",
      "first_published_at:DATE"),
  CATEGORY(
      "category",
      "name:STRING",
      "slug:STRING",
      "description:STRING",
      "enabled:BOOLEAN",
      "sort_order:NUMBER"),
  CONTENT(
      "content",
      "type:STRING",
      "slug:STRING",
      "title:STRING",
      "excerpt:STRING",
      "body_markdown:STRING",
      "cover:JSON",
      "seo:JSON",
      "status:STRING",
      "first_published_at:DATE",
      "is_system:BOOLEAN"),
  FAQ(
      "faq",
      "question:STRING",
      "answer:STRING",
      "group_name:STRING",
      "enabled:BOOLEAN",
      "sort_order:NUMBER"),
  SITE(
      "site_settings",
      "brand_name:STRING",
      "tagline:STRING",
      "contact_email:STRING",
      "contact_phone:STRING",
      "privacy_version:STRING"),
  HOME(
      "home_config",
      "section_order:JSON",
      "enabled_sections:JSON",
      "hero:JSON",
      "featured_product_ids:JSON",
      "dealer_cta:JSON"),
  INQUIRY(
      "contact_inquiry",
      "name:STRING",
      "email:STRING",
      "country:STRING",
      "type:STRING",
      "subject:STRING",
      "message:STRING",
      "product_id:ID",
      "privacy_version:STRING",
      "reference:STRING",
      "status:STRING",
      "internal_note:STRING",
      "consent_at:DATE"),
  APPLICATION(
      "dealer_application",
      "company_name:STRING",
      "contact_name:STRING",
      "email:STRING",
      "phone:STRING",
      "country:STRING",
      "website:STRING",
      "business_type:STRING",
      "interested_product_ids:JSON",
      "message:STRING",
      "privacy_version:STRING",
      "reference:STRING",
      "status:STRING",
      "outcome:STRING",
      "internal_note:STRING",
      "consent_at:DATE",
      "open_dedupe_key:STRING"),
  MEDIA(
      "media_asset",
      "url:STRING",
      "mime_type:STRING",
      "byte_size:NUMBER",
      "width:NUMBER",
      "height:NUMBER",
      "original_name:STRING"),
  AUDIT(
      "audit_log",
      "actor_id:ID",
      "action:STRING",
      "entity_type:STRING",
      "entity_id:ID",
      "before_data:JSON",
      "after_data:JSON",
      "request_id:STRING"),
  ADMIN(
      "admin_user", "email:STRING", "password_hash:STRING", "display_name:STRING", "status:STRING");
  public final String table;
  public final Map<String, String> fields = new LinkedHashMap<>();

  EntityType(String table, String... definitions) {
    this.table = table;
    for (String def : definitions) {
      String[] p = def.split(":");
      fields.put(p[0], p[1]);
    }
  }
}
