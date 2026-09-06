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
const orderStorageKey = "wemove.orders.v1";
const savedOrders = () => {
  try {
    const value = JSON.parse(localStorage.getItem(orderStorageKey) || "[]");
    return Array.isArray(value)
      ? value.filter(
          (item) =>
            item &&
            typeof item.orderNumber === "string" &&
            typeof item.accessToken === "string",
        )
      : [];
  } catch {
    return [];
  }
};
const writeSavedOrders = (orders) => {
  try {
    localStorage.setItem(orderStorageKey, JSON.stringify(orders.slice(0, 50)));
  } catch {
    // The private order page remains usable when browser storage is unavailable.
  }
};
const rememberOrder = (orderNumber, accessToken) => {
  if (!orderNumber || !accessToken) return;
  const orders = savedOrders().filter((item) => item.orderNumber !== orderNumber);
  orders.unshift({ orderNumber, accessToken, savedAt: new Date().toISOString() });
  writeSavedOrders(orders);
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
      rememberOrder(order.order_number, order.access_token);
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

const orderPage = document.querySelector("[data-order-number]");
if (orderPage) rememberOrder(orderPage.dataset.orderNumber, orderPage.dataset.orderToken);

const ordersList = document.querySelector("[data-orders-list]");
if (ordersList) {
  const empty = document.querySelector("[data-orders-empty]");
  const notice = document.querySelector("[data-orders-notice]");
  const money = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "CNY",
  });
  const date = new Intl.DateTimeFormat("en", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
  const text = (tag, className, value) => {
    const node = document.createElement(tag);
    if (className) node.className = className;
    node.textContent = value;
    return node;
  };
  const renderOrder = (order, saved) => {
    const card = document.createElement("article");
    card.className = "saved-order-card";
    const heading = document.createElement("div");
    heading.className = "saved-order-heading";
    const identity = document.createElement("div");
    identity.append(
      text("span", "saved-order-date", date.format(new Date(order.created_at))),
      text("h2", "", order.order_number),
    );
    const status = text("span", `order-status ${order.status}`, order.status.replaceAll("_", " "));
    heading.append(identity, status);
    const items = document.createElement("div");
    items.className = "saved-order-items";
    (order.items || []).forEach((item) => {
      const row = document.createElement("div");
      row.append(
        text("span", "", `${item.product_name} × ${item.quantity}`),
        text("strong", "", money.format(item.line_total_cents / 100)),
      );
      items.append(row);
    });
    const footer = document.createElement("div");
    footer.className = "saved-order-footer";
    const total = document.createElement("p");
    total.append("Total ", text("strong", "", money.format(order.total_cents / 100)));
    const link = text("a", "button primary", "View order →");
    link.href = `/orders/${encodeURIComponent(order.order_number)}?token=${encodeURIComponent(saved.accessToken)}`;
    footer.append(total, link);
    card.append(heading, items, footer);
    return card;
  };
  const loadOrders = async () => {
    const records = savedOrders();
    if (!records.length) {
      ordersList.hidden = true;
      empty.hidden = false;
      return;
    }
    const results = await Promise.all(
      records.map(async (saved) => {
        try {
          const response = await fetch(
            `/api/v1/orders/${encodeURIComponent(saved.orderNumber)}?access_token=${encodeURIComponent(saved.accessToken)}`,
          );
          if (response.status === 404) return { saved, missing: true };
          const result = await response.json();
          if (!response.ok) throw Error(result.message || "Unable to load this order.");
          return { saved, order: result.data };
        } catch {
          return { saved, failed: true };
        }
      }),
    );
    const visible = results.filter((result) => result.order);
    writeSavedOrders(results.filter((result) => !result.missing).map((result) => result.saved));
    ordersList.replaceChildren(...visible.map((result) => renderOrder(result.order, result.saved)));
    ordersList.hidden = !visible.length;
    empty.hidden = Boolean(visible.length);
    const failures = results.filter((result) => result.failed).length;
    if (failures) {
      notice.textContent = `${failures} order${failures === 1 ? "" : "s"} could not be refreshed. Check your connection and try again.`;
      notice.hidden = false;
    }
  };
  loadOrders();
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

const dealerMessage = document.querySelector("#dealer-message");
const showDealerMessage = (message, success = false) => {
  if (!dealerMessage) return;
  dealerMessage.textContent = message;
  dealerMessage.className = "form-message visible" + (success ? " success" : "");
  dealerMessage.focus();
};
const dealerActivation = document.querySelector("[data-dealer-activation]");
dealerActivation?.addEventListener("submit", async (event) => {
  event.preventDefault();
  const values = Object.fromEntries(new FormData(dealerActivation));
  if (!values.token) return showDealerMessage("This activation link is incomplete.");
  if (values.password !== values.confirm_password)
    return showDealerMessage("The passwords do not match.");
  const button = dealerActivation.querySelector("button");
  button.disabled = true;
  button.textContent = "Activating…";
  try {
    const response = await fetch("/api/v1/dealer/auth/activate", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": await commerceToken(),
      },
      body: JSON.stringify({ token: values.token, password: values.password }),
    });
    const result = await response.json();
    if (!response.ok) throw Error(commerceError(result));
    dealerActivation.hidden = true;
    showDealerMessage("Your account is active. You can now sign in.", true);
  } catch (error) {
    showDealerMessage(error.message || "Unable to activate the account.");
    button.disabled = false;
    button.textContent = "Activate account →";
  }
});

const dealerLogin = document.querySelector("[data-dealer-login]");
dealerLogin?.addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = dealerLogin.querySelector("button");
  button.disabled = true;
  button.textContent = "Signing in…";
  try {
    const response = await fetch("/api/v1/dealer/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": await commerceToken(),
      },
      body: JSON.stringify(Object.fromEntries(new FormData(dealerLogin))),
    });
    const result = await response.json();
    if (!response.ok) throw Error(commerceError(result));
    location.assign("/dealers/portal");
  } catch (error) {
    showDealerMessage(error.message || "Unable to sign in.");
    button.disabled = false;
    button.textContent = "Sign in →";
  }
});

document.querySelector("[data-dealer-logout]")?.addEventListener("click", async (event) => {
  event.currentTarget.disabled = true;
  try {
    const response = await fetch("/api/v1/dealer/auth/logout", {
      method: "POST",
      headers: { "X-CSRF-Token": await commerceToken() },
    });
    if (!response.ok) throw Error("Unable to sign out.");
    location.assign("/dealers/login");
  } catch (error) {
    showDealerMessage(error.message);
    event.currentTarget.disabled = false;
  }
});

document.querySelectorAll(".filters").forEach((form) =>
  form.addEventListener("submit", () => {
    form.querySelectorAll("input,select").forEach((input) => {
      if (!input.value) input.disabled = true;
    });
  }),
);
