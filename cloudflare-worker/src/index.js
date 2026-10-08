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

function requireEnv(env, name) {
  const value = String(env[name] || "").trim();
  if (!value) {
    const error = new Error(name + " is not configured");
    error.code = "CONFIG_MISSING_" + name;
    error.stage = "configuration";
    throw error;
  }
  return value;
}

function paypalEnvironment(env) {
  const value = String(env.PAYPAL_ENVIRONMENT || "").trim().toLowerCase();
  if (value !== "live" && value !== "sandbox") {
    const error = new Error("PAYPAL_ENVIRONMENT must be explicitly set to live or sandbox");
    error.code = "INVALID_PAYPAL_ENVIRONMENT";
    error.stage = "configuration";
    throw error;
  }
  return value;
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
  const firebaseApiKey = requireEnv(env, "FIREBASE_WEB_API_KEY");
  if (!header.startsWith("Bearer ")) throw new Error("Missing Firebase ID token");
  const idToken = header.slice(7);
  const r = await fetch(
    "https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=" + encodeURIComponent(firebaseApiKey),
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
  const base = paypalEnvironment(env) === "live"
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
  const projectId = requireEnv(env, "FIREBASE_PROJECT_ID");
  const url =
    "https://firestore.googleapis.com/v1/projects/" +
    encodeURIComponent(projectId) +
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
  const projectId = requireEnv(env, "FIREBASE_PROJECT_ID");
  const url =
    "https://firestore.googleapis.com/v1/projects/" +
    encodeURIComponent(projectId) +
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
  const data = await r.json().catch(() => ({}));

  if (!r.ok) {
    const error = new Error(data.message || "PayPal subscription lookup failed");
    error.code = data.name || (r.status === 404 ? "SUBSCRIPTION_NOT_FOUND" : "PAYPAL_SUBSCRIPTION_LOOKUP_FAILED");
    error.stage = "paypal-subscription-lookup";
    error.debugId = data.debug_id || "";
    error.httpStatus = r.status === 404 ? 404 : 502;
    throw error;
  }

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

  const subscriptionStatus = data.status || "APPROVAL_PENDING";

  // Persist the PayPal subscription ID immediately so the app can recover
  // the entitlement even when the browser return/deep-link is delayed.
  try {
    await setSubscriptionEntitlement(
      env,
      user.localId,
      data.id,
      subscriptionStatus,
      subscriptionStatus === "ACTIVE"
    );
  } catch (_) {
    // PayPal subscription creation must not fail because entitlement sync
    // is temporarily unavailable. Webhook/status refresh will retry it.
  }

  return reply({
    ok: true,
    subscriptionId: data.id,
    approveUrl: approve.href,
    productId: productId,
    planId: plan.id,
    status: subscriptionStatus
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

async function resolveOwnedSubscription(paypal, userId, candidates) {
  const seen = new Set();
  let lastError = null;

  for (const candidate of candidates) {
    const subscriptionId = String(candidate || "").trim();
    if (!subscriptionId || seen.has(subscriptionId)) continue;
    seen.add(subscriptionId);

    try {
      const current = await getPayPalSubscription(paypal, subscriptionId);

      if (current.custom_id && current.custom_id !== userId) {
        const error = new Error("Subscription ownership mismatch");
        error.code = "SUBSCRIPTION_OWNERSHIP_MISMATCH";
        error.stage = "subscription-ownership";
        lastError = error;
        continue;
      }

      return { subscriptionId, current };
    } catch (error) {
      lastError = error;
    }
  }

  if (lastError) throw lastError;

  const error = new Error("No PayPal subscription is linked to this account");
  error.code = "SUBSCRIPTION_NOT_FOUND";
  error.stage = "subscription-lookup";
  error.httpStatus = 404;
  throw error;
}

async function cancelSubscription(request, env) {
  const user = await firebaseUser(request, env);
  const paypal = await paypalToken(env);
  const body = await request.json().catch(() => ({}));
  const requestedSubscriptionId = String(body.subscriptionId || "").trim();

  const fields = await firestoreUser(env, user.localId);
  const storedSubscriptionId = fieldString(fields, "paypalSubscriptionId").trim();

  // Prefer the server-linked subscription. If it is missing, stale or belongs
  // to another PayPal account, use the APK's cached id as a recovery path.
  const resolved = await resolveOwnedSubscription(
    paypal,
    user.localId,
    [storedSubscriptionId, requestedSubscriptionId]
  );
  const subscriptionId = resolved.subscriptionId;
  const current = resolved.current;

  if (current.status === "CANCELLED" || current.status === "EXPIRED") {
    let entitlementSynced = true;
    try {
      await setSubscriptionEntitlement(
        env,
        user.localId,
        subscriptionId,
        current.status,
        false
      );
    } catch (error) {
      entitlementSynced = false;
      console.error("Subscription already inactive; Firestore sync failed", {
        uid: user.localId,
        subscriptionId,
        status: current.status,
        error: error && error.message ? error.message : String(error)
      });
    }

    return reply({
      ok: true,
      premium: false,
      status: current.status,
      subscriptionId,
      entitlementSynced
    });
  }

  const response = await fetch(
    paypal.base + "/v1/billing/subscriptions/" + encodeURIComponent(subscriptionId) + "/cancel",
    {
      method: "POST",
      headers: {
        "Authorization": "Bearer " + paypal.token,
        "Content-Type": "application/json",
        "Accept": "application/json"
      },
      body: JSON.stringify({ reason: "User requested cancellation from Image Tools" })
    }
  );

  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error = new Error(data.message || "PayPal subscription cancellation failed");
    error.code = data.name || "PAYPAL_CANCEL_FAILED";
    error.stage = "paypal-cancel";
    error.debugId = data.debug_id || "";
    error.httpStatus = response.status === 404 ? 404 :
      response.status === 401 || response.status === 403 ? 502 :
      response.status >= 500 ? 502 :
      response.status === 409 || response.status === 422 ? 409 :
      502;
    throw error;
  }

  let entitlementSynced = true;
  try {
    await setSubscriptionEntitlement(
      env,
      user.localId,
      subscriptionId,
      "CANCELLED",
      false
    );
  } catch (error) {
    entitlementSynced = false;
    console.error("PayPal cancellation succeeded; Firestore sync failed", {
      uid: user.localId,
      subscriptionId,
      error: error && error.message ? error.message : String(error)
    });
  }

  return reply({
    ok: true,
    premium: false,
    status: "CANCELLED",
    subscriptionId,
    entitlementSynced,
    warning: entitlementSynced
      ? null
      : "Subscription cancelled at PayPal, but entitlement sync failed."
  });
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


const SITE = "https://steep-pine-34fe.nexaurenstore.workers.dev";

function htmlPage(title, body) {
  return new Response("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><meta name=\"theme-color\" content=\"#0b1022\"><title>" +
    title +
    " • Image Tools</title><style>" +
    "body{margin:0;background:#f7f8fc;color:#172033;font-family:Inter,system-ui,-apple-system,Segoe UI,Roboto,sans-serif;line-height:1.65}" +
    "main{max-width:820px;margin:0 auto;padding:32px 18px 64px}header{padding:10px 0 24px}h1{font-size:36px;line-height:1.15;margin:0 0 8px}h2{margin-top:30px}a{color:#5b45d6;text-decoration:none;font-weight:700}" +
    ".brand{display:inline-block;padding:7px 11px;border-radius:999px;background:#ede9fe;color:#6d28d9;font-size:12px;font-weight:800;letter-spacing:.08em}" +
    ".card{background:white;border:1px solid #e5e7eb;border-radius:22px;padding:22px;margin:14px 0;box-shadow:0 8px 30px rgba(15,23,42,.05)}" +
    "footer{margin-top:36px;color:#64748b;font-size:13px}li{margin:7px 0}" +
    "</style></head><body><main><header><span class=\"brand\">IMAGE TOOLS</span><h1>" +
    title +
    "</h1></header><div class=\"card\">" + body + "</div><footer><a href=\"" + SITE + "\">Home</a> · <a href=\"" + SITE + "/privacy\">Privacy</a> · <a href=\"" + SITE + "/terms\">Terms</a> · <a href=\"" + SITE + "/support\">Support</a></footer></main></body></html>",
    {status:200,headers:{"Content-Type":"text/html; charset=utf-8","Cache-Control":"public, max-age=3600"}});
}

function homePage() {
  return htmlPage("Image Tools", 
    "<p>A focused Android image toolbox for resizing, compression, conversion, cropping, filters, privacy cleanup, collage, PDF export and more.</p>" +
    "<p>Core image editing runs locally on the device. Premium features use a secure PayPal checkout and server-side entitlement verification.</p>" +
    "<p><strong>Android:</strong> package <code>com.nexauren.imagetools</code> · version 1.11.4</p>" +
    "<p><a href=\"" + SITE + "/privacy\">Read the Privacy Policy</a></p>" +
    "<p><a href=\"" + SITE + "/terms\">Read the Terms of Service</a></p>" +
    "<p><a href=\"" + SITE + "/support\">Support and account deletion requests</a></p>");
}

function privacyPage() {
  return htmlPage("Privacy Policy", 
    "<p><strong>Last updated: October 7, 2026.</strong></p>" +
    "<p>Image Tools is an Android application focused on local image utilities. This policy explains the main data flows used by the app.</p>" +
    "<h2>1. Images and files</h2>" +
    "<p>The core image tools process selected files on the device. The app does not upload the image itself to the Image Tools backend merely to resize, compress, convert, crop, rotate, filter, watermark, adjust colors, blur, sharpen, pixelate, add borders, create palettes, create collages, remove backgrounds, recognize text with OCR, read EXIF metadata, blur detected faces, create GIFs, merge PDFs, apply social presets, or export HEIC/AVIF/PDF results. Exported files are saved to the location selected by you.</p>" +
    "<h2>2. Account data</h2>" +
    "<p>When you create or sign in to an account, Image Tools uses Firebase Authentication. Depending on the sign-in method, Firebase may process an email address and account/profile information supplied by you or by the selected identity provider.</p>" +
    "<h2>3. Subscription data</h2>" +
    "<p>If you use Premium, the app sends an authenticated request to our Cloudflare Worker to start, refresh or cancel a subscription. PayPal processes the payment and subscription transaction. The app backend stores a limited entitlement record such as subscription status, the PayPal subscription identifier and the internal Firebase user identifier so Premium access can be verified.</p>" +
    "<h2>4. Analytics and diagnostics</h2>" +
    "<p>The Android build includes Firebase Analytics. Firebase may process technical and usage information according to the configuration and policies of that service. We use this capability to understand general app usage and improve reliability.</p>" +
    "<h2>5. Security</h2>" +
    "<p>PayPal client secrets and Firebase service-account credentials are kept on the Cloudflare Worker and are not embedded in the APK. Network requests use HTTPS endpoints.</p>" +
    "<h2>6. Data sharing</h2>" +
    "<p>Payment information is processed by PayPal. Authentication and related account services are provided by Firebase. The Cloudflare Worker handles authenticated application requests. Each provider's own privacy policy governs the data it processes.</p>" +
    "<h2>7. Retention and deletion</h2>" +
    "<p>We keep account and subscription records for as long as reasonably necessary to provide the service, maintain billing records, prevent abuse and meet legal obligations. To request deletion of your Image Tools account and associated server-side account data, use the support page with the email address associated with your account.</p>" +
    "<h2>8. Children's privacy</h2>" +
    "<p>Image Tools is not designed to knowingly collect personal information from children who are below the minimum age required by applicable law. Parents or guardians may contact support with privacy questions or deletion requests.</p>" +
    "<h2>9. Changes</h2>" +
    "<p>This policy may be updated when the app's data practices change. The latest version is published on this page.</p>" +
    "<h2>10. Contact</h2>" +
    "<p>For privacy questions or deletion requests, visit <a href=\"" + SITE + "/support\">" + SITE + "/support</a>.</p>");
}

function termsPage() {
  return htmlPage("Terms of Service", 
    "<p><strong>Last updated: October 7, 2026.</strong></p>" +
    "<h2>1. Service</h2><p>Image Tools provides image editing and utility features for Android devices. Features may change as the product evolves.</p>" +
    "<h2>2. Your content</h2><p>You remain responsible for the images and other content you choose to process or export. You must have the rights and permissions required to use that content.</p>" +
    "<h2>3. Premium</h2><p>Premium features are provided through a recurring PayPal subscription. The exact price, currency and billing cycle shown during checkout are controlled by the active PayPal plan. Subscription status is verified by our backend.</p>" +
    "<h2>4. Cancellation</h2><p>Subscription cancellation is handled through the payment provider associated with the subscription. Payment-provider records and billing terms may also apply. For help, use the Support page.</p>" +
    "<h2>5. Availability</h2><p>We aim to keep the service reliable, but we do not guarantee uninterrupted availability or that every image format will be supported on every device.</p>" +
    "<h2>6. Acceptable use</h2><p>You may not use the service for unlawful activity, fraud, infringement of another person's rights, or attempts to compromise the service.</p>" +
    "<h2>7. Changes</h2><p>We may change these terms when the service changes. The current terms are published on this page.</p>" +
    "<h2>8. Contact</h2><p>Questions about the service can be sent through <a href=\"" + SITE + "/support\">Support</a>.</p>");
}

function supportPage() {
  return htmlPage("Support", 
    "<p>For help with Image Tools, payment status, account access or privacy requests, use the contact channel associated with the developer account or the project's public issue tracker.</p>" +
    "<h2>Account deletion</h2>" +
    "<p>To request deletion of your Image Tools account and associated server-side data, include the email address used for the account and a short statement requesting deletion. Do not send passwords or PayPal credentials.</p>" +
    "<p><a href=\"https://github.com/nexauren1/Image-Tools/issues\">Open the Image Tools support tracker on GitHub</a></p>" +
    "<p>For payment-provider disputes or billing details, use the transaction information and support options provided by PayPal.</p>");
}

export default {
  async fetch(request, env) {
    try {
      if (request.method === "OPTIONS") return new Response(null, { headers: HEADERS });
      const url = new URL(request.url);

      if (request.method === "GET" && url.pathname === "/") return homePage();
      if (request.method === "GET" && (url.pathname === "/privacy" || url.pathname === "/privacy-policy")) return privacyPage();
      if (request.method === "GET" && (url.pathname === "/terms" || url.pathname === "/terms-of-service")) return termsPage();
      if (request.method === "GET" && (url.pathname === "/support" || url.pathname === "/support/account-deletion")) return supportPage();

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
      console.error("Unhandled Image Tools Worker request exception", error);
      const code = error && error.code ? error.code : "UNEXPECTED_ERROR";
      const stage = error && error.stage ? error.stage : "worker";
      const explicitStatus = Number(error && error.httpStatus);
      const status = explicitStatus >= 400 && explicitStatus <= 599
        ? explicitStatus
        : code === "PERMISSION_DENIED" || code === "NOT_AUTHORIZED"
          ? 403
          : 500;

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
