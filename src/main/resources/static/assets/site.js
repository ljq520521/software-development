"use strict";
const menu = document.querySelector(".menu-button");
menu?.addEventListener("click", () => {
  const open = menu.getAttribute("aria-expanded") !== "true";
  menu.setAttribute("aria-expanded", String(open));
  document.querySelector("#navigation").classList.toggle("open", open);
});
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape" && menu?.getAttribute("aria-expanded") === "true") {
    menu.click();
    menu.focus();
  }
});
document.querySelectorAll("[data-page-step]").forEach((button) =>
  button.addEventListener("click", () => {
    const url = new URL(location.href);
    url.searchParams.set(
      "page",
      String(
        Math.max(
          1,
          Number(url.searchParams.get("page") || 1) +
            Number(button.dataset.pageStep),
        ),
      ),
    );
    location.assign(url);
  }),
);
document.querySelectorAll("[data-image-src]").forEach((button) =>
  button.addEventListener("click", () => {
    const image = document.querySelector("#product-photo");
    image.src = button.dataset.imageSrc;
    image.alt = button.querySelector("img").alt;
  }),
);
const form = document.querySelector("[data-lead-form]");
if (form) {
  let key = crypto.randomUUID(),
    csrf = "",
    payloadSnapshot = "";
  const box = document.querySelector("#form-message");
  const show = (message, success = false) => {
    box.textContent = message;
    box.className = "form-message visible" + (success ? " success" : "");
    box.focus();
  };
  const token = async () => {
    const r = await fetch("/api/v1/auth/csrf");
    const j = await r.json();
    if (!r.ok) throw Error(j.message);
    csrf = j.data.csrf_token;
  };
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    const button = form.querySelector("[type=submit]"),
      old = button.textContent;
    button.disabled = true;
    button.textContent = "Sending…";
    box.className = "form-message";
    try {
      if (!csrf) await token();
      const values = Object.fromEntries(new FormData(form));
      values.privacy_consent = Boolean(form.elements.privacy_consent.checked);
      if (form.dataset.leadForm === "dealer") {
        values.interested_product_ids = [
          ...form.querySelectorAll("[name=interested_product_ids]:checked"),
        ].map((x) => x.value);
        if (!values.website) values.website = "";
      } else if (!values.product_id) delete values.product_id;
      const serialized = JSON.stringify(values);
      if (payloadSnapshot && payloadSnapshot !== serialized)
        key = crypto.randomUUID();
      payloadSnapshot = serialized;
      const r = await fetch(
        form.dataset.leadForm === "dealer"
          ? "/api/v1/dealer/applications"
          : "/api/v1/forms/contact",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": csrf,
            "Idempotency-Key": key,
          },
          body: serialized,
        },
      );
      const j = await r.json();
      if (!r.ok) {
        if (j.code === "CSRF_INVALID") csrf = "";
        if (j.code === "PRIVACY_VERSION_CHANGED") {
          const site = await (await fetch("/api/v1/site")).json();
          form.elements.privacy_version.value = site.data.privacy_version;
          form.elements.privacy_consent.checked = false;
        }
        const errors = Object.entries(j.field_errors || {}).map(
          ([field, messages]) =>
            field.replaceAll("_", " ") + ": " + messages.join(" "),
        );
        throw Error(errors.length ? errors.join("\n") : j.message);
      }
      show(
        "Thank you. Your " +
          (form.dataset.leadForm === "dealer" ? "application" : "message") +
          " has been received.\n\nReference: " +
          j.data.reference +
          "\n\nPlease save this reference for your records.",
        true,
      );
      form.hidden = true;
    } catch (e) {
      show(e.message || "Unable to submit. Please try again.");
    } finally {
      button.disabled = false;
      button.textContent = old;
    }
  });
  const type = form.querySelector("[name=type]");
  type?.addEventListener("change", () => {
    form.elements.product_id.required = type.value === "product_question";
  });
}

const commerceForm = document.querySelector("[data-commerce-form]");
const commerceMessage = document.querySelector("#commerce-message");
const showCommerceMessage = (message, success = false) => {
  if (!commerceMessage) return;
  commerceMessage.textContent = message;
  commerceMessage.className = "form-message visible" + (success ? " success" : "");
  commerceMessage.focus();
};
const commerceToken = async () => {
  const response = await fetch("/api/v1/auth/csrf");
  const result = await response.json();
  if (!response.ok) throw Error(result.message || "Unable to start the request.");
  return result.data.csrf_token;
};
const commerceError = (result) => {
  const details = Object.entries(result.field_errors || {}).map(
    ([field, messages]) => `${field.replaceAll("_", " ")}: ${messages.join(" ")}`,
  );
  return details.length ? details.join("\n") : result.message || "The request could not be completed.";
};
if (commerceForm) {
  const orderKey = crypto.randomUUID();
  commerceForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const button = commerceForm.querySelector("[type=submit]");
    const original = button.textContent;
    button.disabled = true;
    button.textContent = "Creating order…";
    try {
      const values = Object.fromEntries(new FormData(commerceForm));
      values.quantity = Number(values.quantity);
      values.privacy_consent = commerceForm.elements.privacy_consent.checked;
      const response = await fetch("/api/v1/orders", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": await commerceToken(),
          "Idempotency-Key": orderKey,
        },
        body: JSON.stringify(values),
      });
      const result = await response.json();
      if (!response.ok) throw Error(commerceError(result));
      const order = result.data;
      location.assign(
        `/orders/${encodeURIComponent(order.order_number)}?token=${encodeURIComponent(order.access_token)}`,
      );
    } catch (error) {
      showCommerceMessage(error.message || "Unable to create the order.");
      button.disabled = false;
      button.textContent = original;
    }
  });
}

document.querySelectorAll("[data-payment-method]").forEach((button) => {
  button.addEventListener("click", async () => {
    const page = document.querySelector("[data-order-number]");
    const original = button.textContent;
    document.querySelectorAll("[data-payment-method]").forEach((item) => (item.disabled = true));
    button.textContent = "Processing…";
    try {
      const response = await fetch(
        `/api/v1/orders/${encodeURIComponent(page.dataset.orderNumber)}/payments`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": await commerceToken(),
          },
          body: JSON.stringify({
            access_token: page.dataset.orderToken,
            method: button.dataset.paymentMethod,
          }),
        },
      );
      const result = await response.json();
      if (!response.ok) throw Error(commerceError(result));
      showCommerceMessage("Payment recorded. Refreshing your order…", true);
      location.reload();
    } catch (error) {
      showCommerceMessage(error.message || "Unable to complete the payment.");
      document.querySelectorAll("[data-payment-method]").forEach((item) => (item.disabled = false));
      button.textContent = original;
    }
  });
});

document.querySelectorAll(".filters").forEach((form) =>
  form.addEventListener("submit", () => {
    form.querySelectorAll("input,select").forEach((input) => {
      if (!input.value) input.disabled = true;
    });
  }),
);
