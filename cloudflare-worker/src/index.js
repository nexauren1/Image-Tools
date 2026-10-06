const HEADERS = {
  "Content-Type": "application/json; charset=utf-8",
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "Authorization, Content-Type",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS"
};

const reply = (data, status = 200) => new Response(
  typeof data === "string" ? data : JSON.stringify(data),
  { status, headers: HEADERS }
);

const b64 = (input) => {
  const bytes = typeof input === "string" ? new TextEncoder().encode(input) : new Uint8Array(input);
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/g, "");
};

const pemBytes = (pem) => {
  const normalized = String(pem || "").replace(/\\n/g, "\n").replace(/\\r/g, "\r");
  const body = normalized.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replace(/\s/g, "");
  const raw = atob(body);
  const bytes = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i++) bytes[i] = raw.charCodeAt(i);
  return bytes.buffer;
};

async function signJwt(input, privateKey) {
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemBytes(privateKey),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
  return crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(input));
}

async function googleAccessToken(env) {
  if (!env.FIREBASE_CLIENT_EMAIL || !env.FIREBASE_PRIVATE_KEY) {
    const error = new Error("Firebase server credentials are not configured");
    error.code = "FIREBASE_SERVER_CONFIG_MISSING";
    throw error;
  }
  const now = Math.floor(Date.now() / 1000);
  const head = b64(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = b64(JSON.stringify({
    iss: env.FIREBASE_CLIENT_EMAIL,
    scope: "https://www.googleapis.com/auth/datastore",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600
  }));
  const unsigned = head + "." + claim;
  const sig = b64(await signJwt(unsigned, env.FIREBASE_PRIVATE_KEY));
  const token = unsigned + "." + sig;
  const r = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: token
    })
  });
  const data = await r.json();
  if (!r.ok) throw new Error(data.error_description || "Google service authentication failed");
  return data.access_token;
}

async function firebaseUser(request, env) {
  const header = request.headers.get("Authorization") || "";
  if (!header.startsWith("Bearer ")) throw new Error("Missing Firebase ID token");
  const idToken = header.slice(7);
  const r = await fetch(
    "https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=" + encodeURIComponent(env.FIREBASE_WEB_API_KEY || "AIzaSyCtO5UOedU4qtdZBgQERMhygWYLUxybVTo"),
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ idToken })
    }
  );
  const data = await r.json();
  if (!r.ok || !data.users || !data.users[0] || !data.users[0].localId) {
    throw new Error("Invalid Firebase ID token");
  }
  return data.users[0];
}

async function paypalToken(env) {
  const base = env.PAYPAL_ENVIRONMENT === "live"
    ? "https://api-m.paypal.com"
    : "https://api-m.sandbox.paypal.com";
  const basic = btoa(env.PAYPAL_CLIENT_ID + ":" + env.PAYPAL_CLIENT_SECRET);
  const r = await fetch(base + "/v1/oauth2/token", {
    method: "POST",
    headers: {
      "Authorization": "Basic " + basic,
      "Content-Type": "application/x-www-form-urlencoded"
    },
    body: "grant_type=client_credentials"
  });
  const data = await r.json();
  if (!r.ok) throw new Error(data.error_description || "PayPal authentication failed");
  return { base: base, token: data.access_token };
}

async function firestoreUser(env, uid) {
  const access = await googleAccessToken(env);
  const url =
    "https://firestore.googleapis.com/v1/projects/" +
    encodeURIComponent(env.FIREBASE_PROJECT_ID || "nexauren-story") +
    "/databases/(default)/documents/users/" +
    encodeURIComponent(uid);

  const r = await fetch(url, {
    headers: { "Authorization": "Bearer " + access }
  });
  if (r.status === 404) return {};
  const data = await r.json();
  if (!r.ok) throw new Error("Firestore user lookup failed");
  return data.fields || {};
}

const fieldString = (fields, name) => fields && fields[name] && fields[name].stringValue || "";

async function setSubscriptionEntitlement(env, uid, subscriptionId, status, premium) {
  const access = await googleAccessToken(env);
  const url =
    "https://firestore.googleapis.com/v1/projects/" +
    encodeURIComponent(env.FIREBASE_PROJECT_ID || "nexauren-story") +
    "/databases/(default)/documents/users/" +
    encodeURIComponent(uid) +
    "?updateMask.fieldPaths=plan" +
    "&updateMask.fieldPaths=premium" +
    "&updateMask.fieldPaths=paymentProvider" +
    "&updateMask.fieldPaths=paymentProduct" +
    "&updateMask.fieldPaths=paypalSubscriptionId" +
    "&updateMask.fieldPaths=subscriptionStatus" +
    "&updateMask.fieldPaths=updatedAt";

  const body = {
    fields: {
      plan: { stringValue: premium ? "premium-monthly" : "free" },
      premium: { booleanValue: premium },
      paymentProvider: { stringValue: "paypal" },
      paymentProduct: { stringValue: "image-tools-premium-monthly" },
      paypalSubscriptionId: { stringValue: subscriptionId },
      subscriptionStatus: { stringValue: status },
      updatedAt: { timestampValue: new Date().toISOString() }
    }
  };

  const r = await fetch(url, {
    method: "PATCH",
    headers: {
      "Authorization": "Bearer " + access,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(body)
  });

  if (!r.ok) throw new Error("Firestore subscription update failed");
}

async function getPayPalSubscription(paypal, subscriptionId) {
  const r = await fetch(
    paypal.base + "/v1/billing/subscriptions/" + encodeURIComponent(subscriptionId),
    {
      headers: {
        "Authorization": "Bearer " + paypal.token,
        "Content-Type": "application/json"
      }
    }
  );
  const data = await r.json();
  if (!r.ok) throw new Error(data.message || "PayPal subscription lookup failed");
  return data;
}

async function verifyWebhook(request, env, paypal, rawBody) {
  const payload = JSON.parse(rawBody);
  const verifyPayload = {
    auth_algo: request.headers.get("paypal-auth-algo") || "",
    cert_url: request.headers.get("paypal-cert-url") || "",
    transmission_id: request.headers.get("paypal-transmission-id") || "",
    transmission_sig: request.headers.get("paypal-transmission-sig") || "",
    transmission_time: request.headers.get("paypal-transmission-time") || "",
    webhook_id: env.PAYPAL_WEBHOOK_ID || "",
    webhook_event: payload
  };

  if (!verifyPayload.webhook_id || !verifyPayload.transmission_id ||
      !verifyPayload.transmission_time || !verifyPayload.transmission_sig ||
      !verifyPayload.cert_url) {
    return false;
  }

  const r = await fetch(paypal.base + "/v1/notifications/verify-webhook-signature", {
    method: "POST",
    headers: {
      "Authorization": "Bearer " + paypal.token,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(verifyPayload)
  });
  const data = await r.json();
  return r.ok && data.verification_status === "SUCCESS";
}

async function handleSubscriptionWebhook(env, event, paypal) {
  const type = event.event_type || "";
  const resource = event.resource || {};
  let subscriptionId = "";
  let uid = "";

  if (type.startsWith("BILLING.SUBSCRIPTION.")) {
    subscriptionId = resource.id || "";
    uid = resource.custom_id || "";
  } else if (type.startsWith("PAYMENT.SALE.")) {
    subscriptionId = resource.billing_agreement_id || "";
    uid = resource.custom_id || "";
  }

  if (!subscriptionId) return;

  if (!uid) {
    const subscription = await getPayPalSubscription(paypal, subscriptionId);
    uid = subscription.custom_id || "";
  }

  if (!uid) return;

  let premium = false;
  let status = String(resource.status || "UNKNOWN");

  if (type === "BILLING.SUBSCRIPTION.ACTIVATED") {
    premium = true;
    status = "ACTIVE";
  } else if (type === "BILLING.SUBSCRIPTION.UPDATED") {
    status = String(resource.status || "UNKNOWN");
    premium = status === "ACTIVE";
  } else if (type === "PAYMENT.SALE.COMPLETED") {
    premium = true;
    status = "ACTIVE";
  } else if (
    type === "BILLING.SUBSCRIPTION.CANCELLED" ||
    type === "BILLING.SUBSCRIPTION.EXPIRED" ||
    type === "BILLING.SUBSCRIPTION.SUSPENDED" ||
    type === "BILLING.SUBSCRIPTION.PAYMENT.FAILED" ||
    type === "PAYMENT.SALE.REFUNDED" ||
    type === "PAYMENT.SALE.REVERSED"
  ) {
    premium = false;
  } else if (type === "BILLING.SUBSCRIPTION.CREATED") {
    premium = false;
    status = "APPROVAL_PENDING";
  } else {
    return;
  }

  await setSubscriptionEntitlement(env, uid, subscriptionId, status, premium);
}

async function paypalJson(paypal, path, options = {}) {
  const response = await fetch(paypal.base + path, {
    ...options,
    headers: {
      "Authorization": "Bearer " + paypal.token,
      "Content-Type": "application/json",
      "Accept": "application/json",
      ...(options.headers || {})
    }
  });
  const data = await response.json().catch(() => ({}));
  return { response, data };
}


function throwPaypalError(stage, data, fallback) {
  const details = Array.isArray(data && data.details) ? data.details[0] : null;
  const error = new Error(data && data.message ? data.message : fallback);
  error.stage = stage;
  error.code = (details && details.issue) || (data && data.name) || "PAYPAL_ERROR";
  error.debugId = (data && data.debug_id) || "";
  throw error;
}

function withPaypalHeaders(paypal, extra = {}) {
  return {
    "Authorization": "Bearer " + paypal.token,
    "Content-Type": "application/json",
    "Accept": "application/json",
    ...extra
  };
}

async function getPaypalProduct(paypal, productId) {
  const { response, data } = await paypalJson(
    paypal,
    "/v1/catalogs/products/" + encodeURIComponent(productId),
    { method: "GET" }
  );
  if (response.status === 404) return null;
  if (!response.ok) throwPaypalError("product-lookup", data, "PayPal product lookup failed");
  return data;
}

async function listPaypalProducts(paypal) {
  const { response, data } = await paypalJson(
    paypal,
    "/v1/catalogs/products?page_size=20&page=1&total_required=true",
    { method: "GET" }
  );
  if (!response.ok) throwPaypalError("product-list", data, "PayPal product list failed");
  return data.products || [];
}

async function createPaypalProduct(paypal, spec) {
  const body = {
    name: spec.name,
    description: spec.description,
    type: spec.type || "DIGITAL"
  };

  if (spec.category) body.category = spec.category;
  if (spec.homeUrl) body.home_url = spec.homeUrl;

  if (spec.id && /^PROD-[A-Z0-9]+$/.test(spec.id)) body.id = spec.id;
  if (spec.imageUrl) body.image_url = spec.imageUrl;

  const { response, data } = await paypalJson(
    paypal,
    "/v1/catalogs/products",
    {
      method: "POST",
      headers: {
        "Prefer": "return=representation",
        "PayPal-Request-Id": "image-tools-product-" + spec.id
      },
      body: JSON.stringify(body)
    }
  );

  if (!response.ok) {
    throwPaypalError("product-create", data, "PayPal product creation failed");
  }
  return data;
}

async function ensurePaypalProduct(env, paypal, spec) {
  const configuredId = env.PAYPAL_PRODUCT_ID && /^PROD-[A-Z0-9]+$/.test(env.PAYPAL_PRODUCT_ID)
    ? env.PAYPAL_PRODUCT_ID
    : "";

  if (configuredId) {
    const existing = await getPaypalProduct(paypal, configuredId);
    if (existing) return existing;
  }

  const products = await listPaypalProducts(paypal);
  const byName = products.find((item) => item.name === spec.name);
  if (byName) return byName;

  return createPaypalProduct(paypal, { ...spec, id: undefined });
}

async function listPaypalPlans(paypal, productId) {
  const { response, data } = await paypalJson(
    paypal,
    "/v1/billing/plans?product_id=" + encodeURIComponent(productId) +
      "&page_size=20&page=1&total_required=true",
    { method: "GET" }
  );
  if (!response.ok) throwPaypalError("plan-list", data, "PayPal plan list failed");
  return data.plans || [];
}

function planMatches(plan, spec) {
  const cycle = (plan.billing_cycles || []).find((item) => item.tenure_type === "REGULAR");
  const price = cycle && cycle.pricing_scheme && cycle.pricing_scheme.fixed_price;
  const frequency = cycle && cycle.frequency;
  return Boolean(
    plan.name === spec.name &&
    price &&
    price.currency_code === spec.currency &&
    price.value === spec.price &&
    frequency &&
    frequency.interval_unit === spec.intervalUnit &&
    Number(frequency.interval_count || 1) === Number(spec.intervalCount || 1) &&
    Number(cycle.total_cycles || 0) === 0
  );
}

async function activatePaypalPlan(paypal, planId) {
  const { response, data } = await paypalJson(
    paypal,
    "/v1/billing/plans/" + encodeURIComponent(planId) + "/activate",
    {
      method: "POST",
      body: JSON.stringify({})
    }
  );
  if (!response.ok) throwPaypalError("plan-activate", data, "PayPal plan activation failed");
  return data;
}

async function createPaypalPlan(paypal, productId, spec) {
  const { response, data } = await paypalJson(
    paypal,
    "/v1/billing/plans",
    {
      method: "POST",
      headers: {
        "PayPal-Request-Id": "image-tools-plan-" +
          String(spec.key || spec.name).toLowerCase().replace(/[^a-z0-9]+/g, "-")
      },
      body: JSON.stringify({
        product_id: productId,
        name: spec.name,
        description: spec.description,
        billing_cycles: [{
          frequency: {
            interval_unit: spec.intervalUnit,
            interval_count: Number(spec.intervalCount || 1)
          },
          tenure_type: "REGULAR",
          sequence: 1,
          total_cycles: 0,
          pricing_scheme: {
            fixed_price: {
              value: spec.price,
              currency_code: spec.currency
            }
          }
        }],
        payment_preferences: {
          auto_bill_outstanding: true,
          payment_failure_threshold: 1
        }
      })
    }
  );

  if (!response.ok) throwPaypalError("plan-create", data, "PayPal plan creation failed");
  if (data.status !== "ACTIVE") {
    await activatePaypalPlan(paypal, data.id);
    data.status = "ACTIVE";
  }
  return data;
}

async function ensurePaypalPlan(paypal, productId, spec) {
  const configuredId = spec.planId || "";
  if (configuredId) {
    const { response, data } = await paypalJson(
      paypal,
      "/v1/billing/plans/" + encodeURIComponent(configuredId),
      { method: "GET" }
    );
    if (response.ok && planMatches(data, spec) && data.status !== "INACTIVE") {
      if (data.status === "CREATED") {
        await activatePaypalPlan(paypal, data.id);
        data.status = "ACTIVE";
      }
      return data;
    }
  }

  const plans = await listPaypalPlans(paypal, productId);
  const matching = plans.find((plan) => planMatches(plan, spec) && plan.status !== "INACTIVE");
  if (matching) {
    if (matching.status === "CREATED") {
      await activatePaypalPlan(paypal, matching.id);
      matching.status = "ACTIVE";
    }
    return matching;
  }

  return createPaypalPlan(paypal, productId, spec);
}

async function ensurePaypalCatalog(env, paypal, plans = []) {
  const product = await ensurePaypalProduct(env, paypal, {
    id: env.PAYPAL_PRODUCT_ID || "",
    name: env.PAYPAL_PRODUCT_NAME || "Image Tools Premium",
    description: env.PAYPAL_PRODUCT_DESCRIPTION || "Premium image editing features for Image Tools.",
    type: "DIGITAL",
    category: "",
    homeUrl: env.PAYPAL_PRODUCT_HOME_URL || ""
  });

  const defaultPlan = {
    key: "premium_monthly",
    name: env.PAYPAL_PLAN_NAME || "Image Tools Premium Monthly",
    description: env.PAYPAL_PLAN_DESCRIPTION || "Premium access billed monthly.",
    price: env.PAYPAL_PLAN_PRICE || "5.00",
    currency: env.PAYPAL_PLAN_CURRENCY || "USD",
    intervalUnit: env.PAYPAL_PLAN_INTERVAL_UNIT || "MONTH",
    intervalCount: Number(env.PAYPAL_PLAN_INTERVAL_COUNT || 1),
    planId: env.PAYPAL_PLAN_ID || ""
  };

  const requested = [defaultPlan, ...plans].filter((plan, index, all) =>
    all.findIndex((item) => (item.key || item.name) === (plan.key || plan.name)) === index
  );

  const results = [];
  for (const plan of requested) {
    results.push(await ensurePaypalPlan(paypal, product.id, plan));
  }

  return {
    product,
    plans: results
  };
}

async function requireAdminCatalog(request, env) {
  const expected = env.PAYPAL_CATALOG_ADMIN_KEY;
  const provided = request.headers.get("X-Admin-Key") || "";
  if (!expected || provided !== expected) {
    throw new Error("Unauthorized catalog administration");
  }
}

async function createSubscription(request, env) {
  const user = await firebaseUser(request, env);
  const paypal = await paypalToken(env);

  let plan;
  let productId = "";
  if (env.PAYPAL_PLAN_ID) {
    const { response, data } = await paypalJson(
      paypal,
      "/v1/billing/plans/" + encodeURIComponent(env.PAYPAL_PLAN_ID),
      { method: "GET" }
    );
    if (!response.ok) throwPaypalError("plan-lookup", data, "Configured PayPal plan could not be found");
    if (data.status !== "ACTIVE") {
      throw new Error("Configured PayPal plan is not active");
    }
    plan = data;
    productId = data.product_id || "";
  } else {
    const catalog = await ensurePaypalCatalog(env, paypal);
    plan = catalog.plans[0];
    productId = catalog.product.id;
    if (!plan || plan.status !== "ACTIVE") {
      throw new Error("Premium monthly plan is not active");
    }
  }

  const publicUrl = (env.WORKER_PUBLIC_URL || new URL(request.url).origin).replace(/\/$/, "");
  const { response, data } = await paypalJson(
    paypal,
    "/v1/billing/subscriptions",
    {
      method: "POST",
      headers: {
        "PayPal-Request-Id": "image-tools-subscription-" + user.localId + "-" + Date.now()
      },
      body: JSON.stringify({
        plan_id: plan.id,
        custom_id: user.localId,
        application_context: {
          brand_name: "Image Tools",
          locale: "en-US",
          user_action: "SUBSCRIBE_NOW",
          payment_method: {
            payer_selected: "PAYPAL",
            payee_preferred: "IMMEDIATE_PAYMENT_REQUIRED"
          },
          return_url: publicUrl + "/paypal/return",
          cancel_url: publicUrl + "/paypal/return?cancel=1"
        }
      })
    }
  );

  if (!response.ok) {
    throwPaypalError("subscription-create", data, "PayPal subscription creation failed");
  }

  const approve = (data.links || []).find((item) => item.rel === "approve");
  if (!approve) throw new Error("PayPal approval URL was not returned");

  return reply({
    ok: true,
    subscriptionId: data.id,
    approveUrl: approve.href,
    productId: productId,
    planId: plan.id,
    status: data.status || "APPROVAL_PENDING"
  });
}

async function adminCatalog(request, env) {
  await requireAdminCatalog(request, env);
  const body = await request.json().catch(() => ({}));
  const plans = Array.isArray(body.plans) ? body.plans : [];
  const paypal = await paypalToken(env);
  const catalog = await ensurePaypalCatalog(env, paypal, plans);

  return reply({
    ok: true,
    product: {
      id: catalog.product.id,
      name: catalog.product.name,
      status: catalog.product.status || "ACTIVE"
    },
    plans: catalog.plans.map((plan) => ({
      id: plan.id,
      name: plan.name,
      status: plan.status,
      productId: plan.product_id
    }))
  });
}

async function subscriptionStatus(request, env, url) {
  const user = await firebaseUser(request, env);
  const paypal = await paypalToken(env);
  let subscriptionId = url.searchParams.get("subscriptionId") || "";

  if (!subscriptionId) {
    const fields = await firestoreUser(env, user.localId);
    subscriptionId = fieldString(fields, "paypalSubscriptionId");
  }

  if (!subscriptionId) {
    return reply({ ok: true, premium: false, status: "NONE" });
  }

  let data;
  try {
    data = await getPayPalSubscription(paypal, subscriptionId);
  } catch (error) {
    throw error;
  }
  if (data.custom_id && data.custom_id !== user.localId) {
    return reply({ ok: false, error: "Subscription ownership mismatch" }, 403);
  }

  const premium = data.status === "ACTIVE";
  await setSubscriptionEntitlement(
    env,
    user.localId,
    subscriptionId,
    data.status || "UNKNOWN",
    premium
  );

  return reply({
    ok: true,
    premium,
    status: data.status || "UNKNOWN",
    subscriptionId
  });
}

async function cancelSubscription(request, env) {
  const user = await firebaseUser(request, env);
  const paypal = await paypalToken(env);
  const fields = await firestoreUser(env, user.localId);
  const subscriptionId = fieldString(fields, "paypalSubscriptionId");
  if (!subscriptionId) return reply({ ok: false, error: "No active subscription found" }, 404);

  const current = await getPayPalSubscription(paypal, subscriptionId);
  if (current.custom_id && current.custom_id !== user.localId) {
    return reply({ ok: false, error: "Subscription ownership mismatch" }, 403);
  }

  if (current.status === "CANCELLED" || current.status === "EXPIRED") {
    await setSubscriptionEntitlement(env, user.localId, subscriptionId, current.status, false);
    return reply({ ok: true, premium: false, status: current.status });
  }

  const r = await fetch(
    paypal.base + "/v1/billing/subscriptions/" + encodeURIComponent(subscriptionId) + "/cancel",
    {
      method: "POST",
      headers: {
        "Authorization": "Bearer " + paypal.token,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ reason: "User requested cancellation" })
    }
  );

  if (!r.ok) {
    const data = await r.json();
    throw new Error(data.message || "PayPal subscription cancellation failed");
  }

  await setSubscriptionEntitlement(
    env,
    user.localId,
    subscriptionId,
    "CANCELLED",
    false
  );

  return reply({ ok: true, premium: false, status: "CANCELLED" });
}

async function createOrder(request, env) {
  const user = await firebaseUser(request, env);
  const paypal = await paypalToken(env);

  const publicUrl = (env.WORKER_PUBLIC_URL || new URL(request.url).origin).replace(/\/$/, "");
  const r = await fetch(paypal.base + "/v2/checkout/orders", {
    method: "POST",
    headers: {
      "Authorization": "Bearer " + paypal.token,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      intent: "CAPTURE",
      purchase_units: [{
        reference_id: "image-tools-premium",
        custom_id: user.localId,
        description: "Image Tools Premium",
        amount: { currency_code: "USD", value: "9.00" }
      }],
      application_context: {
        brand_name: "Image Tools",
        landing_page: "LOGIN",
        user_action: "PAY_NOW",
        return_url: publicUrl + "/paypal/return",
        cancel_url: publicUrl + "/paypal/return?cancel=1"
      }
    })
  });

  const data = await r.json();
  if (!r.ok) throw new Error(data.message || "PayPal order creation failed");
  const approve = (data.links || []).find((item) => item.rel === "approve");
  if (!approve) throw new Error("PayPal approval URL was not returned");

  return reply({ ok: true, orderId: data.id, approveUrl: approve.href });
}

async function captureOrder(request, env) {
  const user = await firebaseUser(request, env);
  const body = await request.json();
  const orderId = body.orderId;
  if (!orderId) return reply({ ok: false, error: "Missing orderId" }, 400);

  const paypal = await paypalToken(env);
  const r = await fetch(paypal.base + "/v2/checkout/orders/" + encodeURIComponent(orderId) + "/capture", {
    method: "POST",
    headers: {
      "Authorization": "Bearer " + paypal.token,
      "Content-Type": "application/json"
    }
  });
  const data = await r.json();
  if (!r.ok) throw new Error(data.message || "PayPal capture failed");

  const purchaseUnit = data.purchase_units && data.purchase_units[0];
  const captures = purchaseUnit && purchaseUnit.payments && purchaseUnit.payments.captures || [];
  const completed = data.status === "COMPLETED" && captures.some((capture) =>
    capture.status === "COMPLETED" &&
    capture.amount &&
    capture.amount.currency_code === "USD" &&
    capture.amount.value === "9.00"
  );

  if (!completed) return reply({ ok: false, error: "Payment not completed", status: data.status }, 400);

  if (purchaseUnit.custom_id && purchaseUnit.custom_id !== user.localId) {
    return reply({ ok: false, error: "Payment ownership mismatch" }, 403);
  }

  await setPremium(env, user.localId, orderId);
  return reply({ ok: true, premium: true, orderId: orderId });
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return new Response(null, { headers: HEADERS });
    const url = new URL(request.url);

    try {
      if (request.method === "GET" && url.pathname === "/health") {
        return reply({
          ok: true,
          service: "image-tools-payments",
          environment: env.PAYPAL_ENVIRONMENT || "sandbox",
          paypalConfigured: Boolean(env.PAYPAL_CLIENT_ID && env.PAYPAL_CLIENT_SECRET),
          paypalPlanConfigured: Boolean(env.PAYPAL_PLAN_ID),
          paypalWebhookConfigured: Boolean(env.PAYPAL_WEBHOOK_ID),
          paypalCatalogAutoCreate: true,
          paypalCatalogAdminConfigured: Boolean(env.PAYPAL_CATALOG_ADMIN_KEY),
          firebaseAuthConfigured: Boolean(env.FIREBASE_WEB_API_KEY),
          firestoreAdminConfigured: Boolean(
            env.FIREBASE_PROJECT_ID &&
            env.FIREBASE_CLIENT_EMAIL &&
            env.FIREBASE_PRIVATE_KEY &&
            env.FIREBASE_WEB_API_KEY
          ),
          firebaseProjectConfigured: Boolean(env.FIREBASE_PROJECT_ID),
          firebaseServiceAccountConfigured: Boolean(env.FIREBASE_CLIENT_EMAIL && env.FIREBASE_PRIVATE_KEY)
        });
      }

      if (request.method === "POST" && url.pathname === "/admin/paypal/catalog/ensure") {
        return adminCatalog(request, env);
      }

      if (request.method === "POST" && url.pathname === "/paypal/create-subscription") {
        return createSubscription(request, env);
      }

      if (request.method === "GET" && url.pathname === "/paypal/subscription-status") {
        return subscriptionStatus(request, env, url);
      }

      if (request.method === "POST" && url.pathname === "/paypal/cancel-subscription") {
        return cancelSubscription(request, env);
      }

      if (request.method === "POST" && url.pathname === "/paypal/webhook") {
        const rawBody = await request.text();
        const paypal = await paypalToken(env);
        if (!(await verifyWebhook(request, env, paypal, rawBody))) {
          return reply({ ok: false, error: "Invalid PayPal webhook signature" }, 400);
        }
        await handleSubscriptionWebhook(env, JSON.parse(rawBody), paypal);
        return reply({ ok: true });
      }

      if (request.method === "GET" && url.pathname === "/paypal/return") {
        if (url.searchParams.get("cancel") === "1") {
          return new Response("Subscription cancelled. Return to Image Tools.", {
            status: 200,
            headers: { "Content-Type": "text/plain; charset=utf-8" }
          });
        }

        const subscriptionId =
          url.searchParams.get("subscription_id") ||
          url.searchParams.get("ba_token") ||
          url.searchParams.get("token");

        if (!subscriptionId) {
          return new Response("Missing subscription token.", { status: 400 });
        }

        return Response.redirect(
          "imagetools://paypal/return?subscriptionId=" + encodeURIComponent(subscriptionId),
          302
        );
      }

      return reply({ ok: false, error: "Not found" }, 404);
    } catch (error) {
      const code = error && error.code ? error.code : "UNEXPECTED_ERROR";
      const stage = error && error.stage ? error.stage : "worker";
      const status = code === "PERMISSION_DENIED" || code === "NOT_AUTHORIZED" ? 403 : 500;
      return reply({
        ok: false,
        error: error && error.message ? error.message : "Unexpected error",
        code,
        stage,
        debugId: error && error.debugId ? error.debugId : null
      }, status);
    }
  }
};
