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

async function setPremium(env, uid, orderId) {
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
    "&updateMask.fieldPaths=paypalOrderId" +
    "&updateMask.fieldPaths=updatedAt";

  const body = {
    fields: {
      plan: { stringValue: "premium" },
      premium: { booleanValue: true },
      paymentProvider: { stringValue: "paypal" },
      paymentProduct: { stringValue: "image-tools-premium" },
      paypalOrderId: { stringValue: orderId },
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

  if (!r.ok) throw new Error("Firestore Premium update failed");
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
        return_url: env.WORKER_PUBLIC_URL + "/paypal/return",
        cancel_url: env.WORKER_PUBLIC_URL + "/paypal/return?cancel=1"
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
          firebaseAuthConfigured: Boolean(env.FIREBASE_WEB_API_KEY || true),
          firestoreAdminConfigured: Boolean(env.FIREBASE_CLIENT_EMAIL && env.FIREBASE_PRIVATE_KEY)
        });
      }

      if (request.method === "POST" && url.pathname === "/paypal/create-order") {
        return createOrder(request, env);
      }

      if (request.method === "POST" && url.pathname === "/paypal/capture-order") {
        return captureOrder(request, env);
      }

      if (request.method === "GET" && url.pathname === "/paypal/return") {
        if (url.searchParams.get("cancel") === "1") {
          return new Response("Payment cancelled. Return to Image Tools.", {
            status: 200,
            headers: { "Content-Type": "text/plain; charset=utf-8" }
          });
        }
        const token = url.searchParams.get("token");
        if (!token) return new Response("Missing payment token.", { status: 400 });
        return Response.redirect(
          "imagetools://paypal/return?orderId=" + encodeURIComponent(token),
          302
        );
      }

      return reply({ ok: false, error: "Not found" }, 404);
    } catch (error) {
      return reply({ ok: false, error: error && error.message ? error.message : "Unexpected error" }, 500);
    }
  }
};