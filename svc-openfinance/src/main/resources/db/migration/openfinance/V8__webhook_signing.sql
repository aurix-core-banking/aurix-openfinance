-- ============================================================
-- Flyway: V8 — Assinatura de webhooks (Distribution Plane)
-- Adiciona o segredo HMAC usado para assinar (X-Webhook-Signature)
-- e proteger contra replay (X-Webhook-Timestamp) cada notificação
-- entregue via SubscriptionService.deliverWebhook().
-- ============================================================

ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS webhook_secret VARCHAR(100);

-- Assinaturas existentes (se houver) recebem um segredo temporário — em
-- produção real, o participante precisaria rotacionar antes do primeiro uso.
UPDATE subscriptions SET webhook_secret = 'ROTATE_ME_' || subscription_id
    WHERE webhook_secret IS NULL;

ALTER TABLE subscriptions
    ALTER COLUMN webhook_secret SET NOT NULL;
